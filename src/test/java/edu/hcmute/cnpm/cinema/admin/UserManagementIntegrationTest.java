package edu.hcmute.cnpm.cinema.admin;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.UserManagementService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quản trị viên cấp vai trò cho tài khoản. */
@AutoConfigureMockMvc
@DisplayName("Quản lý người dùng và cấp vai trò")
class UserManagementIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserManagementService userManagementService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Quản trị viên nâng khách hàng lên nhân viên được")
    void shouldPromoteCustomerToStaff_whenAdminChangesRole() {
        User admin = testDataFactory.createUserWithRole("admin@example.com", Role.ADMIN);
        User customer = testDataFactory.createCustomer("khach@example.com");

        userManagementService.changeRole(admin.getId(), customer.getId(), Role.STAFF);

        assertThat(userRepository.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(Role.STAFF);
    }

    @Test
    @DisplayName("Không ai tự đổi được vai trò của chính mình, tránh tự khoá mình ra ngoài")
    void shouldRejectChangingOwnRole() {
        User admin = testDataFactory.createUserWithRole("admin@example.com", Role.ADMIN);

        assertThatThrownBy(() -> userManagementService.changeRole(admin.getId(), admin.getId(), Role.CUSTOMER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chính mình");
        assertThat(userRepository.findById(admin.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Tìm tài khoản theo họ tên không dấu hoặc theo một phần email")
    void shouldFindUsers_whenKeywordMatchesNameOrEmail() {
        User customer = testDataFactory.createCustomer("an.nguyen@example.com");
        customer.setFullName("Nguyễn Văn An");
        userRepository.save(customer);
        testDataFactory.createCustomer("binh@example.com");

        assertThat(userManagementService.searchUsers("nguyen van"))
                .extracting(User::getEmail).containsExactly("an.nguyen@example.com");
        assertThat(userManagementService.searchUsers("BINH@"))
                .extracting(User::getEmail).containsExactly("binh@example.com");
        assertThat(userManagementService.searchUsers(null)).hasSize(2);
    }

    @Test
    @DisplayName("Đếm theo vai trò trả đủ cả ba vai trò, vai trò chưa có ai thì bằng 0")
    void shouldCountEveryRole_evenWhenNobodyHasIt() {
        testDataFactory.createCustomer("mot@example.com");
        testDataFactory.createCustomer("hai@example.com");
        testDataFactory.createUserWithRole("admin@example.com", Role.ADMIN);

        Map<Role, Long> counts = userManagementService.countUsersByRole();

        assertThat(counts).containsEntry(Role.CUSTOMER, 2L)
                .containsEntry(Role.ADMIN, 1L)
                .containsEntry(Role.STAFF, 0L);
    }

    @Test
    @DisplayName("Gửi form đổi vai trò qua trang quản trị thì lưu và báo thành công")
    void shouldSaveRoleAndFlashSuccess_whenAdminPostsForm() throws Exception {
        User admin = testDataFactory.createUserWithRole("admin@example.com", Role.ADMIN);
        User customer = testDataFactory.createCustomer("khach@example.com");

        mockMvc.perform(post("/admin/users/{id}/role", customer.getId()).param("role", "STAFF")
                        .sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists(Constants.MODEL_SUCCESS_MESSAGE));

        assertThat(userRepository.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(Role.STAFF);
    }

    @Test
    @DisplayName("Quản trị viên tự đổi vai trò mình qua form thì bị chặn và báo lỗi")
    void shouldFlashError_whenAdminTriesToChangeOwnRoleThroughForm() throws Exception {
        User admin = testDataFactory.createUserWithRole("admin@example.com", Role.ADMIN);

        mockMvc.perform(post("/admin/users/{id}/role", admin.getId()).param("role", "CUSTOMER")
                        .sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists(Constants.MODEL_ERROR_MESSAGE));

        assertThat(userRepository.findById(admin.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }
}
