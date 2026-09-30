package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Validation tài khoản ở tầng nghiệp vụ")
class AccountValidationTest {
    @Test @DisplayName("Email được chuẩn hoá và chặn sai định dạng")
    void shouldValidateEmail() {
        assertThat(AccountValidation.email(" AN@GMAIL.COM ")).isEqualTo("an@gmail.com");
        for (String email : new String[]{"", "an", "an@", "a b@gmail.com", "a".repeat(150) + "@gmail.com"})
            assertThatThrownBy(() -> AccountValidation.email(email)).isInstanceOf(BusinessException.class);
    }
    @Test @DisplayName("BCrypt giới hạn 72 byte, không phải 72 ký tự")
    void shouldRejectOversizedUnicodePassword() {
        AccountValidation.password("a".repeat(72));
        AccountValidation.password("ế".repeat(24));
        for (String password : new String[]{"     ", "12345", "a".repeat(73), "ế".repeat(25)})
            assertThatThrownBy(() -> AccountValidation.password(password)).isInstanceOf(BusinessException.class);
    }
    @Test @DisplayName("Họ tên và điện thoại được kiểm tra trước khi lưu")
    void shouldValidateProfile() {
        assertThat(AccountValidation.name(" Nguyễn Văn An ")).isEqualTo("Nguyễn Văn An");
        assertThat(AccountValidation.phone(" 0901234567 ")).isEqualTo("0901234567");
        assertThat(AccountValidation.phone("")).isNull();
        assertThatThrownBy(() -> AccountValidation.name(" ")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> AccountValidation.phone("123")).isInstanceOf(BusinessException.class);
    }
}
