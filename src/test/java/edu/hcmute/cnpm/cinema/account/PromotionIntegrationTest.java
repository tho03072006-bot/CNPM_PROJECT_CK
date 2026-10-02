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
                .andExpect(status().isOk()).andExpect(view().name("account/payment-success"))
                .andExpect(content().string(containsString("Đặt vé thành công")))
                .andExpect(content().string(containsString("Xem và in hóa đơn")))
                .andExpect(content().string(containsString("259.000 đ")));
        mvc.perform(get("/hoa-don/{code}", order.getReceiptCode()).sessionAttr(Constants.SESSION_USER, f.user()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("receipt-card")))
                .andExpect(content().string(containsString("Ưu đãi (UTE10)")));
        jdbcTemplate.update("UPDATE vouchers SET discount_percent = 5 WHERE code = 'UTE10'");
        var receipt = receipts.findReceipt(order.getReceiptCode(), f.user());
        assertThat(receipt.order().getTotalAmount()).isEqualByComparingTo("259000");
        assertThat(receipt.lines().stream().filter(line -> line.unit().equals("Vé")).toList())
                .allSatisfy(line -> assertThat(line.amount()).isEqualByComparingTo("100000"));
        TicketRefund refund = refunds.cancelPaidTicket(f.user().getId(), paid.getFirst().getId(), LocalDateTime.now());
        assertThat(refund.getRefundAmount()).isEqualByComparingTo("90000");
        assertThat(refund.getOriginalPrice()).isEqualByComparingTo("100000");
        assertThat(receipts.findReceipt(order.getReceiptCode(), f.user()).lines()).hasSize(3);
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
