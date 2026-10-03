package edu.hcmute.cnpm.cinema.account;

import edu.hcmute.cnpm.cinema.dto.stats.RevenueRow;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.service.StatsService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** M3.8 - thong ke doanh thu cho trang quan tri. */
@DisplayName("Thong ke doanh thu")
class StatsServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private StatsService statsService;

    @Test
    @DisplayName("Chi tinh ve da thanh toan, bo qua ve dang giu")
    void shouldCountOnlyPaidTickets_whenBuildingReport() {
        Room room = testDataFactory.createRoom("Cinema 1", 3, 3);
        User customer = testDataFactory.createCustomer("khach@example.com");
        Movie movie = testDataFactory.createMovie("Phim kiem thu thong ke");
        Showtime showtime = testDataFactory.createShowtime(movie, room,
                LocalDateTime.now().plusDays(1).withHour(19).withMinute(0));

        savePaidTicket(showtime, testDataFactory.createSeat(room, "A", 1), customer,
                new BigDecimal("100000"), LocalDateTime.now());
        // Ve nay dang giu, chua tra tien -> khong duoc tinh vao doanh thu.
        ticketRepository.saveAndFlush(
                testDataFactory.newHeldTicket(showtime, testDataFactory.createSeat(room, "A", 2), customer));

        List<RevenueRow> byDay = statsService.findRevenueByDay(StatsService.DEFAULT_DAYS);

        assertThat(statsService.sumTickets(byDay))
                .as("Chi 1 ve da thanh toan nen tong so ve phai la 1")
                .isEqualTo(1);
        assertThat(statsService.sumRevenue(byDay)).isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    @DisplayName("Ngay khong ban duoc ve nao van hien voi so 0")
    void shouldListEveryDay_evenWhenNoSale() {
        List<RevenueRow> byDay = statsService.findRevenueByDay(7);

        assertThat(byDay)
                .as("Hoi 7 ngay thi phai tra ve du 7 dong, ngay e cung phai co mat")
                .hasSize(7);
        assertThat(byDay).allSatisfy(row -> assertThat(row.getTicketCount()).isZero());
    }

    @Test
    @DisplayName("Phim ban chay xep theo doanh thu giam dan")
    void shouldSortMoviesByRevenue_whenListingTopMovies() {
        Room room = testDataFactory.createRoom("Cinema 1", 5, 5);
        User customer = testDataFactory.createCustomer("khach@example.com");

        Movie phimIt = testDataFactory.createMovie("Phim ban it");
        Showtime suatIt = testDataFactory.createShowtime(phimIt, room,
                LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        savePaidTicket(suatIt, testDataFactory.createSeat(room, "A", 1), customer,
                new BigDecimal("100000"), LocalDateTime.now());

        Movie phimNhieu = testDataFactory.createMovie("Phim ban chay");
        Showtime suatNhieu = testDataFactory.createShowtime(phimNhieu, room,
                LocalDateTime.now().plusDays(1).withHour(14).withMinute(0));
        savePaidTicket(suatNhieu, testDataFactory.createSeat(room, "B", 1), customer,
                new BigDecimal("300000"), LocalDateTime.now());
        savePaidTicket(suatNhieu, testDataFactory.createSeat(room, "B", 2), customer,
                new BigDecimal("300000"), LocalDateTime.now());

        List<RevenueRow> top = statsService.findTopMovies(StatsService.DEFAULT_DAYS, 5);

        assertThat(top).extracting(RevenueRow::getLabel)
                .containsExactly("Phim ban chay", "Phim ban it");
        assertThat(top.get(0).getTicketCount()).isEqualTo(2);
        assertThat(top.get(0).getRevenue()).isEqualByComparingTo(new BigDecimal("600000"));
    }

    private void savePaidTicket(Showtime showtime, Seat seat, User customer,
                                BigDecimal price, LocalDateTime paidAt) {
        Ticket ticket = testDataFactory.newHeldTicket(showtime, seat, customer);
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPrice(price);
        ticket.setPaidAt(paidAt);
        ticketRepository.saveAndFlush(ticket);
    }
}
