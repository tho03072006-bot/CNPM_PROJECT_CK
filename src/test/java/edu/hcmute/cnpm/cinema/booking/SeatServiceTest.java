package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.booking.SeatMapView;
import edu.hcmute.cnpm.cinema.dto.booking.SeatView;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SeatServiceTest {
    private BookingTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new BookingTestFixture();
        when(fixture.showtimeRepository.findById(1L)).thenReturn(Optional.of(fixture.showtime));
        when(fixture.seatRepository.findByRoomId(1L)).thenReturn(List.of(fixture.seat));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    @DisplayName("Từ chối mã suất chiếu không hợp lệ")
    void shouldRejectShowtime_whenShowtimeIdIsInvalid(Long showtimeId) {
        assertThatThrownBy(() -> fixture.seatService.findSeatMap(showtimeId))
                .isInstanceOf(InvalidBookingException.class);
    }

    @Test
    @DisplayName("Báo không tìm thấy khi suất chiếu không tồn tại")
    void shouldReportMissingShowtime_whenShowtimeDoesNotExist() {
        assertThatThrownBy(() -> fixture.seatService.findSeatMap(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Không cho đặt khi suất chiếu đã bắt đầu")
    void shouldRejectShowtime_whenShowtimeHasStarted() {
        fixture.showtime.setStartTime(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> fixture.seatService.findSeatMap(1L))
                .isInstanceOf(InvalidBookingException.class);
    }

    @Test
    @DisplayName("Không cho đặt phim đã ngừng hoạt động")
    void shouldRejectShowtime_whenMovieIsInactive() {
        fixture.movie.setActive(false);
        assertThatThrownBy(() -> fixture.seatService.findSeatMap(1L))
                .isInstanceOf(InvalidBookingException.class);
    }

    @Test
    @DisplayName("Không dựng sơ đồ cho phòng có số cột không hợp lệ")
    void shouldRejectRoom_whenTotalColumnsIsInvalid() {
        fixture.room.setTotalColumns(0);
        assertThatThrownBy(() -> fixture.seatService.findSeatMap(1L))
                .isInstanceOf(InvalidBookingException.class);
    }

    @Test
    @DisplayName("Sơ đồ sắp theo hàng và số cột, hiển thị đúng trạng thái và giá")
    void shouldReturnOrderedSeatMap_whenSeatsHaveDifferentStatuses() {
        Seat secondSeat = fixture.testDataFactory.createSeat(fixture.room, "A", 2);
        secondSeat.setId(2L);
        secondSeat.setSeatType("VIP");
        Seat thirdSeat = fixture.testDataFactory.createSeat(fixture.room, "B", 1);
        thirdSeat.setId(3L);
        Ticket heldTicket = fixture.testDataFactory.newHeldTicket(fixture.showtime, secondSeat, fixture.customer);
        Ticket paidTicket = fixture.testDataFactory.newHeldTicket(fixture.showtime, thirdSeat, fixture.customer);
        paidTicket.setStatus(TicketStatus.PAID);
        when(fixture.seatRepository.findByRoomId(1L)).thenReturn(List.of(thirdSeat, secondSeat, fixture.seat));
        when(fixture.ticketRepository.findByShowtimeIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(List.of(heldTicket, paidTicket));

        SeatMapView seatMap = fixture.seatService.findSeatMap(1L);

        assertThat(seatMap.getSeats()).extracting(SeatView::getId).containsExactly(1L, 2L, 3L);
        assertThat(seatMap.getSeats()).extracting(SeatView::getStatus)
                .containsExactly("AVAILABLE", "HELD", "PAID");
        assertThat(seatMap.getSeats().get(1).getPrice()).isEqualByComparingTo("112500.00");
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"CANCELLED", "EXPIRED"})
    @DisplayName("Vé cũ còn chiếm UNIQUE không được hiển thị thành ghế trống")
    void shouldKeepSeatUnavailable_whenHistoricalTicketStillReservesUniqueKey(TicketStatus status) {
        Ticket ticket = fixture.testDataFactory.newHeldTicket(fixture.showtime, fixture.seat, fixture.customer);
        ticket.setStatus(status);
        when(fixture.ticketRepository.findByShowtimeIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of(ticket));
        assertThat(fixture.seatService.findSeatMap(1L).getSeats().getFirst().getStatus())
                .isEqualTo("UNAVAILABLE");
    }

    @Test
    @DisplayName("Vé vừa đến hạn giữ chưa được báo trống khi chưa giải phóng UNIQUE")
    void shouldKeepSeatUnavailable_whenHoldReachesExpiry() {
        Ticket ticket = fixture.testDataFactory.newHeldTicket(fixture.showtime, fixture.seat, fixture.customer);
        ticket.setHeldAt(LocalDateTime.now().minusMinutes(Constants.SEAT_HOLD_MINUTES));
        when(fixture.ticketRepository.findByShowtimeIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of(ticket));
        assertThat(fixture.seatService.findSeatMap(1L).getSeats().getFirst().isAvailable()).isFalse();
    }
}
