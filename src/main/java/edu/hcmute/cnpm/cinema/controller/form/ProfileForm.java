package edu.hcmute.cnpm.cinema.controller.form;

import edu.hcmute.cnpm.cinema.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dữ liệu ở form sửa hồ sơ. Luật kiểm tra giống hệt form đăng ký cho hai bên khớp nhau. */
public class ProfileForm {

    @NotBlank(message = "Bạn hãy nhập họ tên.")
    @Size(max = 150, message = "Họ tên không được dài quá 150 ký tự.")
    private String fullName;

    @Pattern(regexp = "^$|^0\\d{9,10}$",
            message = "Số điện thoại phải bắt đầu bằng số 0 và có 10 đến 11 chữ số.")
    private String phone;

    public static ProfileForm from(User user) {
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setPhone(user.getPhone());
        return form;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
