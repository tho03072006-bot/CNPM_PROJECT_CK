package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.dto.stats.RevenueRow;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.PaymentMethod;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketRefund;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.service.StatsService;
import edu.hcmute.cnpm.cinema.service.TicketRefundService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Huỷ vé đã thanh toán theo chính sách hoàn tiền.
 *
 * Mọi test truyền "bây giờ" vào tay: chính sách tính theo số giờ còn lại tới giờ chiếu,
 * dựa vào đồng hồ máy thì test chạy lúc nào ra kết quả lúc đó.
 */
@DisplayName("Huỷ vé đã thanh toán và hoàn tiền")
class TicketRefundIntegrationTest extends IntegrationTestBase {

    private static final BigDecimal PRICE = new BigDecimal("75000.00");

    @Autowired
    private TicketRefundService ticketRefundService;
    @Autowired
    private StatsService statsService;

    private User customer;
    private Showtime showtime;
    private Seat seatA1;
    private LocalDateTime showStart;

    @BeforeEach
    void setUpShowtime() {
        Movie movie = testDataFactory.createMovie("Phim huỷ vé");
        Room room = testDataFactory.createRoom("Cinema 3", 1, 6);
        seatA1 = testDataFactory.createSeat(room, "A", 1);
        testDataFactory.createSeat(room, "A", 2);
        showStart = LocalDateTime.now().plusDays(3).withNano(0);
        showtime = testDataFactory.createShowtime(movie, room, showStart);
        customer = testDataFactory.createCustomer("khach.huyve@example.com");
    }

