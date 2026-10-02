package edu.hcmute.cnpm.cinema.account;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.service.AuthService;
import edu.hcmute.cnpm.cinema.service.MailDelivery;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("Module 3: đăng ký và khôi phục qua OTP trên web")
class AccountOtpIntegrationTest extends IntegrationTestBase {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @MockitoBean MailDelivery mail;
    private String code;
    @BeforeEach void captureMail() {
        when(mail.send(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            var matcher = Pattern.compile("[0-9]{6}").matcher((String) invocation.getArgument(2));
            if (matcher.find()) code = matcher.group();
            return true;
        });
    }
    @Test @DisplayName("Đăng ký không lưu tài khoản trước OTP và mã không dùng lại được")
    void shouldCreateAccountOnlyAfterOtp() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post("/dang-ky").session(session).param("fullName", "Nguyễn Văn An")
                .param("email", "otp.registration@gmail.com").param("phone", "0901234567")
                .param("password", "password123").param("confirmPassword", "password123"))
                .andExpect(redirectedUrl("/dang-ky/xac-thuc"));
        assertThat(userRepository.existsByEmail("otp.registration@gmail.com")).isFalse();
        mvc.perform(get("/dang-ky/xac-thuc").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("autocomplete=\"one-time-code\"")));
        mvc.perform(post("/dang-ky/xac-thuc").session(session).param("code", "invalid"))
                .andExpect(redirectedUrl("/dang-ky/xac-thuc"));
        assertThat(userRepository.existsByEmail("otp.registration@gmail.com")).isFalse();
        mvc.perform(post("/dang-ky/xac-thuc").session(session).param("code", code)).andExpect(redirectedUrl("/dang-nhap"));
        User user = auth.login("otp.registration@gmail.com", "password123");
        assertThat(user.getPasswordHash()).doesNotContain("password123");
        assertThat(session.getAttribute(Constants.SESSION_USER)).isNull();
        mvc.perform(post("/dang-ky/xac-thuc").session(session).param("code", code))
                .andExpect(redirectedUrl("/dang-ky/xac-thuc"));
    }
    @Test @DisplayName("Email OTP khôi phục dẫn tới form mật khẩu mới và mật khẩu cũ mất hiệu lực")
    void shouldRecoverPasswordThroughOtp() throws Exception {
        auth.register("Khôi Phục", "otp.recovery@gmail.com", null, "oldPassword123");
        var session = new MockHttpSession();
        mvc.perform(post("/quen-mat-khau").session(session).param("email", "otp.recovery@gmail.com"))
                .andExpect(redirectedUrl("/quen-mat-khau/xac-thuc"));
        mvc.perform(get("/quen-mat-khau/xac-thuc").session(session)).andExpect(status().isOk());
        String url = mvc.perform(post("/quen-mat-khau/xac-thuc").session(session).param("code", code))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(url).startsWith("/dat-lai-mat-khau?token=");
        String token = url.substring(url.indexOf("token=") + 6);
        mvc.perform(get(url).session(session)).andExpect(status().isOk());
        mvc.perform(post("/dat-lai-mat-khau").session(session).param("token", token)
                .param("newPassword", "newPassword123").param("confirmPassword", "newPassword123"))
                .andExpect(redirectedUrl("/dang-nhap"));
        assertThat(auth.login("otp.recovery@gmail.com", "newPassword123")).isNotNull();
        assertThatThrownBy(() -> auth.login("otp.recovery@gmail.com", "oldPassword123")).hasMessageContaining("không đúng");
        mvc.perform(get(url)).andExpect(redirectedUrl("/quen-mat-khau"));
    }
    @Test @DisplayName("Nhập sai định dạng, mật khẩu Unicode quá dài hoặc hai mật khẩu không khớp không gửi OTP")
    void shouldRejectInvalidRegistrationWithoutMail() throws Exception {
        mvc.perform(post("/dang-ky").param("fullName", "An").param("email", "invalid")
                .param("password", "abc123").param("confirmPassword", "abc124"))
                .andExpect(status().isOk()).andExpect(view().name("account/register"));
        mvc.perform(post("/dang-ky").param("fullName", "An").param("email", "invalid.unicode@gmail.com")
                .param("password", "ế".repeat(25)).param("confirmPassword", "ế".repeat(25)))
                .andExpect(status().isOk()).andExpect(view().name("account/register"));
        verifyNoInteractions(mail);
    }
    @Test @DisplayName("Trang đăng nhập không trả mật khẩu trong HTML khi nhập sai")
    void shouldNotRenderPasswordAfterLoginError() throws Exception {
        mvc.perform(post("/dang-nhap").param("email", "missing@gmail.com").param("password", "privatePassword"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(containsString("privatePassword"))));
    }
    @Test @DisplayName("Không thể gọi thẳng endpoint trả tại quầy hoặc MoMo để bỏ qua OTP")
    void shouldRejectPaymentWithoutOtp() throws Exception {
        var user = testDataFactory.createCustomer("otp.bypass@gmail.com");
        var session = new MockHttpSession(); session.setAttribute(Constants.SESSION_USER, user);
        for (String suffix : new String[]{"", "/momo", "/momo-qr"})
            mvc.perform(post("/thanh-toan/999" + suffix).session(session)).andExpect(redirectedUrl("/thanh-toan/999"));
        assertThat(ticketRepository.count()).isZero();
    }
    @Test @DisplayName("Không thể yêu cầu OTP cho phương thức tiền mặt của vé online")
    void shouldRejectCounterPayment() throws Exception {
        var user = testDataFactory.createCustomer("otp.cash-blocked@gmail.com");
        var session = new MockHttpSession(); session.setAttribute(Constants.SESSION_USER, user);
        mvc.perform(post("/thanh-toan/999/otp").session(session).param("method", "counter"))
                .andExpect(redirectedUrl("/thanh-toan/999"))
                .andExpect(flash().attribute(Constants.MODEL_ERROR_MESSAGE,
                        "Vé online chỉ hỗ trợ thanh toán bằng QR hoặc thẻ qua MoMo."));
        verifyNoInteractions(mail);
    }
    @Test @DisplayName("Đăng nhập xoay session và không chuyển tới đường dẫn ngoài")
    void shouldRotateSessionAndRejectExternalRedirect() throws Exception {
        auth.register("Đăng Nhập", "otp.login@gmail.com", null, "password123");
        var session = new MockHttpSession(); String previous = session.getId();
        mvc.perform(post("/dang-nhap").session(session).param("email", "otp.login@gmail.com")
                .param("password", "password123").param("next", "/\\evil.com"))
                .andExpect(redirectedUrl("/"));
        assertThat(session.getId()).isNotEqualTo(previous);
    }
}
