package edu.hcmute.cnpm.cinema.account;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.service.PaymentService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** M3.5 - xac nhan thanh toan. */
@DisplayName("Xac nhan thanh toan")
class PaymentServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PaymentService paymentService;

    @Test
    @DisplayName("Thanh toan chuyen ve sang da thanh toan va ghi lai thoi diem tra tien")
    void shouldMarkTicketsPaid_whenConfirmingPayment() {
        Fixture fixture = createFixture();
        holdTicket(fixture, fixture.seatA1, LocalDateTime.now());
        holdTicket(fixture, fixture.seatA2, LocalDateTime.now());

        List<Ticket> paid = paymentService.confirmPayment(
                fixture.customer.getId(), fixture.showtime.getId());

        assertThat(paid).hasSize(2);
        assertThat(paid).allSatisfy(ticket -> {
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
            assertThat(ticket.getPaidAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("Chan thanh toan khi ve da qua thoi gian giu ghe")
    void shouldReject_whenHoldAlreadyExpired() {
        Fixture fixture = createFixture();
        holdTicket(fixture, fixture.seatA1,
                LocalDateTime.now().minusMinutes(Constants.SEAT_HOLD_MINUTES + 1));

        assertThatThrownBy(() -> paymentService.confirmPayment(
                fixture.customer.getId(), fixture.showtime.getId()))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("quá " + Constants.SEAT_HOLD_MINUTES + " phút");
    }

    @Test
    @DisplayName("Chan thanh toan khi khong co ve nao dang giu")
    void shouldReject_whenNothingHeld() {
        Fixture fixture = createFixture();

        assertThatThrownBy(() -> paymentService.confirmPayment(
                fixture.customer.getId(), fixture.showtime.getId()))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("Không tìm thấy vé đang giữ");
    }

    @Test
    @DisplayName("Khong tra tien ho ve cua nguoi khac")
    void shouldOnlyPayOwnTickets_whenOtherCustomerHoldsSameShowtime() {
        Fixture fixture = createFixture();
        holdTicket(fixture, fixture.seatA1, LocalDateTime.now());

        User nguoiKhac = testDataFactory.createCustomer("nguoikhac@example.com");
        Ticket veNguoiKhac = testDataFactory.newHeldTicket(fixture.showtime, fixture.seatA2, nguoiKhac);
        ticketRepository.saveAndFlush(veNguoiKhac);

        List<Ticket> paid = paymentService.confirmPayment(
                fixture.customer.getId(), fixture.showtime.getId());

        assertThat(paid).hasSize(1);
        assertThat(ticketRepository.findById(veNguoiKhac.getId()).orElseThrow().getStatus())
                .as("Ve cua nguoi khac phai giu nguyen trang thai dang giu")
                .isEqualTo(TicketStatus.HELD);
    }

    @Test
    @DisplayName("Ve qua han khong hien o trang thanh toan")
    void shouldHideExpiredTickets_whenListingPayableTickets() {
        Fixture fixture = createFixture();
        holdTicket(fixture, fixture.seatA1, LocalDateTime.now());
        holdTicket(fixture, fixture.seatA2,
                LocalDateTime.now().minusMinutes(Constants.SEAT_HOLD_MINUTES + 1));

        List<Ticket> payable = paymentService.findPayableTickets(
                fixture.customer.getId(), fixture.showtime.getId());

        assertThat(payable).hasSize(1);
    }

    private record Fixture(Showtime showtime, Seat seatA1, Seat seatA2, User customer) {}

    private Fixture createFixture() {
        Movie movie = testDataFactory.createMovie("Phim kiem thu thanh toan");
        Room room = testDataFactory.createRoom("Cinema 1", 2, 2);
        Seat seatA1 = testDataFactory.createSeat(room, "A", 1);
        Seat seatA2 = testDataFactory.createSeat(room, "A", 2);
        Showtime showtime = testDataFactory.createShowtime(movie, room,
                LocalDateTime.now().plusDays(1).withHour(19).withMinute(0));
        return new Fixture(showtime, seatA1, seatA2,
                testDataFactory.createCustomer("khach@example.com"));
    }

    private void holdTicket(Fixture fixture, Seat seat, LocalDateTime heldAt) {
        Ticket ticket = testDataFactory.newHeldTicket(fixture.showtime, seat, fixture.customer);
        ticket.setHeldAt(heldAt);
        ticketRepository.saveAndFlush(ticket);
    }
}
