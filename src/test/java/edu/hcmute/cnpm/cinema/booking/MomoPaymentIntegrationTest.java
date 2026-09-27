package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.config.MomoProperties;
import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.payment.MomoPaymentResult;
import edu.hcmute.cnpm.cinema.dto.payment.MomoPaymentResult.Outcome;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.PaymentMethod;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.MomoPaymentService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Thanh toán qua MoMo. Lớp gọi HTTP của MoMo là bản giả (xem IntegrationTestBase), còn
 * chữ ký thì ký thật bằng khoá test - để chứng minh chữ ký giả bị từ chối.
 */
@AutoConfigureMockMvc
@DisplayName("Thanh toán qua MoMo")
class MomoPaymentIntegrationTest extends IntegrationTestBase {

    private static final String TRANS_ID = "4115000001";

    @Autowired
    private MomoPaymentService momoPaymentService;
    @Autowired
    private MomoProperties momoProperties;
    @Autowired
    private MockMvc mockMvc;

    private User customer;
    private Showtime showtime;
    private List<Ticket> heldTickets;

    @BeforeEach
    void holdTwoSeats() {
        Movie movie = testDataFactory.createMovie("Phim MoMo");
        Room room = testDataFactory.createRoom("Cinema 2", 1, 6);
        showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(2));
        customer = testDataFactory.createCustomer("khach.momo@example.com");
        heldTickets = List.of(
                ticketRepository.save(testDataFactory.newHeldTicket(showtime, testDataFactory.createSeat(room, "A", 1), customer)),
                ticketRepository.save(testDataFactory.newHeldTicket(showtime, testDataFactory.createSeat(room, "A", 2), customer)));
    }

    @Test
    @DisplayName("Bắt đầu thanh toán thì tạo giao dịch MoMo đúng tổng tiền các ghế đang giữ")
    void shouldCreateMomoPayment_withTotalOfHeldSeats() {
        when(momoApiClient.createPayment(anyString(), anyLong(), anyString())).thenReturn("https://test-payment.momo.vn/pay?t=abc");

        String payUrl = momoPaymentService.startPayment(customer.getId(), showtime.getId());

        assertThat(payUrl).isEqualTo("https://test-payment.momo.vn/pay?t=abc");
        verify(momoApiClient).createPayment(
                argThat(orderId -> orderId.matches("UTE-" + showtime.getId() + "-" + customer.getId() + "-\\d+")),
                eq(150000L), anyString());
    }

    @Test
    @DisplayName("Kết quả có chữ ký đúng và thành công thì vé chuyển sang đã thanh toán qua MoMo")
    void shouldMarkTicketsPaidByMomo_whenSignedResultIsSuccessful() {
        MomoPaymentResult result = momoPaymentService.handleResult(signedResult(150000, "0"));

        assertThat(result.getOutcome()).isEqualTo(Outcome.PAID);
        for (Ticket ticket : ticketRepository.findAllById(result.getTicketIds())) {
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
            assertThat(ticket.getPaymentMethod()).isEqualTo(PaymentMethod.MOMO);
            assertThat(ticket.getPaymentRef()).isEqualTo(TRANS_ID);
        }
        assertThat(result.getTicketIds()).hasSize(2);
    }

    @Test
    @DisplayName("Khách tải lại trang kết quả thì chỉ báo đã thanh toán, không xử lý lại, không hoàn tiền nhầm")
    void shouldBeIdempotent_whenSameResultArrivesTwice() {
        Map<String, String> result = signedResult(150000, "0");
        momoPaymentService.handleResult(result);

        MomoPaymentResult second = momoPaymentService.handleResult(result);

        assertThat(second.getOutcome()).isEqualTo(Outcome.ALREADY_PAID);
        verify(momoApiClient, never()).refund(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Sửa số tiền sau khi MoMo ký là bị từ chối, vé vẫn đang giữ")
    void shouldRejectResult_whenSignatureDoesNotMatch() {
        Map<String, String> forged = signedResult(150000, "0");
        forged.put("amount", "1000");

        assertThatThrownBy(() -> momoPaymentService.handleResult(forged))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sai chữ ký");
        assertThat(ticketRepository.findAll()).allMatch(ticket -> ticket.getStatus() == TicketStatus.HELD);
    }

    @Test
    @DisplayName("Khách bấm huỷ bên MoMo thì ghế vẫn đang giữ, không có gì bị ghi nhận")
    void shouldKeepSeatsHeld_whenCustomerCancelsAtMomo() {
        MomoPaymentResult result = momoPaymentService.handleResult(signedResult(150000, "1006"));

        assertThat(result.getOutcome()).isEqualTo(Outcome.FAILED);
        assertThat(ticketRepository.findAll()).allMatch(ticket -> ticket.getStatus() == TicketStatus.HELD);
    }

    @Test
    @DisplayName("Ghế hết hạn giữ trong lúc khách đang trả tiền bên MoMo thì tự hoàn lại toàn bộ tiền")
    void shouldRefundAutomatically_whenHoldExpiredBeforeConfirmation() {
        for (Ticket ticket : heldTickets) {
            ticket.setHeldAt(LocalDateTime.now().minusMinutes(Constants.SEAT_HOLD_MINUTES + 5L));
            ticketRepository.save(ticket);
        }
        when(momoApiClient.refund(anyString(), eq(TRANS_ID), eq(150000L), anyString())).thenReturn("4215000001");

        MomoPaymentResult result = momoPaymentService.handleResult(signedResult(150000, "0"));

        assertThat(result.getOutcome()).isEqualTo(Outcome.REFUNDED);
        assertThat(result.getMessage()).contains("tự hoàn lại");
        verify(momoApiClient).refund(anyString(), eq(TRANS_ID), eq(150000L), anyString());
        assertThat(ticketRepository.findAll()).noneMatch(ticket -> ticket.getStatus() == TicketStatus.PAID);
    }

    @Test
    @DisplayName("MoMo đưa khách quay về trang kết quả thì chuyển sang trang đặt vé thành công")
    void shouldRedirectToSuccessPage_whenReturnUrlIsSigned() throws Exception {
        MockHttpServletRequestBuilder request = get("/thanh-toan/momo/ket-qua")
                .sessionAttr(Constants.SESSION_USER, customer);
        signedResult(150000, "0").forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/thanh-toan/hoan-tat"));
    }

    @Test
    @DisplayName("Trang thanh toán có nút MoMo khi rạp đã cấu hình MoMo")
    void shouldShowMomoButton_whenMomoConfigured() throws Exception {
        mockMvc.perform(get("/thanh-toan/{id}", showtime.getId()).sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thanh toán bằng ví MoMo")))
                .andExpect(content().string(containsString("Trả tiền mặt tại quầy")));
    }

    /** Dựng kết quả MoMo gửi về và ký bằng khoá test, đúng thứ tự trường MoMo quy định. */
    private Map<String, String> signedResult(long amount, String resultCode) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("partnerCode", momoProperties.getPartnerCode());
        params.put("orderId", "UTE-" + showtime.getId() + "-" + customer.getId() + "-1790000000000");
        params.put("requestId", params.get("orderId"));
        params.put("amount", String.valueOf(amount));
        params.put("orderInfo", "Thanh toan 2 ve UTE Cinema");
        params.put("orderType", "momo_wallet");
        params.put("transId", TRANS_ID);
        params.put("resultCode", resultCode);
        params.put("message", "0".equals(resultCode) ? "Thành công." : "Giao dịch bị từ chối bởi người dùng.");
        params.put("payType", "napas");
        params.put("responseTime", "1790000000123");
        params.put("extraData", "");
        String raw = "accessKey=" + momoProperties.getAccessKey()
                + "&amount=" + params.get("amount") + "&extraData=" + params.get("extraData")
                + "&message=" + params.get("message") + "&orderId=" + params.get("orderId")
                + "&orderInfo=" + params.get("orderInfo") + "&orderType=" + params.get("orderType")
                + "&partnerCode=" + params.get("partnerCode") + "&payType=" + params.get("payType")
                + "&requestId=" + params.get("requestId") + "&responseTime=" + params.get("responseTime")
                + "&resultCode=" + params.get("resultCode") + "&transId=" + params.get("transId");
        params.put("signature", momoProperties.sign(raw));
        return params;
    }
}
