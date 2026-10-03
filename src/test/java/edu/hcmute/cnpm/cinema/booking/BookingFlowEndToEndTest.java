package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.service.AuthService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M4.9 - Test luong dat ve tu dau den cuoi.
 *
 * Di het duong ma mot khach that se di: dang ky tai khoan, mo lich chieu,
 * giu ghe, thanh toan, roi xem lai ve cua minh. Moi buoc goi qua HTTP nhu
 * trinh duyet goi, khong goi tat vao service.
 *
 * Day la test chung minh BON module ghep lai voi nhau chay duoc, chu khong
 * phai tung module chay rieng thi dung.
 */
@AutoConfigureMockMvc
@DisplayName("M4.9 - Luong dat ve tu dau den cuoi")
class BookingFlowEndToEndTest extends IntegrationTestBase {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private edu.hcmute.cnpm.cinema.service.MailDelivery mailDelivery;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("Khach dang ky, giu ghe, thanh toan roi thay ve trong tai khoan")
    void shouldCompleteWholeBookingJourney() throws Exception {
        // ---------- Module 1 chuan bi: phim, phong, ghe, suat chieu ----------
        Movie movie = testDataFactory.createMovie("Phim kiem thu end-to-end");
        Room room = testDataFactory.createRoom("Cinema 1", 3, 3);
        Seat seatA1 = testDataFactory.createSeat(room, "A", 1);
        Seat seatA2 = testDataFactory.createSeat(room, "A", 2);
        Showtime showtime = testDataFactory.createShowtime(movie, room,
                LocalDateTime.now().plusDays(1).withHour(19).withMinute(0).withSecond(0).withNano(0));

        // ---------- Module 3: khach tao tai khoan ----------
        User customer = authService.register("Khach End To End", "endtoend@example.com",
                "0901234567", "matkhau123");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(Constants.SESSION_USER, customer);

        // ---------- Module 1: khach xem lich chieu ----------
        mockMvc.perform(get("/lich-chieu").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Phim kiem thu end-to-end")));

        // ---------- Module 2: mo so do ghe roi giu 2 ghe ----------
        mockMvc.perform(get("/booking/showtime/" + showtime.getId()).session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/booking/showtime/" + showtime.getId() + "/hold")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\":[" + seatA1.getId() + "," + seatA2.getId() + "] ,\"ageConfirmed\":true,\"termsAccepted\":true}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"success\":true")));

        List<Ticket> held = ticketRepository
                .findByUserIdAndShowtimeIdAndStatus(customer.getId(), showtime.getId(), TicketStatus.HELD);
        assertThat(held).as("Giu 2 ghe thi phai co dung 2 ve o trang thai dang giu").hasSize(2);

        // ---------- Module 3: trang thanh toan hien dung 2 ve ----------
        mockMvc.perform(get("/thanh-toan/" + showtime.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Xác nhận thanh toán")));

        // ---------- Module 3: xac nhan tra tien ----------
        java.util.concurrent.atomic.AtomicReference<String> otpCode = new java.util.concurrent.atomic.AtomicReference<>();
        org.mockito.Mockito.when(mailDelivery.send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            var matcher = java.util.regex.Pattern.compile("[0-9]{6}").matcher((String) invocation.getArgument(2));
            if (matcher.find()) otpCode.set(matcher.group());
            return true;
        });
        org.mockito.Mockito.when(momoApiClient.createQrPayment(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new edu.hcmute.cnpm.cinema.dto.payment.MomoCheckout(
                        "https://test-payment.momo.vn/pay", "momo://test-qr"));
        mockMvc.perform(post("/thanh-toan/" + showtime.getId() + "/otp").session(session).param("method", "momo-qr"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/thanh-toan/" + showtime.getId() + "/otp").session(session))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Xác thực thanh toán")));
        String qrPage = mockMvc.perform(post("/thanh-toan/" + showtime.getId() + "/momo-qr")
                        .session(session).param("code", otpCode.get()))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(qrPage).startsWith("/thanh-toan/momo/qr/");
        assertThat(ticketRepository.findByUserIdOrderByHeldAtDesc(customer.getId()))
                .allMatch(ticket -> ticket.getStatus() == TicketStatus.HELD);
        String orderId = qrPage.substring(qrPage.lastIndexOf('/') + 1);
        long amount = held.stream().map(Ticket::getPrice).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add).longValueExact();
        org.mockito.Mockito.when(momoApiClient.queryPayment(orderId))
                .thenReturn(new edu.hcmute.cnpm.cinema.dto.payment.MomoQueryResult(0, "Thành công", "4115000001", amount));
        mockMvc.perform(get(qrPage + "/trang-thai").session(session)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.state").value("PAID"));
        mockMvc.perform(get(qrPage + "/xong").session(session)).andExpect(status().is3xxRedirection());

        List<Ticket> afterPayment = ticketRepository.findByUserIdOrderByHeldAtDesc(customer.getId());
        assertThat(afterPayment).hasSize(2);
        assertThat(afterPayment).allSatisfy(ticket -> {
            assertThat(ticket.getStatus())
                    .as("Thanh toan xong ve phai chuyen sang da thanh toan")
                    .isEqualTo(TicketStatus.PAID);
            assertThat(ticket.getPaidAt())
                    .as("Phai ghi lai thoi diem thanh toan")
                    .isNotNull();
        });

        // ---------- Module 3: ve nam trong trang ve cua toi ----------
        mockMvc.perform(get("/ve-cua-toi").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/lich-su-dat-ve"));
        mockMvc.perform(get("/lich-su-dat-ve").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Phim kiem thu end-to-end")))
                .andExpect(content().string(containsString("Đã thanh toán")));

        // ---------- Module 2: ghe da ban khong con trong so do ----------
        mockMvc.perform(get("/booking/showtime/" + showtime.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("seat booked")));
    }

    @Test
    @DisplayName("Chua dang nhap thi khong giu ghe duoc")
    void shouldRefuseHold_whenNotLoggedIn() throws Exception {
        Movie movie = testDataFactory.createMovie("Phim chan khach vang lai");
        Room room = testDataFactory.createRoom("Cinema 1", 2, 2);
        Seat seat = testDataFactory.createSeat(room, "A", 1);
        Showtime showtime = testDataFactory.createShowtime(movie, room,
                LocalDateTime.now().plusDays(1).withHour(19).withMinute(0));

        mockMvc.perform(post("/booking/showtime/" + showtime.getId() + "/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\":[" + seat.getId() + "] ,\"ageConfirmed\":true,\"termsAccepted\":true}"))
                .andExpect(status().is4xxClientError());

        assertThat(ticketRepository.count())
                .as("Khong duoc tao ve nao khi chua dang nhap")
                .isZero();
    }
}
