package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.service.BookingOrderService;
import edu.hcmute.cnpm.cinema.service.PaymentService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("Bắp nước và hóa đơn điện tử")
class ConcessionReceiptIntegrationTest extends IntegrationTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private BookingOrderService bookingOrderService;
    @Autowired private PaymentService paymentService;
    @Autowired private TicketRefundService ticketRefundService;

    @Test
    @DisplayName("Khách chọn combo trước thanh toán và in được hóa đơn sau khi trả tiền")
    void shouldIncludeConcessionsAndShowReceipt_whenPaymentCompleted() throws Exception {
        User customer = testDataFactory.createCustomer("combo@example.com");
        Movie movie = testDataFactory.createMovie("Phim combo");
        Room room = testDataFactory.createRoom("Cinema 1", 2, 4);
        Seat seat = testDataFactory.createSeat(room, "A", 1);
        Showtime showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(1));
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat, customer));

        ConcessionProduct combo = new ConcessionProduct();
        combo.setCode("COMBO-TEST");
        combo.setName("Combo một người");
        combo.setDescription("Bắp và nước");
        combo.setPrice(new BigDecimal("79000"));
        combo.setIcon("🍿");
        combo.setActive(true);
        combo.setDisplayOrder(1);
        combo.setStockQuantity(50);
        combo = concessionProductRepository.save(combo);

        mockMvc.perform(get("/bap-nuoc/{id}", showtime.getId())
                        .sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bạn có muốn mua bắp nước?")));

        mockMvc.perform(post("/bap-nuoc/{id}", showtime.getId())
                        .sessionAttr(Constants.SESSION_USER, customer)
                        .param("quantities[" + combo.getId() + "]", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/thanh-toan/" + showtime.getId()));

        BookingOrder draft = bookingOrderService.prepareForPayment(customer.getId(), showtime.getId());
        assertThat(draft.getConcessionSubtotal()).isEqualByComparingTo("158000");
        assertThat(draft.getTotalAmount()).isEqualByComparingTo("233000");

        Ticket paid = paymentService.confirmPayment(customer.getId(), showtime.getId()).get(0);
        String receiptCode = paid.getBookingOrder().getReceiptCode();

        String html = mockMvc.perform(get("/hoa-don/{code}", receiptCode)
                        .sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hóa đơn thanh toán")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("233.000 đ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Hai trăm ba mươi ba nghìn đồng chẵn.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vé xem phim - Ghế A1")))
                .andReturn().getResponse().getContentAsString();
        // Kiểm tra mẫu hóa đơn dạng thẻ được khôi phục và vẫn có các dòng tiền đầy đủ.
        String paper = html.substring(html.indexOf("receipt-card"), html.indexOf("</article>"));
        assertThat(paper).doesNotContain("<svg");

        assertThat(concessionProductRepository.findById(combo.getId()).orElseThrow().getStockQuantity())
                .as("Thanh toán xong phải trừ kho đúng 2 phần khách mua")
                .isEqualTo(48);
    }

    @Test
    @DisplayName("Vé hủy sau khi thanh toán vẫn nằm trên hóa đơn, ghi rõ đã hoàn bao nhiêu")
    void shouldKeepRefundedTicketOnReceipt() throws Exception {
        User customer = testDataFactory.createCustomer("huy-mot-ve@example.com");
        Movie movie = testDataFactory.createMovie("Phim hoàn vé");
        Room room = testDataFactory.createRoom("Cinema 3", 2, 4);
        Seat first = testDataFactory.createSeat(room, "C", 1);
        Seat second = testDataFactory.createSeat(room, "C", 2);
        Showtime showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(3));
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, first, customer));
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, second, customer));
        List<Ticket> paid = paymentService.confirmPayment(customer.getId(), showtime.getId());
        String receiptCode = paid.get(0).getBookingOrder().getReceiptCode();
        Ticket cancelled = paid.stream().filter(ticket -> ticket.getSeat().getId().equals(first.getId()))
                .findFirst().orElseThrow();

        ticketRefundService.cancelPaidTicket(customer.getId(), cancelled.getId(), LocalDateTime.now());

        mockMvc.perform(get("/hoa-don/{code}", receiptCode).sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vé xem phim - Ghế C1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vé xem phim - Ghế C2")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("hoàn lại 75.000 đ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("150.000 đ")));
    }

    @Test
    @DisplayName("Dữ liệu MOMO_DEMO cũ đọc được trên trang vé, tài khoản, hóa đơn và hủy vé")
    void shouldReadLegacyDemoPayments_withoutBreakingCustomerPages() throws Exception {
        User customer = testDataFactory.createCustomer("momo-demo@example.com");
        Movie movie = testDataFactory.createMovie("Phim thanh toán thử");
        Room room = testDataFactory.createRoom("Cinema Demo", 1, 2);
        Seat first = testDataFactory.createSeat(room, "A", 1);
        Seat second = testDataFactory.createSeat(room, "A", 2);
        Showtime showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(3));
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, first, customer));
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, second, customer));
        List<Ticket> paid = paymentService.confirmPayment(customer.getId(), showtime.getId());
        BookingOrder order = paid.get(0).getBookingOrder();
        TicketRefund refund = ticketRefundService.cancelPaidTicket(customer.getId(), paid.get(0).getId(),
                LocalDateTime.now());
        Ticket remaining = paid.get(1);

        // Ghi chuỗi từ dữ liệu cũ trực tiếp để kiểm tra Hibernate đọc cả ba bảng.
        jdbcTemplate.update("UPDATE tickets SET payment_method = 'MOMO_DEMO' WHERE id = ?", remaining.getId());
        jdbcTemplate.update("UPDATE booking_orders SET payment_method = 'MOMO_DEMO' WHERE id = ?", order.getId());
        jdbcTemplate.update("UPDATE ticket_refunds SET payment_method = 'MOMO_DEMO' WHERE id = ?", refund.getId());

        mockMvc.perform(get("/ve-cua-toi").sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MoMo giả lập Nhóm 8")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hoàn tiền mô phỏng")));
        mockMvc.perform(get("/tai-khoan").sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MoMo giả lập Nhóm 8")));
        mockMvc.perform(get("/hoa-don/{code}", order.getReceiptCode())
                        .sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MoMo giả lập Nhóm 8")));
        mockMvc.perform(get("/ve-cua-toi/{id}/huy", remaining.getId())
                        .sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("không chuyển tiền thật")));
    }

    @Test
    @DisplayName("Người khác không xem được hóa đơn của khách, nhân viên quầy thì xem được")
    void shouldGuardReceipt_whenViewerIsNotOwnerOrEmployee() throws Exception {
        User customer = testDataFactory.createCustomer("chu-hoa-don@example.com");
        User stranger = testDataFactory.createCustomer("nguoi-la@example.com");
        User staff = testDataFactory.createUserWithRole("quay@example.com", Role.STAFF);
        Movie movie = testDataFactory.createMovie("Phim hóa đơn");
        Room room = testDataFactory.createRoom("Cinema 2", 2, 4);
        Seat seat = testDataFactory.createSeat(room, "B", 2);
        Showtime showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(1));
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat, customer));
        String receiptCode = paymentService.confirmPayment(customer.getId(), showtime.getId())
                .get(0).getBookingOrder().getReceiptCode();

        mockMvc.perform(get("/hoa-don/{code}", receiptCode).sessionAttr(Constants.SESSION_USER, stranger))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/hoa-don/{code}", receiptCode).sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Khach hang test")));
    }
}
