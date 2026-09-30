package edu.hcmute.cnpm.cinema.account;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.AuthService;
import edu.hcmute.cnpm.cinema.service.PasswordResetService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quên mật khẩu: link đặt lại có chữ ký, hết hạn, dùng một lần, không cho dò email. */
@AutoConfigureMockMvc
@DisplayName("Quên mật khẩu và đặt lại mật khẩu")
class PasswordResetIntegrationTest extends IntegrationTestBase {

    private static final String OLD_PASSWORD = "matkhaucu1";
    private static final String NEW_PASSWORD = "matkhaumoi2";

    @Autowired
    private PasswordResetService passwordResetService;
    @Autowired
    private AuthService authService;
    @Autowired
    private MockMvc mockMvc;

    private User user;

    @BeforeEach
    void registerUser() {
        user = authService.register("Quên Mật Khẩu", "quenmk@gmail.com", null, OLD_PASSWORD);
    }

    @Test
    @DisplayName("Link hợp lệ thì đặt được mật khẩu mới, đăng nhập bằng mật khẩu mới được, mật khẩu cũ thì không")
    void shouldResetPassword_whenTokenIsValid() {
        Instant now = Instant.now();
        String token = passwordResetService.createToken(user, now);

        passwordResetService.resetPassword(token, NEW_PASSWORD, now.plusSeconds(60));

        assertThat(authService.login("quenmk@gmail.com", NEW_PASSWORD).getId()).isEqualTo(user.getId());
        assertThatThrownBy(() -> authService.login("quenmk@gmail.com", OLD_PASSWORD))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Link chỉ dùng được một lần: đặt mật khẩu xong thì link cũ hết hiệu lực")
    void shouldRejectToken_afterItWasUsedOnce() {
        Instant now = Instant.now();
        String token = passwordResetService.createToken(user, now);
        passwordResetService.resetPassword(token, NEW_PASSWORD, now);

        assertThatThrownBy(() -> passwordResetService.resetPassword(token, "matkhaukhac3", now))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không hợp lệ hoặc đã hết hạn");
    }

    @Test
    @DisplayName("Link quá 30 phút thì hết hạn")
    void shouldRejectToken_whenExpired() {
        Instant createdAt = Instant.now();
        String token = passwordResetService.createToken(user, createdAt);

        assertThatThrownBy(() -> passwordResetService.findUserByToken(token,
                createdAt.plusSeconds(PasswordResetService.RESET_LINK_MINUTES * 60L + 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Sửa mã tài khoản hoặc chữ ký trong link là bị từ chối")
    void shouldRejectToken_whenTampered() {
        Instant now = Instant.now();
        User other = authService.register("Người Khác", "nguoikhac@gmail.com", null, OLD_PASSWORD);
        String token = passwordResetService.createToken(user, now);
        String[] parts = token.split("\\.");

        String otherUserToken = other.getId() + "." + parts[1] + "." + parts[2];
        String brokenSignature = parts[0] + "." + parts[1] + "." + parts[2].substring(1) + "A";

        assertThatThrownBy(() -> passwordResetService.findUserByToken(otherUserToken, now))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> passwordResetService.findUserByToken(brokenSignature, now))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> passwordResetService.findUserByToken("rac", now))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Đổi mật khẩu ở trang tài khoản cũng làm link đặt lại cũ hết hiệu lực")
    void shouldRejectOldLink_whenPasswordChangedFromAccountPage() {
        Instant now = Instant.now();
        String token = passwordResetService.createToken(user, now);

        authService.changePassword(user.getId(), OLD_PASSWORD, NEW_PASSWORD);

        assertThatThrownBy(() -> passwordResetService.findUserByToken(token, now))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Link trong thư lấy tên miền từ cấu hình, không lấy từ header Host của request")
    void shouldBuildLinkFromConfiguredBaseUrl() {
        String link = passwordResetService.buildResetLink("1.2.abc");

        assertThat(link).isEqualTo("http://localhost:8082/dat-lai-mat-khau?token=1.2.abc");
    }

    @Test
    @DisplayName("Email có hay không có tài khoản đều nhận cùng một câu trả lời - không dò được email")
    void shouldAnswerTheSame_forKnownAndUnknownEmail() throws Exception {
        MvcResult known = mockMvc.perform(post("/quen-mat-khau").param("email", "quenmk@gmail.com"))
                .andExpect(status().is3xxRedirection()).andReturn();
        MvcResult unknown = mockMvc.perform(post("/quen-mat-khau").param("email", "khongai@gmail.com"))
                .andExpect(status().is3xxRedirection()).andReturn();

        Object knownMessage = known.getFlashMap().get(Constants.MODEL_SUCCESS_MESSAGE);
        assertThat(knownMessage).isNotNull();
        assertThat(unknown.getFlashMap().get(Constants.MODEL_SUCCESS_MESSAGE)).isEqualTo(knownMessage);
    }

    @Test
    @DisplayName("Đi hết luồng qua trang web: mở link, nhập mật khẩu mới, được đưa về trang đăng nhập")
    void shouldResetThroughWebPages_whenLinkIsValid() throws Exception {
        String token = passwordResetService.createToken(user, Instant.now());

        mockMvc.perform(get("/dat-lai-mat-khau").param("token", token))
                .andExpect(status().isOk());
        MvcResult mismatch = mockMvc.perform(post("/dat-lai-mat-khau").param("token", token)
                        .param("newPassword", "BiMatMoi12345").param("confirmPassword", "GoSai12345"))
                .andExpect(status().isOk()).andReturn();
        assertThat(mismatch.getResponse().getContentAsString())
                .contains("Hai lần nhập mật khẩu mới chưa giống nhau.")
                .doesNotContain("BiMatMoi12345", "GoSai12345");

        mockMvc.perform(post("/dat-lai-mat-khau").param("token", token)
                        .param("newPassword", NEW_PASSWORD).param("confirmPassword", NEW_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dang-nhap"))
                .andExpect(flash().attributeExists(Constants.MODEL_SUCCESS_MESSAGE));
        assertThat(authService.login("quenmk@gmail.com", NEW_PASSWORD).getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Mở link hỏng thì quay về trang quên mật khẩu kèm lời nhắc, không lộ lỗi hệ thống")
    void shouldRedirectWithMessage_whenOpeningBrokenLink() throws Exception {
        mockMvc.perform(get("/dat-lai-mat-khau").param("token", "1.2.sai"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quen-mat-khau"))
                .andExpect(flash().attributeExists(Constants.MODEL_ERROR_MESSAGE));
    }
}
