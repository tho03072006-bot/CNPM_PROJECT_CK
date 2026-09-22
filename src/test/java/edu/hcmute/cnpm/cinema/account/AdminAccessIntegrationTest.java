package edu.hcmute.cnpm.cinema.account;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test phan quyen khu vuc quan tri (M3.3 va M3.9).
 *
 * Kiem tra bang cach goi THANG dia chi, dung cach ma nguoi biet duong dan se lam,
 * chu khong phai chi an nut tren giao dien di.
 */
@AutoConfigureMockMvc
@DisplayName("Phan quyen khu vuc quan tri")
class AdminAccessIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    /** Cac dia chi quan tri can duoc bao ve. */
    private static final String[] ADMIN_PATHS = {
            "/admin/movies", "/admin/rooms", "/admin/showtimes", "/admin/thong-ke"
    };

    @Test
    @DisplayName("Chua dang nhap thi bi tu choi o moi trang quan tri")
    void shouldForbid_whenNotLoggedIn() throws Exception {
        for (String path : ADMIN_PATHS) {
            mockMvc.perform(get(path)).andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Tai khoan khach hang goi thang dia chi quan tri cung bi tu choi")
    void shouldForbid_whenCustomerCallsAdminUrlDirectly() throws Exception {
        User customer = testDataFactory.createCustomer("khach@example.com");

        for (String path : ADMIN_PATHS) {
            mockMvc.perform(get(path).sessionAttr(Constants.SESSION_USER, customer))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Tai khoan quan tri thi vao duoc")
    void shouldAllow_whenAdminLoggedIn() throws Exception {
        User admin = testDataFactory.createCustomer("admin@example.com");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        for (String path : ADMIN_PATHS) {
            mockMvc.perform(get(path).sessionAttr(Constants.SESSION_USER, admin))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Tai khoan nhan vien chua duoc vao khu vuc quan tri")
    void shouldForbid_whenStaffLoggedIn() throws Exception {
        User staff = testDataFactory.createCustomer("nhanvien@example.com");
        staff.setRole(Role.STAFF);
        userRepository.save(staff);

        mockMvc.perform(get("/admin/thong-ke").sessionAttr(Constants.SESSION_USER, staff))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Trang cong khai van vao duoc khi chua dang nhap")
    void shouldAllowPublicPages_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/movies")).andExpect(status().isOk());
        mockMvc.perform(get("/lich-chieu")).andExpect(status().isOk());
        mockMvc.perform(get("/gia-ve")).andExpect(status().isOk());
        mockMvc.perform(get("/dang-nhap")).andExpect(status().isOk());
        mockMvc.perform(get("/dang-ky")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Trang tai khoan chuyen ve trang dang nhap khi chua dang nhap")
    void shouldRedirectToLogin_whenOpeningAccountPagesAnonymously() throws Exception {
        mockMvc.perform(get("/tai-khoan")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/ve-cua-toi")).andExpect(status().is3xxRedirection());
    }
}
