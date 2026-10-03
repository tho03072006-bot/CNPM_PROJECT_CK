package edu.hcmute.cnpm.cinema.account;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.AuthService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sửa hồ sơ và đổi mật khẩu của người đang đăng nhập. */
@AutoConfigureMockMvc
@DisplayName("Sửa hồ sơ và đổi mật khẩu")
class AccountSettingsIntegrationTest extends IntegrationTestBase {

    private static final String OLD_PASSWORD = "matkhaucu1";
    private static final String NEW_PASSWORD = "matkhaumoi2";

    @Autowired
    private AuthService authService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Sửa họ tên và số điện thoại hợp lệ thì lưu xuống database")
    void shouldSaveNameAndPhone_whenProfileIsValid() {
        User user = authService.register("Tên Cũ", "suahoso@example.com", null, OLD_PASSWORD);

        authService.updateProfile(user.getId(), "  Trần Văn Mới  ", "0912345678");

        User saved = userRepository.findById(user.getId()).orElseThrow();
        assertThat(saved.getFullName()).isEqualTo("Trần Văn Mới");
        assertThat(saved.getPhone()).isEqualTo("0912345678");
    }

    @Test
    @DisplayName("Để trống số điện thoại là xoá số đã lưu")
    void shouldClearPhone_whenPhoneLeftBlank() {
        User user = authService.register("Có Số", "coso@example.com", "0901234567", OLD_PASSWORD);

        authService.updateProfile(user.getId(), "Có Số", "   ");

        assertThat(userRepository.findById(user.getId()).orElseThrow().getPhone()).isNull();
    }

    @Test
    @DisplayName("Số điện thoại sai định dạng thì bị từ chối ở tầng Service")
    void shouldRejectProfile_whenPhoneIsMalformed() {
        User user = authService.register("Sai Số", "saiso@example.com", null, OLD_PASSWORD);

        assertThatThrownBy(() -> authService.updateProfile(user.getId(), "Sai Số", "12345"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Số điện thoại");
    }

    @Test
    @DisplayName("Đổi mật khẩu xong thì đăng nhập bằng mật khẩu mới được, mật khẩu cũ thì không")
    void shouldLoginWithNewPasswordOnly_whenPasswordChanged() {
        User user = authService.register("Đổi Mật Khẩu", "doimk@example.com", null, OLD_PASSWORD);

        authService.changePassword(user.getId(), OLD_PASSWORD, NEW_PASSWORD);

        assertThat(authService.login("doimk@example.com", NEW_PASSWORD).getId()).isEqualTo(user.getId());
        assertThatThrownBy(() -> authService.login("doimk@example.com", OLD_PASSWORD))
                .isInstanceOf(BusinessException.class);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPasswordHash())
                .as("Mật khẩu mới cũng phải được băm, không lưu thô")
                .isNotEqualTo(NEW_PASSWORD)
                .startsWith("$2");
    }

    @Test
    @DisplayName("Nhập sai mật khẩu hiện tại thì không đổi được")
    void shouldRejectPasswordChange_whenCurrentPasswordIsWrong() {
        User user = authService.register("Sai Mật Khẩu", "saimk@example.com", null, OLD_PASSWORD);

        assertThatThrownBy(() -> authService.changePassword(user.getId(), "khongphai", NEW_PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu hiện tại không đúng.");
        assertThat(authService.login("saimk@example.com", OLD_PASSWORD).getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Mật khẩu mới trùng mật khẩu cũ hoặc quá ngắn thì bị từ chối")
    void shouldRejectPasswordChange_whenNewPasswordIsSameOrTooShort() {
        User user = authService.register("Trùng Mật Khẩu", "trungmk@example.com", null, OLD_PASSWORD);

        assertThatThrownBy(() -> authService.changePassword(user.getId(), OLD_PASSWORD, OLD_PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("khác mật khẩu hiện tại");
        assertThatThrownBy(() -> authService.changePassword(user.getId(), OLD_PASSWORD, "123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ít nhất");
    }

    @Test
    @DisplayName("Sửa hồ sơ qua form thì tên trong phiên đăng nhập đổi theo ngay")
    void shouldRefreshSessionUser_whenProfileUpdatedThroughForm() throws Exception {
        User user = authService.register("Tên Trong Phiên", "phien@example.com", null, OLD_PASSWORD);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(Constants.SESSION_USER, user);

        mockMvc.perform(post("/tai-khoan/sua").session(session)
                        .param("fullName", "Tên Đã Sửa").param("phone", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tai-khoan"));

        User sessionUser = (User) session.getAttribute(Constants.SESSION_USER);
        assertThat(sessionUser.getFullName()).isEqualTo("Tên Đã Sửa");
    }

    @Test
    @DisplayName("Form đổi mật khẩu báo lỗi thì không in lại mật khẩu vừa gõ vào HTML")
    void shouldNotEchoPasswords_whenChangePasswordFormHasErrors() throws Exception {
        User user = authService.register("Không Lộ", "khonglo@example.com", null, OLD_PASSWORD);

        MvcResult result = mockMvc.perform(post("/tai-khoan/doi-mat-khau")
                        .sessionAttr(Constants.SESSION_USER, user)
                        .param("currentPassword", "BiMatHienTai9")
                        .param("newPassword", "BiMatMoi12345")
                        .param("confirmPassword", "GoSaiRoi12345"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Hai lần nhập mật khẩu mới chưa giống nhau.");
        assertThat(html).doesNotContain("BiMatHienTai9", "BiMatMoi12345", "GoSaiRoi12345");
    }

    @Test
    @DisplayName("Chưa đăng nhập thì gửi form sửa hồ sơ bị đưa về trang đăng nhập")
    void shouldRedirectToLogin_whenUpdatingProfileAnonymously() throws Exception {
        mockMvc.perform(post("/tai-khoan/sua").param("fullName", "Ai Đó"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dang-nhap?next=%2Ftai-khoan%2Fsua"));
    }
}
