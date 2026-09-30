package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.booking.ActiveSeatHoldView;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SeatHoldServiceTest {

    @Test
    @DisplayName("Khôi phục đúng thông tin lượt giữ ghế còn hiệu lực")
    void shouldReturnActiveHold_whenHeldTicketsAreStillValid() {
        BookingTestFixture fixture = new BookingTestFixture();
        Ticket ticket = fixture.testDataFactory
                .newHeldTicket(fixture.showtime, fixture.seat, fixture.customer);
        ticket.setId(10L);
        ticket.setHeldAt(LocalDateTime.now());
        when(fixture.ticketRepository.findByUserIdAndShowtimeIdAndStatus(
                1L, 1L, TicketStatus.HELD)).thenReturn(List.of(ticket));

        ActiveSeatHoldView activeHold = fixture.seatHoldService
                .findActiveHold(1L, 1L).orElseThrow();

        assertThat(activeHold.getTicketIds()).containsExactly(10L);
        assertThat(activeHold.getSeatIds()).containsExactly(1L);
        assertThat(activeHold.getSeatLabels()).containsExactly("A1");
        assertThat(activeHold.getTotalPrice()).isEqualByComparingTo(ticket.getPrice());
    }

    @Test
    @DisplayName("Xoá lượt giữ đã hết hạn ngay khi người dùng tải lại trang")
    void shouldDeleteExpiredTickets_whenRestoringHold() {
        BookingTestFixture fixture = new BookingTestFixture();
        Ticket expired = fixture.testDataFactory
                .newHeldTicket(fixture.showtime, fixture.seat, fixture.customer);
        expired.setId(10L);
        expired.setHeldAt(LocalDateTime.now().minusMinutes(Constants.SEAT_HOLD_MINUTES + 1));
        when(fixture.ticketRepository.findByUserIdAndShowtimeIdAndStatus(
                1L, 1L, TicketStatus.HELD)).thenReturn(List.of(expired));

        assertThat(fixture.seatHoldService.findActiveHold(1L, 1L)).isEmpty();
        verify(fixture.ticketRepository).deleteAll(List.of(expired));
    }

    @Test
    @DisplayName("Không truy vấn database khi mã người dùng hoặc suất chiếu không hợp lệ")
    void shouldReturnEmptyWithoutQuery_whenIdentifiersAreInvalid() {
        BookingTestFixture fixture = new BookingTestFixture();

        assertThat(fixture.seatHoldService.findActiveHold(null, 1L)).isEmpty();
        assertThat(fixture.seatHoldService.findActiveHold(1L, 0L)).isEmpty();
        verifyNoInteractions(fixture.ticketRepository);
    }
}
