package edu.hcmute.cnpm.cinema.account;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.booking.BookingHistoryEntry;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.service.BookingHistoryService;
import edu.hcmute.cnpm.cinema.service.BookingOrderService;
import edu.hcmute.cnpm.cinema.service.PaymentService;
import edu.hcmute.cnpm.cinema.service.ReceiptService;
import edu.hcmute.cnpm.cinema.service.TicketRefundService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("Hữu Tài — lịch sử đặt vé và chi tiết theo giao dịch")
class BookingHistoryIntegrationTest extends IntegrationTestBase {
    @Autowired private MockMvc mvc;
    @Autowired private BookingHistoryService history;
    @Autowired private BookingOrderService orders;
    @Autowired private PaymentService payments;
    @Autowired private TicketRefundService refunds;
    @Autowired private ReceiptService receipts;
    @Autowired private edu.hcmute.cnpm.cinema.service.TicketCodeService codes;
    @Autowired private edu.hcmute.cnpm.cinema.service.SeatHoldService holds;

    @Test
    @DisplayName("Khách chưa đăng nhập phải đăng nhập trước khi xem lịch sử hoặc chi tiết")
    void shouldRequireLogin_whenAnonymous() throws Exception {
        mvc.perform(get("/lich-su-dat-ve")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dang-nhap?next=%2Flich-su-dat-ve"));
        mvc.perform(get("/lich-su-dat-ve/UTE-TEST")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dang-nhap?next=%2Flich-su-dat-ve%2FUTE-TEST"));
    }

    @Test
    @DisplayName("Danh sách chỉ có đơn của tài khoản hiện tại, sắp xếp mới nhất trước")
    void shouldListOwnOrders_whenMultipleCustomersHaveBookings() throws Exception {
        User owner = testDataFactory.createCustomer("history-owner@example.com");
        User other = testDataFactory.createCustomer("history-other@example.com");
        Showtime showtime = createShowtime();
        BookingOrder first = createBookingOrder(owner, showtime, BookingOrderStatus.PAID, LocalDateTime.now().minusDays(2));
        BookingOrder latest = createBookingOrder(owner, showtime, BookingOrderStatus.DRAFT, LocalDateTime.now());
        BookingOrder foreign = createBookingOrder(other, showtime, BookingOrderStatus.PAID, LocalDateTime.now().plusMinutes(1));
        assertThat(history.findHistory(owner.getId(), 0, null).getContent())
                .extracting(BookingHistoryEntry::receiptCode).containsExactly(latest.getReceiptCode(), first.getReceiptCode());
        String html = mvc.perform(get("/lich-su-dat-ve").sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Lịch sử đặt vé")))
                .andExpect(content().string(containsString("booking-history-poster")))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains(first.getReceiptCode(), latest.getReceiptCode()).doesNotContain(foreign.getReceiptCode());
    }

    @Test
    @DisplayName("Phân trang 12 đơn, lọc trạng thái và không mất giao dịch cùng thời điểm")
    void shouldPaginateAndFilter_whenHistoryHasMoreThanTwelveOrders() throws Exception {
        User owner = testDataFactory.createCustomer("history-page@example.com");
        Showtime showtime = createShowtime();
        LocalDateTime createdAt = LocalDateTime.now();
        for (int i = 0; i < 13; i++) {
            createBookingOrder(owner, showtime, i == 0 ? BookingOrderStatus.CANCELLED : BookingOrderStatus.DRAFT, createdAt);
        }
        var first = history.findHistory(owner.getId(), 0, null);
        var second = history.findHistory(owner.getId(), 1, null);
        assertThat(first.getTotalElements()).isEqualTo(13);
        assertThat(first.getContent()).hasSize(12);
        assertThat(second.getContent()).hasSize(1).doesNotContainAnyElementsOf(first.getContent());
        assertThat(history.findHistory(owner.getId(), 0, BookingOrderStatus.CANCELLED).getContent()).hasSize(1);
        mvc.perform(get("/lich-su-dat-ve").sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Trang sau")));
        mvc.perform(get("/lich-su-dat-ve").param("status", "CANCELLED").sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Đã hủy")));
    }

    @Test
    @DisplayName("Chi tiết giữ cùng số tiền với hóa đơn, hiển thị từng mã vé và QR")
    void shouldShowReceiptDataAndIndividualQrCodes_whenOrderPaid() throws Exception {
        User owner = testDataFactory.createCustomer("history-detail@example.com");
        Showtime showtime = createShowtime();
        Seat first = testDataFactory.createSeat(showtime.getRoom(), "A", 1);
        Seat second = testDataFactory.createSeat(showtime.getRoom(), "A", 2);
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, first, owner));
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, second, owner));
        ConcessionProduct combo = testDataFactory.createConcessionProduct("HISTORY-COMBO", new BigDecimal("79000"));
        orders.saveConcessions(owner.getId(), showtime.getId(), Map.of(combo.getId(), 1));
        List<Ticket> paid = payments.confirmPayment(owner.getId(), showtime.getId());
        String code = paid.getFirst().getBookingOrder().getReceiptCode();
        var detail = history.findDetail(code, owner.getId());
        assertThat(detail.lines()).isEqualTo(receipts.findReceipt(code, owner).lines());
        assertThat(detail.order().getTotalAmount()).isEqualByComparingTo("229000");
        assertThat(detail.tickets()).hasSize(2).allSatisfy(ticket -> {
            assertThat(ticket.publicCode()).matches("[1-9][0-9]{7}").isEqualTo(codes.codeFor(ticket.ticketId()));
            assertThat(ticket.qrSvg()).contains("QR vé " + ticket.publicCode());
        });
        // Tên phim trên chứng từ phải giữ nguyên khi quản trị đổi tên phim sau khi mua.
        Movie movie = movieRepository.findById(showtime.getMovie().getId()).orElseThrow();
        movie.setTitle("Tên phim đã thay đổi");
        movieRepository.saveAndFlush(movie);
        mvc.perform(get("/lich-su-dat-ve/{code}", code).sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Chi tiết đặt vé")))
                .andExpect(content().string(containsString("229.000 đ")))
                .andExpect(content().string(containsString("Phim lịch sử")))
                .andExpect(content().string(containsString("Bắp nước test")))
                .andExpect(content().string(containsString("Không áp dụng")))
                .andExpect(content().string(containsString("QR vé " + codes.codeFor(paid.getFirst().getId()))));
    }

    @Test
    @DisplayName("Khách khác, nhân viên và quản trị không xem được chi tiết của chủ đơn")
    void shouldHideOtherOwnersDetails_whenViewerHasAnyRole() throws Exception {
        User owner = testDataFactory.createCustomer("history-private@example.com");
        Showtime showtime = createShowtime();
        BookingOrder order = createBookingOrder(owner, showtime, BookingOrderStatus.PAID, LocalDateTime.now());
        for (Role role : Role.values()) {
            User stranger = testDataFactory.createUserWithRole("history-" + role.name().toLowerCase() + "@example.com", role);
            mvc.perform(get("/lich-su-dat-ve/{code}", order.getReceiptCode()).sessionAttr(Constants.SESSION_USER, stranger))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("Hủy toàn bộ vé vẫn giữ giao dịch, mã vé cũ và số tiền hoàn; không hiện QR")
    void shouldKeepCancelledTicketsAndRefunds_whenAllTicketsRefunded() throws Exception {
        User owner = testDataFactory.createCustomer("history-refunded@example.com");
        Showtime showtime = createShowtime();
        Seat seat = testDataFactory.createSeat(showtime.getRoom(), "A", 1);
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat, owner));
        Ticket paid = payments.confirmPayment(owner.getId(), showtime.getId()).getFirst();
        String code = paid.getBookingOrder().getReceiptCode();
        refunds.cancelPaidTicket(owner.getId(), paid.getId(), LocalDateTime.now());
        var detail = history.findDetail(code, owner.getId());
        assertThat(detail.tickets()).hasSize(1);
        assertThat(detail.tickets().getFirst().ticketId()).isEqualTo(paid.getId());
        assertThat(detail.tickets().getFirst().qrSvg()).isNull();
        assertThat(detail.refundedAmount()).isEqualByComparingTo("75000");
        assertThat(history.findHistory(owner.getId(), 0, null).getTotalElements()).isEqualTo(1);
        mvc.perform(get("/lich-su-dat-ve/{code}", code).sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Đã hoàn tiền")))
                .andExpect(content().string(containsString(codes.codeFor(paid.getId()))));
    }

    @Test
    @DisplayName("Đơn nháp xem được số tiền dự kiến nhưng không có mã QR vào phòng")
    void shouldShowUnpaidDetailsWithoutEntryQr_whenDraft() throws Exception {
        User owner = testDataFactory.createCustomer("history-draft@example.com");
        Showtime showtime = createShowtime();
        Seat seat = testDataFactory.createSeat(showtime.getRoom(), "A", 1);
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat, owner));
        BookingOrder draft = orders.prepareForPayment(owner.getId(), showtime.getId());
        assertThat(history.findDetail(draft.getReceiptCode(), owner.getId()).tickets().getFirst().qrSvg()).isNull();
        mvc.perform(get("/lich-su-dat-ve/{code}", draft.getReceiptCode()).sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Tổng tiền dự kiến")))
                .andExpect(content().string(containsString("Chưa chọn phương thức thanh toán")));
    }

    @Test
    @DisplayName("Lịch sử rỗng và trang ngoài phạm vi đều hiển thị hướng dẫn, không lỗi 500")
    void shouldShowEmptyState_whenNoBookingsOrPageOutsideRange() throws Exception {
        User owner = testDataFactory.createCustomer("history-empty@example.com");
        mvc.perform(get("/lich-su-dat-ve").sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Chuyến đi điện ảnh đang chờ bạn")));
        mvc.perform(get("/lich-su-dat-ve").param("page", "100").sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Trang này chưa có giao dịch")));
        assertThat(history.findHistory(owner.getId(), -1, null).getNumber()).isZero();
    }

    private Showtime createShowtime() {
        Movie movie = testDataFactory.createMovie("Phim lịch sử");
        Room room = testDataFactory.createRoom("Cinema 4", 2, 4);
        return testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(3));
    }

    private int generatedShowtimes;

    @Test
    @DisplayName("Hết hạn giữ ghế vẫn đọc được ghế, mã và giá đã lưu; ghế được bán lại")
    void shouldKeepTicketSnapshot_whenExpiredHoldIsReleased() throws Exception {
        User owner = testDataFactory.createCustomer("history-expired@example.com");
        Showtime showtime = createShowtime();
        Seat seat = testDataFactory.createSeat(showtime.getRoom(), "A", 1);
        Ticket held = ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat, owner));
        BookingOrder oldOrder = orders.prepareForPayment(owner.getId(), showtime.getId());
        seat.setSeatRow("B");
        seatRepository.saveAndFlush(seat);
        jdbcTemplate.update("UPDATE tickets SET held_at = ? WHERE id = ?", LocalDateTime.now().minusMinutes(10), held.getId());
        assertThat(holds.releaseExpiredHolds(showtime.getId())).isEqualTo(1);
        assertThat(ticketRepository.findById(held.getId())).isEmpty();
        var detail = history.findDetail(oldOrder.getReceiptCode(), owner.getId());
        assertThat(detail.order().getStatus()).isEqualTo(BookingOrderStatus.CANCELLED);
        assertThat(detail.seats()).isEqualTo("A1");
        assertThat(detail.tickets()).hasSize(1);
        assertThat(detail.tickets().getFirst().ticketId()).isEqualTo(held.getId());
        assertThat(detail.tickets().getFirst().qrSvg()).isNull();
        assertThat(detail.lines().getFirst().amount()).isEqualByComparingTo("75000");
        mvc.perform(get("/lich-su-dat-ve/{code}", oldOrder.getReceiptCode()).sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Ghế A1")));
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat, owner));
        BookingOrder next = orders.prepareForPayment(owner.getId(), showtime.getId());
        assertThat(next.getReceiptCode()).isNotEqualTo(oldOrder.getReceiptCode());
        assertThat(history.findHistory(owner.getId(), 0, null).getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Ưu đãi đã lưu khớp trên chi tiết và hóa đơn, hoàn tiền không vượt tiền đã trả")
    void shouldPreserveAppliedOffer_whenOrderPaidAndTicketRefunded() throws Exception {
        User owner = testDataFactory.createCustomer("history-offer@example.com");
        Showtime showtime = createShowtime();
        Seat first = testDataFactory.createSeat(showtime.getRoom(), "A", 1);
        Seat second = testDataFactory.createSeat(showtime.getRoom(), "A", 2);
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, first, owner));
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, second, owner));
        BookingOrder draft = orders.applyValidatedOffer(owner.getId(), showtime.getId(), "UTE20", "Ưu đãi sinh viên", new BigDecimal("20000"));
        assertThat(payments.prepareCheckout(owner.getId(), showtime.getId(), null).total()).isEqualByComparingTo("130000");
        List<Ticket> paid = payments.confirmPayment(owner.getId(), showtime.getId());
        var detail = history.findDetail(draft.getReceiptCode(), owner.getId());
        assertThat(detail.discountAmount()).isEqualByComparingTo("20000");
        assertThat(detail.order().getAppliedVoucherCode()).isEqualTo("UTE20");
        assertThat(detail.lines()).isEqualTo(receipts.findReceipt(draft.getReceiptCode(), owner).lines());
        for (String path : List.of("/hoa-don/", "/lich-su-dat-ve/")) {
            mvc.perform(get(path + draft.getReceiptCode()).sessionAttr(Constants.SESSION_USER, owner))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("UTE20")))
                    .andExpect(content().string(containsString("Ưu đãi sinh viên")))
                    .andExpect(content().string(containsString("130.000 đ")));
        }
        BigDecimal returned = BigDecimal.ZERO;
        for (Ticket ticket : paid) returned = returned.add(refunds.cancelPaidTicket(owner.getId(), ticket.getId(), LocalDateTime.now()).getRefundAmount());
        assertThat(returned).isEqualByComparingTo("130000");
        detail = history.findDetail(draft.getReceiptCode(), owner.getId());
        assertThat(detail.lines()).isEqualTo(receipts.findReceipt(draft.getReceiptCode(), owner).lines());
        assertThat(detail.tickets()).hasSize(2).allSatisfy(ticket -> assertThat(ticket.qrSvg()).isNull());
        assertThat(detail.lines().stream().map(line -> line.amount()).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("150000");
    }

    @Test
    @DisplayName("Tải QR chỉ được phép với vé đã thanh toán của chủ đơn; vé hủy không tải được")
    void shouldGuardQrDownloads_whenOtherUsersOrCancelledTicketsRequestThem() throws Exception {
        User owner = testDataFactory.createCustomer("history-qr-owner@example.com");
        User other = testDataFactory.createCustomer("history-qr-other@example.com");
        Showtime showtime = createShowtime();
        Seat seat = testDataFactory.createSeat(showtime.getRoom(), "A", 1);
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat, owner));
        Ticket paid = payments.confirmPayment(owner.getId(), showtime.getId()).getFirst();
        String url = "/lich-su-dat-ve/" + paid.getBookingOrder().getReceiptCode() + "/ve/" + paid.getId() + "/qr.svg";
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).sessionAttr(Constants.SESSION_USER, other)).andExpect(status().isNotFound());
        mvc.perform(get(url).sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store, private"))
                .andExpect(content().string(containsString("QR vé " + codes.codeFor(paid.getId()))));
        refunds.cancelPaidTicket(owner.getId(), paid.getId(), LocalDateTime.now());
        mvc.perform(get(url).sessionAttr(Constants.SESSION_USER, owner)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Đường dẫn vé cũ chuyển về một trang lịch sử; chi tiết giữ nút hủy từng vé")
    void shouldKeepLegacyLinksAndCancellation_whenScreensAreUnified() throws Exception {
        User owner = testDataFactory.createCustomer("history-unified@example.com");
        Showtime showtime = createShowtime();
        BookingOrder order = createBookingOrder(owner, showtime, BookingOrderStatus.PAID, LocalDateTime.now());
        mvc.perform(get("/ve-cua-toi").sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/lich-su-dat-ve"));
        mvc.perform(get("/lich-su-dat-ve/{code}", order.getReceiptCode()).sessionAttr(Constants.SESSION_USER, owner))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Hủy vé · hoàn 100%")));
    }

    @Test
    @DisplayName("Ưu đãi không hợp lệ bị chặn, đổi tiền hàng phải xác thực lại ưu đãi")
    void shouldClearOffer_whenOrderSelectionChanges() {
        User owner = testDataFactory.createCustomer("history-offer-change@example.com");
        Showtime showtime = createShowtime();
        Seat seat = testDataFactory.createSeat(showtime.getRoom(), "A", 1);
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat, owner));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> orders.applyValidatedOffer(owner.getId(), showtime.getId(),
                "BAD", "Giảm sai", new BigDecimal("100000")))
                .isInstanceOf(edu.hcmute.cnpm.cinema.exception.InvalidBookingException.class);
        orders.applyValidatedOffer(owner.getId(), showtime.getId(), "UTE20", "Ưu đãi sinh viên", new BigDecimal("20000"));
        ConcessionProduct combo = testDataFactory.createConcessionProduct("HISTORY-OFFER-CHANGE", new BigDecimal("79000"));
        BookingOrder changed = orders.saveConcessions(owner.getId(), showtime.getId(), Map.of(combo.getId(), 1));
        assertThat(changed.getAppliedVoucherCode()).isNull();
        assertThat(changed.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(changed.getTotalAmount()).isEqualByComparingTo("154000");
        orders.applyValidatedOffer(owner.getId(), showtime.getId(), "UTE20", "Ưu đãi sinh viên", new BigDecimal("20000"));
        ConcessionProduct alternative = testDataFactory.createConcessionProduct("HISTORY-SAME-PRICE", new BigDecimal("79000"));
        BookingOrder switched = orders.saveConcessions(owner.getId(), showtime.getId(), Map.of(alternative.getId(), 1));
        assertThat(switched.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(switched.getAppliedVoucherCode()).isNull();
    }

    /** Dùng factory và luồng đơn hàng thật; không sửa TestDataFactory chung của nhóm. */
    private BookingOrder createBookingOrder(User user, Showtime sample,
                                            BookingOrderStatus status, LocalDateTime createdAt) {
        Showtime showtime = testDataFactory.createShowtime(sample.getMovie(), sample.getRoom(),
                sample.getStartTime().plusDays(++generatedShowtimes));
        List<Seat> roomSeats = seatRepository.findByRoomId(sample.getRoom().getId());
        Seat seat = roomSeats.isEmpty() ? testDataFactory.createSeat(sample.getRoom(), "A", 1) : roomSeats.getFirst();
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat, user));
        BookingOrder order = status == BookingOrderStatus.PAID
                ? payments.confirmPayment(user.getId(), showtime.getId()).getFirst().getBookingOrder()
                : orders.prepareForPayment(user.getId(), showtime.getId());
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        return bookingOrderRepository.saveAndFlush(order);
    }

    @Test
    @DisplayName("Hai lần mua cùng suất trong một giây không lấy nhầm biên nhận hoàn tiền")
    void shouldKeepRefundsInOriginalOrder_whenPaymentsAreLessThanOneSecondApart() {
        User owner = testDataFactory.createCustomer("history-close-payments@example.com");
        Showtime showtime = createShowtime();
        Seat first = testDataFactory.createSeat(showtime.getRoom(), "A", 1);
        Seat second = testDataFactory.createSeat(showtime.getRoom(), "A", 2);
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, first, owner));
        Ticket paidFirst = payments.confirmPayment(owner.getId(), showtime.getId()).getFirst();
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, second, owner));
        Ticket paidSecond = payments.confirmPayment(owner.getId(), showtime.getId()).getFirst();
        LocalDateTime base = LocalDateTime.now().withNano(0);
        jdbcTemplate.update("UPDATE tickets SET paid_at = ? WHERE id = ?", base, paidFirst.getId());
        jdbcTemplate.update("UPDATE booking_orders SET paid_at = ? WHERE id = ?", base, paidFirst.getBookingOrder().getId());
        jdbcTemplate.update("UPDATE tickets SET paid_at = ? WHERE id = ?", base.plusNanos(200_000_000), paidSecond.getId());
        jdbcTemplate.update("UPDATE booking_orders SET paid_at = ? WHERE id = ?", base.plusNanos(200_000_000), paidSecond.getBookingOrder().getId());
        refunds.cancelPaidTicket(owner.getId(), paidFirst.getId(), LocalDateTime.now());
        var secondDetail = history.findDetail(paidSecond.getBookingOrder().getReceiptCode(), owner.getId());
        assertThat(secondDetail.tickets()).hasSize(1);
        assertThat(secondDetail.tickets().getFirst().ticketId()).isEqualTo(paidSecond.getId());
        assertThat(secondDetail.refundedAmount()).isEqualByComparingTo("0");
    }
}
