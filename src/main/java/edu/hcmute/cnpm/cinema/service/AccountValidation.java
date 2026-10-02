package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Kiểm tra lại ở tầng nghiệp vụ, kể cả khi không gọi từ form. */
public final class AccountValidation {
    private AccountValidation() {}
    public static String email(String value) {
        String email = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 150 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new BusinessException("Email chưa đúng định dạng hoặc dài quá 150 ký tự.");
        return email;
    }
    public static void password(String value) {
        if (value == null || value.isBlank() || value.length() < AuthService.MIN_PASSWORD_LENGTH)
            throw new BusinessException("Mật khẩu phải có ít nhất 6 ký tự và không được chỉ chứa khoảng trắng.");
        if (value.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BusinessException("Mật khẩu quá dài. Hãy dùng tối đa 72 byte UTF-8 (ký tự có dấu chiếm nhiều byte).");
    }
    public static String name(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 150)
            throw new BusinessException("Họ tên phải có từ 1 đến 150 ký tự.");
        return value.trim();
    }
    public static String phone(String value) {
        String phone = value == null || value.isBlank() ? null : value.trim();
        if (phone != null && !phone.matches("0[0-9]{9,10}"))
            throw new BusinessException("Số điện thoại phải bắt đầu bằng số 0 và có 10 đến 11 chữ số.");
        return phone;
    }
}
