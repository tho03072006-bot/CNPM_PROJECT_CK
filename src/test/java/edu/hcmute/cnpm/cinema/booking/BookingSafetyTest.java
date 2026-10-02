package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.dto.booking.*;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.*;
import edu.hcmute.cnpm.cinema.service.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookingSafetyTest {
    private BookingTestFixture fixture;
    @BeforeEach
    void prepare() {
        fixture = new BookingTestFixture();
        when(fixture.userRepository.findByIdForBookingUpdate(1L)).thenReturn(Optional.of(fixture.customer));
        when(fixture.showtimeRepository.findById(1L)).thenReturn(Optional.of(fixture.showtime));
        when(fixture.seatRepository.findById(1L)).thenReturn(Optional.of(fixture.seat));
        when(fixture.seatRepository.findByRoomId(1L)).thenReturn(List.of(fixture.seat));
    }
    private HoldSeatsRequest request(Long... ids) {
        HoldSeatsRequest request = new HoldSeatsRequest();
        request.setSeatIds(Arrays.asList(ids)); request.setAgeConfirmed(true); request.setTermsAccepted(true);
        return request;
    }
    private Ticket held(long id) {
        Ticket ticket = fixture.testDataFactory.newHeldTicket(fixture.showtime, fixture.seat, fixture.customer);
        ticket.setId(id); ticket.setHeldAt(fixture.clock.now().minusMinutes(2));
        when(fixture.ticketRepository.findByUserIdAndShowtimeIdAndStatus(1L, 1L, TicketStatus.HELD))
                .thenReturn(List.of(ticket));
        return ticket;
    }
    @Test @DisplayName("Chặn hơn 8 mã ghế trước khi truy vấn từng ghế")
    void shouldRejectOversizedRequestBeforeLookingUpSeats_whenTooManyIds() {
        assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L,
                request(1L,2L,3L,4L,5L,6L,7L,8L,9L), fixture.customer)).isInstanceOf(InvalidBookingException.class);
        verify(fixture.seatRepository, never()).findById(anyLong());
        verifyNoInteractions(fixture.ticketRepository);
    }
    @Test @DisplayName("Xác nhận độ tuổi và quy định là bắt buộc tại máy chủ")
    void shouldRejectMissingConsent_whenCustomerHasNotConfirmed() {
        HoldSeatsRequest request = request(1L);
        request.setTermsAccepted(false);
        assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L, request, fixture.customer))
                .isInstanceOf(InvalidBookingException.class).hasMessageContaining("đồng ý");
        request.setTermsAccepted(true); request.setAgeConfirmed(false);
        assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L, request, fixture.customer))
                .isInstanceOf(InvalidBookingException.class).hasMessageContaining("độ tuổi");
        verifyNoInteractions(fixture.ticketRepository);
    }
    @Test @DisplayName("Tab cũ không huỷ được lượt giữ mới")
    void shouldRejectStaleCancellation_whenTicketIdsChanged() {
        held(20L);
        assertThatThrownBy(() -> fixture.seatHoldService.cancelHold(1L, 1L, List.of(10L)))
                .isInstanceOf(InvalidBookingException.class);
        verify(fixture.ticketRepository, never()).deleteHeldTickets(anyLong(), anyLong(), any(), anyList());
    }
    @Test @DisplayName("Huỷ đúng mã lượt giữ dùng xoá có điều kiện HELD và chủ sở hữu")
    void shouldDeleteOnlyExpectedHeldTickets_whenCancellationMatches() {
        held(20L);
        when(fixture.ticketRepository.deleteHeldTickets(1L, 1L, TicketStatus.HELD, List.of(20L))).thenReturn(1);
        assertThat(fixture.seatHoldService.cancelHold(1L, 1L, List.of(20L))).isEqualTo(1);
        verify(fixture.ticketRepository).deleteHeldTickets(1L, 1L, TicketStatus.HELD, List.of(20L));
        verify(fixture.ticketRepository, never()).deleteAll(anyIterable());
    }
    @Test @DisplayName("Đổi ghế giữ nguyên hạn ban đầu")
    void shouldPreserveOriginalDeadline_whenReplacingHold() {
        Ticket old = held(20L);
        Seat next = fixture.testDataFactory.createSeat(fixture.room, "A", 2); next.setId(2L);
        when(fixture.seatRepository.findById(2L)).thenReturn(Optional.of(next));
        when(fixture.seatRepository.findByRoomId(1L)).thenReturn(List.of(fixture.seat, next));
        when(fixture.ticketRepository.deleteHeldTickets(1L, 1L, TicketStatus.HELD, List.of(20L))).thenReturn(1);
        when(fixture.ticketRepository.saveAndFlush(any())).thenAnswer(call -> {
            Ticket saved = call.getArgument(0); saved.setId(21L); return saved;
        });
        // Leave no new single gap: this room has one seat in each row.
        fixture.seat.setSeatRow("B");
        HoldSeatsRequest request = request(2L); request.setExpectedTicketIds(List.of(20L));
        HoldSeatsResponse result = fixture.seatBookingService.holdSeats(1L, request, fixture.customer);
        assertThat(result.getExpiresAt()).isEqualTo(old.getHeldAt().plusMinutes(5));
        assertThat(result.getTicketIds()).containsExactly(21L);
    }
    @Test @DisplayName("Ghế mới bị chiếm thì chưa xoá ghế cũ")
    void shouldKeepOldHold_whenReplacementSeatIsTaken() {
        held(20L);
        Seat next = fixture.testDataFactory.createSeat(fixture.room, "A", 2); next.setId(2L);
        when(fixture.seatRepository.findById(2L)).thenReturn(Optional.of(next));
        Ticket occupied = fixture.testDataFactory.newHeldTicket(fixture.showtime, next, fixture.customer);
        when(fixture.ticketRepository.findByShowtimeIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of(occupied));
        HoldSeatsRequest request = request(2L); request.setExpectedTicketIds(List.of(20L));
        assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L, request, fixture.customer))
                .isInstanceOf(SeatAlreadyTakenException.class);
        verify(fixture.ticketRepository, never()).deleteHeldTickets(anyLong(), anyLong(), any(), anyList());
    }
    @Test @DisplayName("Đúng thời điểm hết hạn đã không được thanh toán")
    void shouldExpireAtExactDeadline_whenClockEqualsExpiry() {
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        BookingClock fixed = new BookingClock(Clock.fixed(now, BookingClock.ZONE));
        Ticket ticket = held(20L); ticket.setHeldAt(fixed.now().minusMinutes(5));
        assertThat(fixed.expired(ticket)).isTrue();
        ticket.setHeldAt(fixed.now().minusMinutes(5).plusNanos(1));
        assertThat(fixed.expired(ticket)).isFalse();
        assertThat(fixed.epochMillis(fixed.now())).isEqualTo(now.toEpochMilli());
    }
    @Test @DisplayName("Trang thanh toán cũ không trả cho mã vé mới")
    void shouldRejectOldPaymentForm_whenHoldWasReplaced() {
        held(20L);
        BookingOrderService orders = mock(BookingOrderService.class);
        PaymentService payments = new PaymentService(fixture.ticketRepository, orders, fixture.locks, fixture.clock);
        assertThatThrownBy(() -> payments.confirmCounterPayment(1L, 1L, List.of(10L)))
                .isInstanceOf(InvalidBookingException.class);
        verifyNoInteractions(orders);
        verify(fixture.ticketRepository, never()).saveAll(anyList());
    }
    @Test @DisplayName("Giao dịch MoMo cũ không trả cho lượt giữ mới dù cùng số tiền")
    void shouldRejectOldGatewayAnchor_whenNewHoldHasSamePrice() {
        held(20L);
        BookingOrderService orders = mock(BookingOrderService.class);
        PaymentService payments = new PaymentService(fixture.ticketRepository, orders, fixture.locks, fixture.clock);
        assertThatThrownBy(() -> payments.confirmPayment(1L, 1L, PaymentMethod.MOMO, "trans",
                List.of(20L), 10L, 75000L)).isInstanceOf(InvalidBookingException.class);
        verifyNoInteractions(orders);
    }
    @Test @DisplayName("Một ghế đôi còn nguyên không bị coi là một chỗ lẻ")
    void shouldAllowFourCoupleSeats_whenOneWholePairRemains() {
        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Seat seat = fixture.testDataFactory.createSeat(fixture.room, "A", i);
            seat.setId((long)i); seat.setSeatType("COUPLE"); seats.add(seat);
        }
        assertThatCode(() -> fixture.seatSelectionPolicy.validateSelection(seats, seats.subList(0,4), Set.of()))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> fixture.seatSelectionPolicy.validateSelection(seats, seats, Set.of()))
                .isInstanceOf(InvalidBookingException.class);
    }
    private BookingStateService stateService() {
        return new BookingStateService(fixture.locks, fixture.clock, fixture.seatHoldService, fixture.seatService,
                fixture.seatRepository, fixture.ticketRepository, fixture.seatSelectionPolicy,
                fixture.seatPricingService, new BookingConsentPolicy());
    }
    @Test @DisplayName("Gợi ý tính ghế đôi là hai người và kiểm tra ngân sách ở máy chủ")
    void shouldSuggestExactAdmissionCount_whenCoupleSeatsAvailable() {
        fixture.seat.setSeatType("COUPLE");
        assertThat(stateService().suggest(1L, null, 2, "COUPLE", new BigDecimal("150000")).seatIds())
                .containsExactly(1L);
        assertThatThrownBy(() -> stateService().suggest(1L, null, 1, "COUPLE", null))
                .isInstanceOf(InvalidBookingException.class);
        assertThatThrownBy(() -> stateService().suggest(1L, null, 2, "COUPLE", new BigDecimal("149999")))
                .isInstanceOf(InvalidBookingException.class);
    }
    @Test @DisplayName("Gợi ý từ chối số người, loại ghế và ngân sách không hợp lệ")
    void shouldRejectInvalidSuggestions_whenParametersAreInvalid() {
        assertThatThrownBy(() -> stateService().suggest(1L, null, 9, "ANY", null)).isInstanceOf(InvalidBookingException.class);
        assertThatThrownBy(() -> stateService().suggest(1L, null, 2, "OTHER", null)).isInstanceOf(InvalidBookingException.class);
        assertThatThrownBy(() -> stateService().suggest(1L, null, 2, "ANY", BigDecimal.ZERO)).isInstanceOf(InvalidBookingException.class);
    }
    @Test @DisplayName("Phân loại cũ được chuẩn hoá, phim cấm và mã lạ bị chặn")
    void shouldValidateClassification_whenRatingIsConfigured() {
        BookingConsentPolicy policy = new BookingConsentPolicy();
        assertThat(policy.normalize("C13")).isEqualTo("T13");
        assertThat(policy.message("K")).contains("người giám hộ");
        assertThatThrownBy(() -> policy.normalize("C")).isInstanceOf(InvalidBookingException.class);
        assertThatThrownBy(() -> policy.normalize("UNKNOWN")).isInstanceOf(InvalidBookingException.class);
        HoldSeatsRequest request = request(1L); request.setAgeConfirmed(false);
        assertThatCode(() -> policy.validate("P", request)).doesNotThrowAnyException();
    }
}
