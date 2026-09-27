package edu.hcmute.cnpm.cinema.staff;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phân quyền cho các trang mới: khu vực nhân viên, trang quản lý người dùng, lối vào /admin.
 *
 * Gọi thẳng địa chỉ như cách người biết đường dẫn sẽ làm, giống AdminAccessIntegrationTest.
 */
@AutoConfigureMockMvc
@DisplayName("Phân quyền khu vực nhân viên và quản lý người dùng")
class StaffAccessIntegrationTest extends IntegrationTestBase {

    private static final String STAFF_PAGE = "/nhan-vien/soat-ve";
    private static final String USER_ADMIN_PAGE = "/admin/users";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Chưa đăng nhập hoặc là khách hàng thì không vào được trang soát vé")
    void shouldForbidStaffPage_whenAnonymousOrCustomer() throws Exception {
        User customer = testDataFactory.createCustomer("khach@example.com");

        mockMvc.perform(get(STAFF_PAGE)).andExpect(status().isForbidden());
        mockMvc.perform(get(STAFF_PAGE).sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Nhân viên và quản trị viên đều vào được trang soát vé")
    void shouldAllowStaffPage_whenStaffOrAdmin() throws Exception {
        User staff = testDataFactory.createUserWithRole("nhanvien@example.com", Role.STAFF);
        User admin = testDataFactory.createUserWithRole("admin@example.com", Role.ADMIN);

        mockMvc.perform(get(STAFF_PAGE).sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk());
        mockMvc.perform(get(STAFF_PAGE).sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Chỉ quản trị viên vào được trang quản lý người dùng, nhân viên thì không")
    void shouldAllowUserAdminPageOnlyForAdmin() throws Exception {
        User customer = testDataFactory.createCustomer("khach@example.com");
        User staff = testDataFactory.createUserWithRole("nhanvien@example.com", Role.STAFF);
        User admin = testDataFactory.createUserWithRole("admin@example.com", Role.ADMIN);

        mockMvc.perform(get(USER_ADMIN_PAGE)).andExpect(status().isForbidden());
        mockMvc.perform(get(USER_ADMIN_PAGE).sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(USER_ADMIN_PAGE).sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(USER_ADMIN_PAGE).sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nhanvien@example.com")));
    }

    @Test
    @DisplayName("Gõ /admin thì quản trị viên vào trang thống kê, người khác bị chặn")
    void shouldRedirectAdminRoot_toStatsPage() throws Exception {
        User admin = testDataFactory.createUserWithRole("admin@example.com", Role.ADMIN);

        mockMvc.perform(get("/admin")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin").sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/thong-ke"));
    }

    @Test
    @DisplayName("Soát mã của vé chưa thanh toán thì trang hiện rõ CHƯA THANH TOÁN")
    void shouldShowNotPaidVerdict_whenStaffChecksHeldTicket() throws Exception {
        User staff = testDataFactory.createUserWithRole("nhanvien@example.com", Role.STAFF);
        User customer = testDataFactory.createCustomer("khach@example.com");
        Movie movie = testDataFactory.createMovie("Phim soát vé");
        Room room = testDataFactory.createRoom("Cinema 2", 1, 4);
        Seat seat = testDataFactory.createSeat(room, "A", 1);
        Showtime showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(1));
        Ticket held = ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat, customer));

        mockMvc.perform(get(STAFF_PAGE).param("ma", "#" + held.getId())
                        .sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CHƯA THANH TOÁN")))
                .andExpect(content().string(containsString("#" + held.getId())));
    }

    @Test
    @DisplayName("Gõ sai mã vé thì hiện lời nhắc ngay trên trang, không đá sang trang lỗi")
    void shouldShowMessageOnPage_whenTicketCodeIsInvalid() throws Exception {
        User staff = testDataFactory.createUserWithRole("nhanvien@example.com", Role.STAFF);

        mockMvc.perform(get(STAFF_PAGE).param("ma", "abc").sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mã vé chỉ gồm chữ số")));
    }
}
