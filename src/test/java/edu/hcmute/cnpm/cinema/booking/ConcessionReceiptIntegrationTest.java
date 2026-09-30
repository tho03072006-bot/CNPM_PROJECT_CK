package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.service.BookingOrderService;
import edu.hcmute.cnpm.cinema.service.PaymentService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

        mockMvc.perform(get("/hoa-don/{code}", receiptCode)
                        .sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hóa đơn thanh toán")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("233.000 đ")));
    }
}
