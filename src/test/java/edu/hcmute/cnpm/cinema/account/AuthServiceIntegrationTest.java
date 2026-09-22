package edu.hcmute.cnpm.cinema.account;

import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.AuthService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test dang ky va dang nhap (M3.1, M3.2).
 *
 * Diem quan trong nhat: mat khau KHONG BAO GIO duoc luu o dang tho.
 */
@DisplayName("Dang ky va dang nhap tai khoan")
class AuthServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("Mat khau luu xuong database phai la ban da bam, khong phai mat khau tho")
    void shouldHashPassword_whenRegistering() {
        User user = authService.register("Nguyen Van A", "vana@example.com", "0901234567", "matkhau123");

        assertThat(user.getPasswordHash())
                .as("Cot password_hash tuyet doi khong duoc chua mat khau tho")
                .isNotEqualTo("matkhau123");
        assertThat(user.getPasswordHash())
                .as("BCrypt luon bat dau bang $2a$, $2b$ hoac $2y$")
                .startsWith("$2");
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("Hai nguoi dang ky cung mat khau van ra hai chuoi bam khac nhau")
    void shouldProduceDifferentHashes_whenTwoUsersShareSamePassword() {
        User first = authService.register("Nguoi Mot", "mot@example.com", null, "matkhau123");
        User second = authService.register("Nguoi Hai", "hai@example.com", null, "matkhau123");

        assertThat(first.getPasswordHash())
                .as("BCrypt tu sinh muoi rieng cho tung mat khau nen hai chuoi phai khac nhau")
                .isNotEqualTo(second.getPasswordHash());
    }

    @Test
    @DisplayName("Email khong phan biet chu hoa chu thuong")
    void shouldNormalizeEmail_whenRegisteringAndLoggingIn() {
        authService.register("Nguyen Van A", "  VanA@Example.COM ", null, "matkhau123");

        User loggedIn = authService.login("vana@example.com", "matkhau123");
        assertThat(loggedIn.getEmail()).isEqualTo("vana@example.com");
    }

    @Test
    @DisplayName("Chan dang ky trung email")
    void shouldReject_whenEmailAlreadyRegistered() {
        authService.register("Nguoi Mot", "trung@example.com", null, "matkhau123");

        assertThatThrownBy(() -> authService.register("Nguoi Hai", "trung@example.com", null, "matkhau456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã có người đăng ký");
    }

    @Test
    @DisplayName("Chan mat khau qua ngan")
    void shouldReject_whenPasswordTooShort() {
        assertThatThrownBy(() -> authService.register("Nguoi Mot", "ngan@example.com", null, "123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(String.valueOf(AuthService.MIN_PASSWORD_LENGTH));
    }

    @Test
    @DisplayName("Dang nhap dung mat khau thi tra ve tai khoan")
    void shouldReturnUser_whenPasswordMatches() {
        authService.register("Nguyen Van A", "vana@example.com", null, "matkhau123");

        User loggedIn = authService.login("vana@example.com", "matkhau123");

        assertThat(loggedIn.getFullName()).isEqualTo("Nguyen Van A");
    }

    @Test
    @DisplayName("Sai mat khau va khong ton tai email deu bao cung mot cau")
    void shouldGiveSameMessage_whenEmailMissingOrPasswordWrong() {
        authService.register("Nguyen Van A", "vana@example.com", null, "matkhau123");

        String wrongPassword = messageOfFailedLogin("vana@example.com", "sai-mat-khau");
        String unknownEmail = messageOfFailedLogin("khongcoai@example.com", "matkhau123");

        assertThat(wrongPassword)
                .as("Bao khac nhau la nguoi la do duoc email nao da dang ky tren he thong")
                .isEqualTo(unknownEmail);
    }

    private String messageOfFailedLogin(String email, String password) {
        try {
            authService.login(email, password);
            throw new AssertionError("Dang nhap le ra phai that bai");
        } catch (BusinessException exception) {
            return exception.getMessage();
        }
    }
}
