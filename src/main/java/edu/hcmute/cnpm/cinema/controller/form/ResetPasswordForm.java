package edu.hcmute.cnpm.cinema.controller.form;

import edu.hcmute.cnpm.cinema.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dữ liệu ở form đặt lại mật khẩu (mở từ link trong email). */
public class ResetPasswordForm {

    private String token;

    @NotBlank(message = "Bạn hãy nhập mật khẩu mới.")
    @Size(min = AuthService.MIN_PASSWORD_LENGTH, max = 72,
            message = "Mật khẩu mới phải dài từ " + AuthService.MIN_PASSWORD_LENGTH + " đến 72 ký tự.")
    private String newPassword;

    @NotBlank(message = "Bạn hãy nhập lại mật khẩu mới.")
    private String confirmPassword;

    public boolean isNewPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
