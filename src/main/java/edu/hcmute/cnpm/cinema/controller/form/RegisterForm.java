package edu.hcmute.cnpm.cinema.controller.form;

import edu.hcmute.cnpm.cinema.service.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dữ liệu người dùng nhập ở form đăng ký tài khoản. */
public class RegisterForm {

    @NotBlank(message = "Bạn hãy nhập họ tên.")
    @Size(max = 150, message = "Họ tên không được dài quá 150 ký tự.")
    private String fullName;

    @NotBlank(message = "Bạn hãy nhập email.")
    @Email(message = "Email chưa đúng định dạng.")
    @Size(max = 150, message = "Email không được dài quá 150 ký tự.")
    private String email;

    @Pattern(regexp = "^$|^0\\d{9,10}$",
            message = "Số điện thoại phải bắt đầu bằng số 0 và có 10 đến 11 chữ số.")
    private String phone;

    @NotBlank(message = "Bạn hãy nhập mật khẩu.")
    @Size(min = AuthService.MIN_PASSWORD_LENGTH, max = 72,
            message = "Mật khẩu phải dài từ " + AuthService.MIN_PASSWORD_LENGTH + " đến 72 ký tự.")
    private String password;

    @NotBlank(message = "Bạn hãy nhập lại mật khẩu.")
    private String confirmPassword;

    /** Hai ô mật khẩu có khớp nhau không. Kiểm tra ở Controller vì cần so hai trường. */
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