    @Test
    @DisplayName("Huỷ trước 24 giờ trở lên thì hoàn đủ 100% và ghế trống lại cho người khác đặt")
    void shouldRefundFullPriceAndFreeSeat_whenCancelledAtLeast24HoursAhead() {
        Ticket ticket = savePaidTicket(seatA1, PaymentMethod.COUNTER, null);

        TicketRefund refund = ticketRefundService.cancelPaidTicket(customer.getId(), ticket.getId(), showStart.minusHours(25));

        assertThat(refund.getRefundPercent()).isEqualTo(100);
        assertThat(refund.getRefundAmount()).isEqualByComparingTo(PRICE);
        assertThat(refund.getSeatLabel()).isEqualTo("A1");
        assertThat(refund.getRefundRef()).as("Trả tại quầy thì hoàn tiền mặt, không gọi MoMo").isNull();
        assertThat(ticketRepository.findById(ticket.getId())).as("Vé phải bị xoá hẳn như ADR-2").isEmpty();

        User another = testDataFactory.createCustomer("nguoi.sau@example.com");
        ticketRepository.saveAndFlush(testDataFactory.newHeldTicket(showtime, seatA1, another));
        assertThat(ticketRepository.count()).as("Người sau đặt lại được đúng ghế đó").isEqualTo(1);
        verify(momoApiClient, never()).refund(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Huỷ trong vòng 2 tới 24 giờ trước giờ chiếu thì hoàn 50%")
    void shouldRefundHalf_whenCancelledWithin24Hours() {
        Ticket ticket = savePaidTicket(seatA1, PaymentMethod.COUNTER, null);

        TicketRefund refund = ticketRefundService.cancelPaidTicket(customer.getId(), ticket.getId(), showStart.minusHours(5));

        assertThat(refund.getRefundPercent()).isEqualTo(TicketRefundService.PARTIAL_REFUND_PERCENT);
        assertThat(refund.getRefundAmount()).isEqualByComparingTo("37500");
        assertThat(refund.getRetainedAmount()).isEqualByComparingTo("37500");
    }

    @Test
    @DisplayName("Còn dưới 2 giờ thì không nhận huỷ, vé giữ nguyên")
    void shouldRejectCancel_whenLessThanTwoHoursLeft() {
        Ticket ticket = savePaidTicket(seatA1, PaymentMethod.COUNTER, null);

        assertThatThrownBy(() -> ticketRefundService.cancelPaidTicket(customer.getId(), ticket.getId(), showStart.minusMinutes(90)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ít nhất " + TicketRefundService.MIN_CANCEL_HOURS + " tiếng");
        assertThat(ticketRepository.findById(ticket.getId())).isPresent();
        assertThat(ticketRefundRepository.count()).isZero();
    }

    @Test
    @DisplayName("Vé đã soát vào phòng thì không huỷ được")
    void shouldRejectCancel_whenTicketAlreadyCheckedIn() {
        Ticket ticket = savePaidTicket(seatA1, PaymentMethod.COUNTER, null);
        ticket.setCheckedInAt(showStart.minusDays(2));
        ticketRepository.save(ticket);

        assertThatThrownBy(() -> ticketRefundService.cancelPaidTicket(customer.getId(), ticket.getId(), showStart.minusDays(2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("soát vào phòng");
    }

    @Test
    @DisplayName("Không huỷ được vé của người khác, và cũng không lộ là mã vé đó có tồn tại")
    void shouldHideTicket_whenItBelongsToSomeoneElse() {
        Ticket ticket = savePaidTicket(seatA1, PaymentMethod.COUNTER, null);
        User stranger = testDataFactory.createCustomer("nguoi.la@example.com");

        assertThatThrownBy(() -> ticketRefundService.cancelPaidTicket(stranger.getId(), ticket.getId(), showStart.minusDays(2)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(ticketRepository.findById(ticket.getId())).isPresent();
    }

    @Test
    @DisplayName("Vé trả qua MoMo thì gọi MoMo hoàn tiền đúng giao dịch và ghi lại mã hoàn")
    void shouldRefundThroughMomo_whenPaidByMomo() {
        Ticket ticket = savePaidTicket(seatA1, PaymentMethod.MOMO, "4100000001");
        when(momoApiClient.refund(anyString(), eq("4100000001"), eq(75000L), anyString())).thenReturn("4200000009");

        TicketRefund refund = ticketRefundService.cancelPaidTicket(customer.getId(), ticket.getId(), showStart.minusDays(2));

        assertThat(refund.getRefundRef()).isEqualTo("4200000009");
        assertThat(ticketRefundRepository.findById(refund.getId()).orElseThrow().getRefundRef()).isEqualTo("4200000009");
    }

    @Test
    @DisplayName("MoMo từ chối hoàn tiền thì huỷ không thành, vé còn nguyên - khách không mất vé mà chưa được hoàn tiền")
    void shouldKeepTicket_whenMomoRefusesRefund() {
        Ticket ticket = savePaidTicket(seatA1, PaymentMethod.MOMO, "4100000002");
        when(momoApiClient.refund(anyString(), anyString(), anyLong(), anyString()))
                .thenThrow(new BusinessException("MoMo chưa hoàn được tiền."));

        assertThatThrownBy(() -> ticketRefundService.cancelPaidTicket(customer.getId(), ticket.getId(), showStart.minusDays(2)))
                .isInstanceOf(BusinessException.class);

        assertThat(ticketRepository.findById(ticket.getId())).as("Transaction phải rollback").isPresent();
        assertThat(ticketRefundRepository.count()).isZero();
    }

    @Test
    @DisplayName("Trang Vé của tôi chỉ hiện nút huỷ ở vé còn huỷ được")
    void shouldQuoteOnlyCancellableTickets() {
        Ticket cancellable = savePaidTicket(seatA1, PaymentMethod.COUNTER, null);
        Seat seatA2 = seatRepository.findAll().stream().filter(seat -> seat.getSeatColumn() == 2).findFirst().orElseThrow();
        Ticket held = ticketRepository.save(testDataFactory.newHeldTicket(showtime, seatA2, customer));

        var quotes = ticketRefundService.quoteCancellableTickets(customer.getId(), showStart.minusDays(2));

        assertThat(quotes).containsOnlyKeys(cancellable.getId());
        assertThat(quotes).doesNotContainKey(held.getId());
    }

    @Test
    @DisplayName("Thống kê: vé huỷ không tính là vé bán, nhưng phần phí giữ lại vẫn vào doanh thu ngày bán")
    void shouldCountRetainedFeeButNotTicket_whenTicketRefundedHalf() {
        LocalDateTime now = LocalDateTime.now();
        Showtime soonShowtime = testDataFactory.createShowtime(showtime.getMovie(), showtime.getRoom(), now.plusHours(10));
        Ticket ticket = testDataFactory.newHeldTicket(soonShowtime, seatA1, customer);
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPaidAt(now);
        ticket = ticketRepository.save(ticket);

        ticketRefundService.cancelPaidTicket(customer.getId(), ticket.getId(), now);

        List<RevenueRow> today = statsService.findRevenueByDay(1);
        assertThat(today.get(0).getTicketCount()).isZero();
        assertThat(today.get(0).getRevenue()).isEqualByComparingTo("37500");
        RevenueRow refunded = statsService.summarizeRefunds(1);
        assertThat(refunded.getTicketCount()).isEqualTo(1);
        assertThat(refunded.getRevenue()).isEqualByComparingTo("37500");
    }

    private Ticket savePaidTicket(Seat seat, PaymentMethod method, String paymentRef) {
        Ticket ticket = testDataFactory.newHeldTicket(showtime, seat, customer);
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPaidAt(showStart.minusDays(5));
        ticket.setPaymentMethod(method);
        ticket.setPaymentRef(paymentRef);
        return ticketRepository.save(ticket);
    }
}
