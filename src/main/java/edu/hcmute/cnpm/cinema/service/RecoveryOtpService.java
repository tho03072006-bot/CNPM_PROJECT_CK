package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;

/** Không tạo tài khoản mới và không cấp phiên đăng nhập khi khôi phục. */
@Service
public class RecoveryOtpService {
    private record Recovery(Long userId, String passwordHash) {}
    private final UserRepository users;
    private final PasswordResetService reset;
    private final OtpService otp;
    public RecoveryOtpService(UserRepository users, PasswordResetService reset, OtpService otp) {
        this.users = users; this.reset = reset; this.otp = otp;
    }
    public String begin(String input) {
        String email = AccountValidation.email(input);
        User user = users.findByEmail(email).orElse(null);
        Recovery recovery = user == null ? new Recovery(null, "") : new Recovery(user.getId(), user.getPasswordHash());
        return otp.begin(OtpService.Purpose.RESET, email, recovery, user != null);
    }
    public String verify(String id, String code) {
        return otp.verify(id, OtpService.Purpose.RESET, code, () -> {
            Recovery recovery = otp.payload(id, OtpService.Purpose.RESET, Recovery.class);
            User user = recovery.userId == null ? null : users.findById(recovery.userId).orElse(null);
            if (user == null || !user.getPasswordHash().equals(recovery.passwordHash))
                throw new BusinessException("Yêu cầu khôi phục không còn hiệu lực. Hãy bắt đầu lại.");
            return reset.createToken(user, Instant.now());
        });
    }
}
