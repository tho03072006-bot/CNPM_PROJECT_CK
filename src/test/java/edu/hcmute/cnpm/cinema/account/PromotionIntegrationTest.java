package edu.hcmute.cnpm.cinema.account;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.service.*;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("Ưu đãi từ giao diện đến thanh toán và hoàn vé")
class PromotionIntegrationTest extends IntegrationTestBase {
    @Autowired private MockMvc mvc;
    @Autowired private BookingOrderService orders;
    @Autowired private PaymentService payments;
    @Autowired private TicketRefundService refunds;
    @Autowired private ReceiptService receipts;
    @Autowired private BookingHistoryService history;
    @Autowired private TicketCodeService codes;
    @Autowired private edu.hcmute.cnpm.cinema.controller.DemoWalletSessions walletSessions;

    private record Fixture(User user, Showtime showtime, List<Ticket> tickets) {}
    private Fixture fixture() {
        testDataFactory.createVoucher("UTE10");
        User user = testDataFactory.createCustomer("voucher@example.com");
        Movie movie = testDataFactory.createMovie("Phim ưu đãi");
        Room room = testDataFactory.createRoom("Cinema 1", 2, 4);
        Showtime showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(3));
        Ticket first = testDataFactory.newHeldTicket(showtime, testDataFactory.createSeat(room, "A", 1), user);
        Ticket second = testDataFactory.newHeldTicket(showtime, testDataFactory.createSeat(room, "A", 2), user);
        first.setPrice(new BigDecimal("100000")); second.setPrice(new BigDecimal("100000"));
        return new Fixture(user, showtime, ticketRepository.saveAll(List.of(first, second)));
    }

    @Test @DisplayName("Áp dụng mã, kiểm tra số tiền cổng thanh toán, hóa đơn và hoàn vé")
    void shouldPersistDiscountThroughPaymentAndRefund() throws Exception {
        Fixture f = fixture();
        List<Long> ids = f.tickets().stream().map(Ticket::getId).toList();
        BookingOrder order = orders.applyVoucher(f.user().getId(), f.showtime().getId(), " ute10 ", ids);
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("20000");
        ConcessionProduct product = testDataFactory.createConcessionProduct("VOUCHER-COMBO", new BigDecimal("79000"));
        orders.saveConcessions(f.user().getId(), f.showtime().getId(), java.util.Map.of(product.getId(), 1));
        assertThat(payments.prepareCheckout(f.user().getId(), f.showtime().getId(), ids).total()).isEqualByComparingTo("259000");
        assertThatThrownBy(() -> payments.confirmPayment(f.user().getId(), f.showtime().getId(),
                PaymentMethod.MOMO, "wrong-amount", ids, null, 200000L)).hasMessageContaining("không khớp");
        List<Ticket> paid = payments.confirmPayment(f.user().getId(), f.showtime().getId(),
                PaymentMethod.MOMO_DEMO, "voucher-payment", ids, null, 259000L);
        assertThat(paid).allSatisfy(t -> assertThat(t.getPrice()).isEqualByComparingTo("90000"));
        mvc.perform(get("/thanh-toan/hoan-tat").sessionAttr(Constants.SESSION_USER, f.user())
                .flashAttr("paidTicketIds", ids).flashAttr("receiptCode", order.getReceiptCode()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/hoa-don/" + order.getReceiptCode()));
        mvc.perform(get("/hoa-don/{code}", order.getReceiptCode()).sessionAttr(Constants.SESSION_USER, f.user()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("invoice-paper")))
                .andExpect(content().string(containsString("UTE10")))
                .andExpect(content().string(containsString("259.000 đ")))
                .andExpect(content().string(containsString("QR chung 2 ghế")));
        jdbcTemplate.update("UPDATE vouchers SET discount_percent = 5 WHERE code = 'UTE10'");
        var receipt = receipts.findReceipt(order.getReceiptCode(), f.user());
        assertThat(receipt.order().getTotalAmount()).isEqualByComparingTo("259000");
        assertThat(receipt.lines().stream().filter(line -> line.unit().equals("Vé")).toList())
                .allSatisfy(line -> assertThat(line.amount()).isEqualByComparingTo("100000"));
        TicketRefund refund = refunds.cancelPaidTicket(f.user().getId(), paid.getFirst().getId(), LocalDateTime.now());
        assertThat(refund.getRefundAmount()).isEqualByComparingTo("90000");
        assertThat(refund.getOriginalPrice()).isEqualByComparingTo("100000");
        assertThat(receipts.findReceipt(order.getReceiptCode(), f.user()).lines()).hasSize(3);
        var detail = history.findDetail(order.getReceiptCode(), f.user().getId());
        assertThat(detail.refundedAmount()).isEqualByComparingTo("90000");
        assertThat(detail.discountAmount()).isEqualByComparingTo("20000");
        assertThat(detail.tickets()).hasSize(2).allSatisfy(ticket -> {
            assertThat(ticket.publicCode()).isEqualTo(codes.codeFor(ticket.ticketId())).matches("[1-9][0-9]{7}");
            assertThat(ticket.price()).isEqualByComparingTo("100000");
            if (ticket.refunded()) assertThat(ticket.qrSvg()).isNull();
            else assertThat(ticket.qrSvg()).contains("QR vé " + ticket.publicCode());
        });
    }

    @Test @DisplayName("Giao diện hiển thị menu, báo mã sai, áp dụng và bỏ mã với bảo vệ CSRF")
    void shouldApplyAndRemoveVoucherFromCheckout() throws Exception {
        Fixture f = fixture();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(Constants.SESSION_USER, f.user());
        String csrf = walletSessions.csrf(session);
        String[] ids = f.tickets().stream().map(t -> t.getId().toString()).toArray(String[]::new);
        mvc.perform(get("/uu-dai").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("UTE10")))
                .andExpect(content().string(containsString("account-dropdown")))
                .andExpect(content().string(containsString("Lịch sử đặt vé")));
        mvc.perform(post("/thanh-toan/{id}/uu-dai", f.showtime().getId()).session(session)
                .param("voucherCode", "UTE10").param("ticketIds", ids).param("walletCsrf", "wrong"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists(Constants.MODEL_ERROR_MESSAGE));
        mvc.perform(post("/thanh-toan/{id}/uu-dai", f.showtime().getId()).session(session)
                .param("voucherCode", "BAD").param("ticketIds", ids).param("walletCsrf", csrf))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists(Constants.MODEL_ERROR_MESSAGE));
        mvc.perform(post("/thanh-toan/{id}/uu-dai", f.showtime().getId()).session(session)
                .param("voucherCode", "UTE10").param("ticketIds", ids).param("walletCsrf", csrf))
                .andExpect(redirectedUrl("/thanh-toan/" + f.showtime().getId()));
        mvc.perform(get("/thanh-toan/{id}", f.showtime().getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("180.000 đ")));
        mvc.perform(post("/thanh-toan/{id}/uu-dai", f.showtime().getId()).session(session)
                .param("voucherCode", "").param("ticketIds", ids).param("walletCsrf", csrf))
                .andExpect(status().is3xxRedirection());
        assertThat(payments.totalDue(f.user().getId(), f.showtime().getId())).isEqualByComparingTo("200000");
        mvc.perform(get("/lich-su-dat-ve").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Lịch sử đặt vé")));
    }

    @Test @DisplayName("Không áp dụng mã cho vé của tài khoản khác hoặc lượt giữ cũ")
    void shouldRejectOtherUsersAndStaleHolds() {
        Fixture f = fixture();
        User other = testDataFactory.createCustomer("other-voucher@example.com");
        List<Long> ids = f.tickets().stream().map(Ticket::getId).toList();
        assertThatThrownBy(() -> orders.applyVoucher(other.getId(), f.showtime().getId(), "UTE10", ids));
        assertThatThrownBy(() -> orders.applyVoucher(f.user().getId(), f.showtime().getId(), "UTE10", List.of(-1L)));
        assertThat(payments.totalDue(f.user().getId(), f.showtime().getId())).isEqualByComparingTo("200000");
    }

    @Test @DisplayName("Tên voucher dài 200 ký tự được lưu đầy đủ trên đơn và hóa đơn")
    void shouldPreserveFullVoucherTitle() {
        Fixture f = fixture();
        String title = "Ưu đãi vé ".repeat(20);
        assertThat(title).hasSize(200);
        jdbcTemplate.update("UPDATE vouchers SET title = ? WHERE code = 'UTE10'", title);
        var ids = f.tickets().stream().map(Ticket::getId).toList();
        var draft = orders.applyVoucher(f.user().getId(), f.showtime().getId(), "UTE10", ids);
        assertThat(draft.getAppliedVoucherName()).isEqualTo(title);
        payments.confirmPayment(f.user().getId(), f.showtime().getId(),
                PaymentMethod.MOMO_DEMO, "long-voucher-title", ids, null, 180000L);
        assertThat(receipts.findReceipt(draft.getReceiptCode(), f.user()).order().getAppliedVoucherName())
                .isEqualTo(title);
    }

    @Test @DisplayName("Thiếu, trùng mã vé và giữ ghế hết hạn đều không được áp dụng voucher")
    void shouldRejectInvalidIdentityAndExpiredHold() {
        Fixture f = fixture();
        var ids = f.tickets().stream().map(Ticket::getId).toList();
        assertThatThrownBy(() -> orders.applyVoucher(f.user().getId(), f.showtime().getId(), "UTE10", null))
                .hasMessageContaining("Thiếu mã");
        assertThatThrownBy(() -> orders.applyVoucher(f.user().getId(), f.showtime().getId(), "UTE10",
                List.of(ids.getFirst(), ids.getFirst()))).isInstanceOf(edu.hcmute.cnpm.cinema.exception.BusinessException.class);
        jdbcTemplate.update("UPDATE tickets SET held_at = ? WHERE user_id = ?",
                LocalDateTime.now().minusMinutes(6), f.user().getId());
        assertThatThrownBy(() -> orders.applyVoucher(f.user().getId(), f.showtime().getId(), "UTE10", ids))
                .hasMessageContaining("giữ ghế hợp lệ");
        assertThat(ticketRepository.findAll()).allMatch(ticket -> ticket.getStatus() == TicketStatus.HELD);
        assertThat(ticketRefundRepository.count()).isZero();
    }

    @Test @DisplayName("Migration chạy lại giữ giá gốc của vé giảm giá khi khôi phục snapshot")
    void shouldBackfillOriginalPricesAndWidenTitle_whenMigrationsRerun() {
        Fixture f = fixture();
        var ids = f.tickets().stream().map(Ticket::getId).toList();
        var order = orders.applyVoucher(f.user().getId(), f.showtime().getId(), "UTE10", ids);
        payments.confirmPayment(f.user().getId(), f.showtime().getId(),
                PaymentMethod.MOMO_DEMO, "migration-voucher", ids, null, 180000L);
        jdbcTemplate.update("UPDATE booking_orders SET ticket_snapshot = NULL WHERE id = ?", order.getId());
        // Mô phỏng schema cũ của Tài, rồi chạy migration mở rộng hai lần.
        jdbcTemplate.execute("ALTER TABLE booking_orders ALTER COLUMN applied_voucher_name NVARCHAR(150) NULL");
        var scripts = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator();
        scripts.setSeparator("GO");
        scripts.addScript(new org.springframework.core.io.FileSystemResource(
                "database/migrations/20261003-voucher-title-length.sql"));
        scripts.addScript(new org.springframework.core.io.FileSystemResource(
                "database/migrations/20261002-booking-history-snapshots.sql"));
        scripts.execute(jdbcTemplate.getDataSource());
        scripts.execute(jdbcTemplate.getDataSource());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COL_LENGTH('dbo.booking_orders', 'applied_voucher_name')", Integer.class)).isEqualTo(400);
        var receipt = receipts.findReceipt(order.getReceiptCode(), f.user());
        assertThat(receipt.lines()).hasSize(2).allSatisfy(line -> assertThat(line.amount()).isEqualByComparingTo("100000"));
        assertThat(receipt.order().getTotalAmount()).isEqualByComparingTo("180000");
        assertThat(receipt.order().getDiscountAmount()).isEqualByComparingTo("20000");
        assertThat(ticketRepository.count()).isEqualTo(2);
    }

    @Test @DisplayName("Migration sửa tổng đơn nháp cũ nhưng giữ nguyên chứng từ đã trả và đã hủy")
    void shouldRepairOnlyDraftTotals_whenLegacyTotalsDiffer() {
        Fixture f = fixture();
        var ids = f.tickets().stream().map(Ticket::getId).toList();
        var draft = orders.applyVoucher(f.user().getId(), f.showtime().getId(), "UTE10", ids);
        var script = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator();
        script.setSeparator("GO");
        script.addScript(new org.springframework.core.io.FileSystemResource(
                "database/migrations/20261003-draft-order-totals.sql"));
        jdbcTemplate.update("UPDATE booking_orders SET total_amount = 200000 WHERE id = ?", draft.getId());
        script.execute(jdbcTemplate.getDataSource());
        script.execute(jdbcTemplate.getDataSource());
        assertThat(jdbcTemplate.queryForObject("SELECT total_amount FROM booking_orders WHERE id = ?",
                BigDecimal.class, draft.getId())).isEqualByComparingTo("180000");
        for (String status : List.of("PAID", "CANCELLED")) {
            jdbcTemplate.update("UPDATE booking_orders SET status = ?, total_amount = 200000 WHERE id = ?", status, draft.getId());
            script.execute(jdbcTemplate.getDataSource());
            assertThat(jdbcTemplate.queryForObject("SELECT total_amount FROM booking_orders WHERE id = ?",
                    BigDecimal.class, draft.getId())).isEqualByComparingTo("200000");
        }
        assertThat(ticketRepository.count()).isEqualTo(2);
        assertThat(demoPaymentRepository.count()).isZero();
    }

    @Test @DisplayName("Đổi điều kiện và ngừng voucher trong database có hiệu lực khi đọc lại")
    void shouldReadChangesFromDatabaseWithoutRestart() {
        Fixture f = fixture();
        var ids = f.tickets().stream().map(Ticket::getId).toList();
        orders.applyVoucher(f.user().getId(), f.showtime().getId(), "UTE10", ids);
        jdbcTemplate.update("UPDATE vouchers SET discount_percent = 5 WHERE code = 'UTE10'");
        assertThat(payments.totalDue(f.user().getId(), f.showtime().getId())).isEqualByComparingTo("190000");
        jdbcTemplate.update("UPDATE vouchers SET active = 0 WHERE code = 'UTE10'");
        assertThatThrownBy(() -> orders.applyVoucher(f.user().getId(), f.showtime().getId(), "UTE10", ids))
                .hasMessageContaining("ngừng áp dụng");
        assertThat(payments.totalDue(f.user().getId(), f.showtime().getId())).isEqualByComparingTo("200000");
    }
}
