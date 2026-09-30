package edu.hcmute.cnpm.cinema.staff;

import edu.hcmute.cnpm.cinema.dto.staff.TicketCheckResult;
import edu.hcmute.cnpm.cinema.dto.staff.TicketCheckResult.Verdict;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.TicketLookupService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Soát vé ở cửa phòng chiếu.
 *
 * Mọi test truyền "bây giờ" vào tay thay vì dùng đồng hồ máy, để chạy lúc 23 giờ 59
 * hay lúc nào cũng ra cùng một kết quả.
 */
@DisplayName("Soát vé theo mã và theo email")
class TicketLookupServiceIntegrationTest extends IntegrationTestBase {

    private static final LocalDate SHOW_DAY = LocalDate.now().plusDays(2);
    private static final LocalDateTime SHOW_START = SHOW_DAY.atTime(19, 0);
    private static final LocalDateTime SAME_DAY_BEFORE_START = SHOW_DAY.atTime(18, 30);

    @Autowired
    private TicketLookupService ticketLookupService;

    private User customer;
    private Showtime showtime;
    private Ticket paidTicket;
    private Ticket heldTicket;

    @BeforeEach
    void setUpTickets() {
        Movie movie = testDataFactory.createMovie("Phim soát vé");
        Room room = testDataFactory.createRoom("Cinema 1", 1, 4);
        Seat seatA1 = testDataFactory.createSeat(room, "A", 1);
        Seat seatA2 = testDataFactory.createSeat(room, "A", 2);
        Seat seatA3 = testDataFactory.createSeat(room, "A", 3);
        showtime = testDataFactory.createShowtime(movie, room, SHOW_START);
        Showtime pastShowtime = testDataFactory.createShowtime(movie, room, SHOW_START.minusDays(4));
        customer = testDataFactory.createCustomer("khach.soatve@example.com");

        paidTicket = savePaidTicket(showtime, seatA1);
        heldTicket = ticketRepository.save(testDataFactory.newHeldTicket(showtime, seatA2, customer));
        savePaidTicket(pastShowtime, seatA3);
    }

    @Test
    @DisplayName("Vé đã thanh toán, đúng ngày, suất chưa kết thúc thì cho vào")
    void shouldAllowEntry_whenPaidTicketIsForToday() {
        TicketCheckResult result = ticketLookupService.checkTicketCode(
                String.valueOf(paidTicket.getId()), SAME_DAY_BEFORE_START);

        assertThat(result.getVerdict()).isEqualTo(Verdict.VALID);
        assertThat(result.getMessage()).contains("Cinema 1", "A1");
    }

    @Test
    @DisplayName("Nhận cả mã có dấu thăng và khoảng trắng thừa như in trên vé")
    void shouldAcceptHashPrefix_whenTicketCodeTypedLikeOnTicket() {
        TicketCheckResult result = ticketLookupService.checkTicketCode(
                "  #" + paidTicket.getId() + " ", SAME_DAY_BEFORE_START);

        assertThat(result.getTicket().getId()).isEqualTo(paidTicket.getId());
    }

    @Test
    @DisplayName("Vé mới giữ ghế chưa thanh toán thì không cho vào")
    void shouldRefuseEntry_whenTicketNotPaid() {
        TicketCheckResult result = ticketLookupService.checkTicketCode(
                String.valueOf(heldTicket.getId()), SAME_DAY_BEFORE_START);

        assertThat(result.getVerdict()).isEqualTo(Verdict.NOT_PAID);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("Vé của suất ngày mai đem tới hôm nay thì báo sai ngày")
    void shouldReportWrongDay_whenShowtimeIsOnLaterDate() {
        TicketCheckResult result = ticketLookupService.checkTicketCode(
                String.valueOf(paidTicket.getId()), SHOW_DAY.minusDays(1).atTime(19, 0));

        assertThat(result.getVerdict()).isEqualTo(Verdict.WRONG_DAY);
    }

    @Test
    @DisplayName("Suất chiếu đã kết thúc thì vé hết hiệu lực")
    void shouldReportEnded_whenShowtimeAlreadyFinished() {
        LocalDateTime afterEnd = showtime.getEndTime().plusMinutes(1);

        TicketCheckResult result = ticketLookupService.checkTicketCode(
                String.valueOf(paidTicket.getId()), afterEnd);

        assertThat(result.getVerdict()).isEqualTo(Verdict.ENDED);
    }

    @Test
    @DisplayName("Mã vé có chữ hoặc không tồn tại thì báo lỗi dễ hiểu, không phải lỗi 500")
    void shouldExplain_whenTicketCodeIsInvalidOrMissing() {
        assertThatThrownBy(() -> ticketLookupService.checkTicketCode("abc", SAME_DAY_BEFORE_START))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chỉ gồm chữ số");
        assertThatThrownBy(() -> ticketLookupService.checkTicketCode("999999", SAME_DAY_BEFORE_START))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không tìm thấy vé có mã #999999.");
    }

    @Test
    @DisplayName("Tra theo email chỉ ra vé của suất chưa kết thúc, xếp theo giờ chiếu rồi theo ghế")
    void shouldListOnlyUpcomingTickets_whenLookingUpByEmail() {
        List<TicketCheckResult> results = ticketLookupService.findUpcomingTicketsByEmail(
                "  KHACH.SOATVE@example.com ", SAME_DAY_BEFORE_START);

        assertThat(results).extracting(result -> result.getTicket().getId())
                .containsExactly(paidTicket.getId(), heldTicket.getId());
        assertThat(results).extracting(TicketCheckResult::getVerdict)
                .containsExactly(Verdict.VALID, Verdict.NOT_PAID);
    }

    @Test
    @DisplayName("Email chưa đăng ký thì báo rõ là không có tài khoản")
    void shouldExplain_whenEmailHasNoAccount() {
        assertThatThrownBy(() -> ticketLookupService.findUpcomingTicketsByEmail(
                "khongai@example.com", SAME_DAY_BEFORE_START))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không có tài khoản nào");
    }

    private Ticket savePaidTicket(Showtime targetShowtime, Seat seat) {
        Ticket ticket = testDataFactory.newHeldTicket(targetShowtime, seat, customer);
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPaidAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }
}
