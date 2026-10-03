package edu.hcmute.cnpm.cinema.staff;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc
@DisplayName("Trang suất chiếu trong ngày: hiển thị và phân quyền")
class StaffShowtimeBoardIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private LocalDate day;

    @BeforeEach
    void createShowtime() {
        day = LocalDate.now().plusDays(2);
        Movie movie = testDataFactory.createMovie("Phim điều phối");
        Room room = testDataFactory.createRoom("Cinema 2", 1, 4);
        testDataFactory.createShowtime(movie, room, day.atTime(15, 0));
    }

    @Test
    @DisplayName("Nhân viên xem được lịch ngày: tên phim, phòng, giờ bắt đầu, giờ hết phim, khoảng nghỉ")
    void shouldShowDayBoard_toStaff() throws Exception {
        User staff = testDataFactory.createUserWithRole("nhanvien.lich@example.com", Role.STAFF);

        mockMvc.perform(get("/nhan-vien/suat-chieu").param("ngay", day.toString())
                        .sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/showtime-day"))
                .andExpect(content().string(containsString("Phim điều phối")))
                .andExpect(content().string(containsString("Cinema 2")))
                .andExpect(content().string(containsString("15:00")))
                .andExpect(content().string(containsString("17:00")))
                .andExpect(content().string(containsString("17:15")))
                .andExpect(content().string(containsString("15 phút")))
                .andExpect(content().string(containsString("/nhan-vien/suat-chieu")));
    }

    @Test
    @DisplayName("Quản trị viên cũng xem được")
    void shouldShowDayBoard_toAdmin() throws Exception {
        User admin = testDataFactory.createUserWithRole("quantri.lich@example.com", Role.ADMIN);

        mockMvc.perform(get("/nhan-vien/suat-chieu").sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ngày không có suất nào thì báo rõ, không ra trang trống")
    void shouldShowEmptyMessage_whenNoShowtimes() throws Exception {
        User staff = testDataFactory.createUserWithRole("nhanvien.trong@example.com", Role.STAFF);

        mockMvc.perform(get("/nhan-vien/suat-chieu").param("ngay", day.plusDays(30).toString())
                        .sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Không có suất chiếu nào")));
    }

    @Test
    @DisplayName("Khách hàng bị chặn, chưa đăng nhập thì chuyển sang trang đăng nhập")
    void shouldBlockCustomersAndAnonymousVisitors() throws Exception {
        User customer = testDataFactory.createCustomer("khach.lich@example.com");

        mockMvc.perform(get("/nhan-vien/suat-chieu").sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/nhan-vien/suat-chieu"))
                .andExpect(redirectedUrl("/dang-nhap?next=%2Fnhan-vien%2Fsuat-chieu"));
    }
}
