package edu.hcmute.cnpm.cinema.controller.form;

import jakarta.validation.constraints.NotBlank;

/** Dữ liệu người dùng nhập ở form đăng nhập. */
public class LoginForm {

    @NotBlank(message = "Bạn hãy nhập email.")
    private String email;

    @NotBlank(message = "Bạn hãy nhập mật khẩu.")
    private String password;

    /**
     * Đường dẫn muốn tới trước khi bị chặn vì chưa đăng nhập.
     *
     * Ví dụ khách bấm "Chọn ghế" lúc chưa đăng nhập: đăng nhập xong sẽ quay lại
     * đúng trang chọn ghế đó thay vì về trang chủ.
     */
    private String next;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNext() { return next; }
    public void setNext(String next) { this.next = next; }
}
