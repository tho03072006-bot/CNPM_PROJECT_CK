package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.BookingController;
import edu.hcmute.cnpm.cinema.dto.booking.HoldSeatsRequest;
import edu.hcmute.cnpm.cinema.dto.booking.HoldSeatsResponse;
import edu.hcmute.cnpm.cinema.dto.booking.SeatMapView;
import edu.hcmute.cnpm.cinema.dto.booking.SeatView;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.exception.SeatAlreadyTakenException;
import edu.hcmute.cnpm.cinema.service.SeatBookingService;
import edu.hcmute.cnpm.cinema.service.SeatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Test MVC và template thật với service giả; không sử dụng database. */
@WebMvcTest(BookingController.class)
class BookingControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SeatService seatService;
    @MockitoBean
    private SeatBookingService seatBookingService;

    @Test
    @DisplayName("Trang chọn ghế dùng layout và thành phần giao diện chung")
    void shouldRenderSeatMap_whenShowtimeIsAvailable() throws Exception {
        when(seatBookingService.getMaximumAdmissionsPerBooking()).thenReturn(8);
        when(seatService.findSeatMap(1L)).thenReturn(new SeatMapView(1L, "Phim kiểm thử", "Phòng 1",
                LocalDateTime.now().plusDays(1), 8,
                List.of(new SeatView(1L, "A", 1, "NORMAL", new BigDecimal("75000.00"), "AVAILABLE"))));
        mockMvc.perform(get("/booking/showtime/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/seat-map"))
                .andExpect(content().string(containsString("Phim kiểm thử")))
                .andExpect(content().string(containsString("/css/style.css")))
                .andExpect(content().string(containsString("seat-map-scroll")))
                .andExpect(content().string(containsString("Bạn cần đăng nhập")))
                .andExpect(content().string(containsString("Tối đa 8 chỗ")))
                .andExpect(content().string(containsString("không tạo thêm một ghế trống đơn độc")))
                .andExpect(content().string(containsString("/booking/showtime/1/hold")));
    }

    @Test
    @DisplayName("API lấy danh tính từ session, không từ dữ liệu JSON của khách")
    void shouldUseSessionUser_whenHoldRequestContainsForgedUserId() throws Exception {
        BookingTestFixture fixture = new BookingTestFixture();
        when(seatBookingService.holdSeats(eq(1L), any(HoldSeatsRequest.class), same(fixture.customer)))
                .thenReturn(new HoldSeatsResponse(List.of(10L), new BigDecimal("75000.00"), LocalDateTime.now().plusMinutes(5)));
        mockMvc.perform(post("/booking/showtime/1/hold")
                        .sessionAttr(Constants.SESSION_USER, fixture.customer)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\":[1],\"userId\":999,\"price\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.ticketIds[0]").value(10))
                .andExpect(jsonPath("$.totalPrice").value(75000));
        verify(seatBookingService).holdSeats(eq(1L), any(HoldSeatsRequest.class), same(fixture.customer));
    }

    @Test
    @DisplayName("Chưa đăng nhập nhận JSON lỗi nghiệp vụ qua handler chung")
    void shouldReturnBadRequest_whenSessionIsMissing() throws Exception {
        when(seatBookingService.holdSeats(eq(1L), any(HoldSeatsRequest.class), isNull()))
                .thenThrow(new InvalidBookingException("Bạn cần đăng nhập trước khi giữ ghế."));
        mockMvc.perform(post("/booking/showtime/1/hold")
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\":[1]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("đăng nhập")));
    }

    @Test
    @DisplayName("Tranh chấp ghế trả 409 và thông báo JSON của nhóm")
    void shouldReturnConflict_whenSeatIsTaken() throws Exception {
        when(seatBookingService.holdSeats(eq(1L), any(HoldSeatsRequest.class), isNull()))
                .thenThrow(new SeatAlreadyTakenException(1L, 1L));
        mockMvc.perform(post("/booking/showtime/1/hold")
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\":[1]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Suất chiếu không tồn tại được hiển thị bằng trang lỗi chung")
    void shouldReturnNotFound_whenShowtimeIsMissing() throws Exception {
        when(seatService.findSeatMap(99L)).thenThrow(new ResourceNotFoundException("suất chiếu", 99L));
        mockMvc.perform(get("/booking/showtime/99"))
                .andExpect(status().isNotFound()).andExpect(view().name("error"));
    }

    @Test
    @DisplayName("Dữ liệu JSON sai định dạng bị chặn trước khi gọi service")
    void shouldRejectRequest_whenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/booking/showtime/1/hold")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"seatIds\":[\"abc\"]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(seatBookingService);
    }

    @Test
    @DisplayName("API giữ ghế chỉ nhận JSON, không nhận form gửi từ trang khác")
    void shouldRejectRequest_whenContentTypeIsForm() throws Exception {
        mockMvc.perform(post("/booking/showtime/1/hold")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("seatIds", "1"))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(seatBookingService);
    }
}
