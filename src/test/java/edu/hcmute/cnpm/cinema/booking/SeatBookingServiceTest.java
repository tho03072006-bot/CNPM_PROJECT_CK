package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.booking.HoldSeatsRequest;
import edu.hcmute.cnpm.cinema.dto.booking.HoldSeatsResponse;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.exception.SeatAlreadyTakenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SeatBookingServiceTest {
    private BookingTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new BookingTestFixture();
        when(fixture.userRepository.findById(1L)).thenReturn(Optional.of(fixture.customer));
        when(fixture.showtimeRepository.findById(1L)).thenReturn(Optional.of(fixture.showtime));
        when(fixture.seatRepository.findById(1L)).thenReturn(Optional.of(fixture.seat));
        when(fixture.seatRepository.findByRoomId(1L)).thenReturn(List.of(fixture.seat));
        when(fixture.ticketRepository.findByShowtimeIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("Không giữ ghế khi chưa đăng nhập")
    void shouldRejectBooking_whenUserIsNotLoggedIn() {
        assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L, createRequest(1L), null))
                .isInstanceOf(InvalidBookingException.class).hasMessageContaining("đăng nhập");
        verifyNoInteractions(fixture.ticketRepository);
    }

    @Test
    @DisplayName("Từ chối session trỏ tới tài khoản đã bị xóa")
    void shouldRejectBooking_whenSessionUserNoLongerExists() {
        when(fixture.userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L, createRequest(1L), fixture.customer))
                .isInstanceOf(InvalidBookingException.class);
        verifyNoInteractions(fixture.ticketRepository);
    }

    @Test
    @DisplayName("Không ghi vé khi thiếu nội dung yêu cầu hoặc danh sách ghế")
    void shouldRejectBooking_whenRequestOrSeatListIsMissing() {
        for (HoldSeatsRequest request : Arrays.asList(null, new HoldSeatsRequest(), createRequest())) {
            assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L, request, fixture.customer))
                    .isInstanceOf(InvalidBookingException.class).hasMessageContaining("ít nhất một ghế");
        }
        verifyNoInteractions(fixture.ticketRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    @DisplayName("Từ chối mã ghế rỗng, bằng không hoặc âm")
    void shouldRejectBooking_whenSeatIdIsInvalid(Long seatId) {
        assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L, createRequest(seatId), fixture.customer))
                .isInstanceOf(InvalidBookingException.class);
        verifyNoInteractions(fixture.ticketRepository);
    }

    @Test
    @DisplayName("Từ chối một ghế xuất hiện hai lần trong yêu cầu")
    void shouldRejectBooking_whenSeatIdsContainDuplicates() {
        assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L, createRequest(1L, 1L), fixture.customer))
                .isInstanceOf(InvalidBookingException.class).hasMessageContaining("nhiều lần");
        verifyNoInteractions(fixture.ticketRepository);
    }

    @Test
    @DisplayName("Kiểm tra toàn bộ ghế trước khi ghi bất kỳ vé nào")
    void shouldRejectBookingWithoutSavingTickets_whenOneSeatDoesNotExist() {
        assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L, createRequest(1L, 99L), fixture.customer))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(fixture.ticketRepository);
    }

    @Test
    @DisplayName("Không đặt ghế thuộc phòng khác")
    void shouldRejectBooking_whenSeatBelongsToAnotherRoom() {
        Room otherRoom = fixture.testDataFactory.createRoom("Phòng khác", 2, 8);
        otherRoom.setId(2L);
        fixture.seat.setRoom(otherRoom);
        assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L, createRequest(1L), fixture.customer))
                .isInstanceOf(InvalidBookingException.class).hasMessageContaining("không thuộc phòng");
        verifyNoInteractions(fixture.ticketRepository);
    }

    @Test
    @DisplayName("Không ghi vé khi ghế có loại không được hỗ trợ")
    void shouldRejectBooking_whenSeatTypeIsUnsupported() {
        fixture.seat.setSeatType("OTHER");
        assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L, createRequest(1L), fixture.customer))
                .isInstanceOf(InvalidBookingException.class);
        verifyNoInteractions(fixture.ticketRepository);
    }

    @Test
    @DisplayName("Giữ nhiều ghế đúng giá, cùng thời điểm và đúng người dùng trong session")
    void shouldCreateHeldTickets_whenAllSeatsAreValid() {
        Seat vipSeat = fixture.testDataFactory.createSeat(fixture.room, "A", 2);
        vipSeat.setId(2L);
        vipSeat.setSeatType("VIP");
        when(fixture.seatRepository.findById(2L)).thenReturn(Optional.of(vipSeat));
        when(fixture.seatRepository.findByRoomId(1L)).thenReturn(List.of(fixture.seat, vipSeat));
        AtomicLong nextTicketId = new AtomicLong();
        when(fixture.ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(nextTicketId.incrementAndGet());
            return ticket;
        });

        HoldSeatsResponse response = fixture.seatBookingService.holdSeats(1L, createRequest(2L, 1L), fixture.customer);

        ArgumentCaptor<Ticket> tickets = ArgumentCaptor.forClass(Ticket.class);
        verify(fixture.ticketRepository, times(2)).saveAndFlush(tickets.capture());
        assertThat(response.getTotalPrice()).isEqualByComparingTo("187500.00");
        assertThat(response.getTicketIds()).containsExactly(1L, 2L);
        assertThat(tickets.getAllValues()).extracting(ticket -> ticket.getSeat().getId()).containsExactly(1L, 2L);
        assertThat(tickets.getAllValues()).allSatisfy(ticket -> {
            assertThat(ticket.getUser()).isSameAs(fixture.customer);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.HELD);
            assertThat(ticket.getPaidAt()).isNull();
            assertThat(ticket.getHeldAt()).isEqualTo(response.getExpiresAt().minusMinutes(Constants.SEAT_HOLD_MINUTES));
        });
    }

    @Test
    @DisplayName("Đổi lỗi ràng buộc database thành lỗi tranh chấp ghế của nhóm")
    void shouldThrowSeatAlreadyTaken_whenDatabaseRejectsDuplicateSeat() {
        DataIntegrityViolationException conflict = new DataIntegrityViolationException("uq_showtime_seat");
        when(fixture.ticketRepository.saveAndFlush(any(Ticket.class))).thenThrow(conflict);
        assertThatThrownBy(() -> fixture.seatBookingService.holdSeats(1L, createRequest(1L), fixture.customer))
                .isInstanceOf(SeatAlreadyTakenException.class).hasCause(conflict);
    }

    private HoldSeatsRequest createRequest(Long... seatIds) {
        HoldSeatsRequest request = new HoldSeatsRequest();
        request.setSeatIds(Arrays.asList(seatIds));
        return request;
    }
}
