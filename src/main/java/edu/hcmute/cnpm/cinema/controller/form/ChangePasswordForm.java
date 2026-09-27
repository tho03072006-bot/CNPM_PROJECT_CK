package edu.hcmute.cnpm.cinema.controller.form;

import edu.hcmute.cnpm.cinema.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dữ liệu ở form đổi mật khẩu. */
public class ChangePasswordForm {

    @NotBlank(message = "Bạn hãy nhập mật khẩu hiện tại.")
    private String currentPassword;

    @NotBlank(message = "Bạn hãy nhập mật khẩu mới.")
    @Size(min = AuthService.MIN_PASSWORD_LENGTH, max = 72,
            message = "Mật khẩu mới phải dài từ " + AuthService.MIN_PASSWORD_LENGTH + " đến 72 ký tự.")
    private String newPassword;

    @NotBlank(message = "Bạn hãy nhập lại mật khẩu mới.")
    private String confirmPassword;

    /** Hai ô mật khẩu mới có khớp nhau không. Kiểm tra ở Controller vì cần so hai trường. */
    public boolean isNewPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
