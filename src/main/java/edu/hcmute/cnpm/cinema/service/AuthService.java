package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Đăng ký và đăng nhập tài khoản.
 *
 * Mật khẩu KHÔNG BAO GIỜ được lưu ở dạng thô: mọi đường vào đây đều đi qua
 * {@link PasswordEncoder} để băm bằng BCrypt.
 */
@Service
public class AuthService {

    /** Độ dài tối thiểu của mật khẩu, dùng chung cho cả kiểm tra ở form lẫn ở service. */
    public static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Tạo tài khoản khách hàng mới.
     *
     * Email luôn được chuẩn hoá về chữ thường, để "An@gmail.com" và "an@gmail.com"
     * không thành hai tài khoản khác nhau.
     */
    @Transactional
    public User register(String fullName, String email, String phone, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        validateRegistration(fullName, normalizedEmail, rawPassword);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Email này đã có người đăng ký. Bạn hãy đăng nhập hoặc dùng email khác.");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPhone(phone == null ? null : phone.trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(Role.CUSTOMER);
        return userRepository.save(user);
    }

    /**
     * Kiểm tra email và mật khẩu, trả về tài khoản nếu đúng.
     *
     * Sai email hay sai mật khẩu đều báo CÙNG một câu, để người lạ không dò được
     * email nào đã đăng ký trên hệ thống.
     */
    @Transactional(readOnly = true)
    public User login(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            throw new BusinessException("Bạn hãy nhập đầy đủ email và mật khẩu.");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Email hoặc mật khẩu không đúng."));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException("Email hoặc mật khẩu không đúng.");
        }
        return user;
    }

    /** Lấy lại thông tin mới nhất của tài khoản đang đăng nhập. */
    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Tài khoản không còn tồn tại. Bạn hãy đăng nhập lại."));
    }

    private void validateRegistration(String fullName, String email, String rawPassword) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("Bạn hãy nhập họ tên.");
        }
        if (email.isEmpty()) {
            throw new BusinessException("Bạn hãy nhập email.");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(
                    "Mật khẩu phải dài ít nhất " + MIN_PASSWORD_LENGTH + " ký tự.");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
