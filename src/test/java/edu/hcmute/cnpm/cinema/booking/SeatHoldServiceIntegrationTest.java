package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.service.SeatHoldService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * M2.6 va M2.7 - tra ghe ve trang thai trong.
 *
 * Diem then chot cua ca hai viec: phai XOA HAN dong ve chu khong doi sang
 * EXPIRED / CANCELLED. Rang buoc UNIQUE (showtime_id, seat_id) cua ADR-1 khong
 * nhin cot status, nen chi can dong ve con nam trong bang la ghe van bi chiem.
 * Test o duoi chung minh dieu do: sau khi tra ghe, dat lai chinh ghe do PHAI duoc.
 */
@DisplayName("Tra ghe ve trang thai trong - het han va tu huy")
class SeatHoldServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private SeatHoldService seatHoldService;

    @Test
    @DisplayName("Ve giu qua han bi xoa han khoi bang, khong phai doi sang EXPIRED")
    void shouldDeleteRow_whenHoldExpired() {
        Fixture fixture = createFixture();
        Ticket expired = saveHeldTicket(fixture,
                LocalDateTime.now().minusMinutes(Constants.SEAT_HOLD_MINUTES + 1));

        int released = seatHoldService.releaseExpiredHolds();

        assertThat(released).isEqualTo(1);
        assertThat(ticketRepository.findById(expired.getId()))
                .as("Phai xoa han dong ve, khong duoc de lai voi trang thai EXPIRED")
                .isEmpty();
    }

    @Test
    @DisplayName("Sau khi don ve het han thi nguoi khac dat lai dung ghe do duoc")
    void shouldFreeSeatForOthers_whenExpiredHoldReleased() {
        Fixture fixture = createFixture();
        saveHeldTicket(fixture, LocalDateTime.now().minusMinutes(Constants.SEAT_HOLD_MINUTES + 1));

        seatHoldService.releaseExpiredHolds();

        User nguoiKhac = testDataFactory.createCustomer("nguoikhac@example.com");
        Ticket veMoi = testDataFactory.newHeldTicket(fixture.showtime, fixture.seat, nguoiKhac);
        // Neu ADR-2 lam sai (chi doi status) thi dong nay se nem
        // DataIntegrityViolationException vi dung rang buoc UNIQUE.
        assertThat(ticketRepository.saveAndFlush(veMoi).getId()).isNotNull();
    }

    @Test
    @DisplayName("Ve con trong thoi gian giu thi khong bi dong nham")
    void shouldKeepTicket_whenHoldStillValid() {
        Fixture fixture = createFixture();
        Ticket conHan = saveHeldTicket(fixture, LocalDateTime.now());

        int released = seatHoldService.releaseExpiredHolds();

        assertThat(released).isZero();
        assertThat(ticketRepository.findById(conHan.getId())).isPresent();
    }

    @Test
    @DisplayName("Ve da thanh toan khong bao gio bi dong")
    void shouldNeverTouchPaidTickets_whenReleasingExpiredHolds() {
        Fixture fixture = createFixture();
        Ticket paid = testDataFactory.newHeldTicket(fixture.showtime, fixture.seat, fixture.customer);
        paid.setHeldAt(LocalDateTime.now().minusDays(3));
        paid.setStatus(TicketStatus.PAID);
        paid.setPaidAt(LocalDateTime.now().minusDays(3));
        ticketRepository.saveAndFlush(paid);

        seatHoldService.releaseExpiredHolds();

        assertThat(ticketRepository.findById(paid.getId()))
                .as("Ve da tra tien thi du giu tu 3 hom truoc cung khong duoc xoa")
                .isPresent();
    }

    @Test
    @DisplayName("Khach tu huy thi ghe duoc tra lai ngay")
    void shouldReleaseSeat_whenCustomerCancels() {
        Fixture fixture = createFixture();
        Ticket held = saveHeldTicket(fixture, LocalDateTime.now());

        int released = seatHoldService.cancelHold(fixture.customer.getId(), fixture.showtime.getId());

        assertThat(released).isEqualTo(1);
        assertThat(ticketRepository.findById(held.getId())).isEmpty();
    }

    @Test
    @DisplayName("Khong co ghe nao dang giu thi bao loi ro rang")
    void shouldReject_whenNothingToCancel() {
        Fixture fixture = createFixture();

        assertThatThrownBy(() ->
                seatHoldService.cancelHold(fixture.customer.getId(), fixture.showtime.getId()))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("không có ghế nào đang giữ");
    }

    // ===== tien ich dung chung cho cac test o tren =====

    private record Fixture(Movie movie, Room room, Seat seat, Showtime showtime, User customer) {}

    private Fixture createFixture() {
        Movie movie = testDataFactory.createMovie("Phim kiem thu tra ghe");
        Room room = testDataFactory.createRoom("Cinema 1", 2, 2);
        Seat seat = testDataFactory.createSeat(room, "A", 1);
        Showtime showtime = testDataFactory.createShowtime(movie, room,
                LocalDateTime.now().plusDays(1).withHour(19).withMinute(0));
        User customer = testDataFactory.createCustomer("khach@example.com");
        return new Fixture(movie, room, seat, showtime, customer);
    }

    private Ticket saveHeldTicket(Fixture fixture, LocalDateTime heldAt) {
        Ticket ticket = testDataFactory.newHeldTicket(fixture.showtime, fixture.seat, fixture.customer);
        ticket.setHeldAt(heldAt);
        return ticketRepository.saveAndFlush(ticket);
    }
}
