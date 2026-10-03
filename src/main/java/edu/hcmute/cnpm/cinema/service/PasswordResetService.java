package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * Quên mật khẩu: gửi link đặt lại qua email, rồi đặt mật khẩu mới bằng link đó.
 *
 * Link có dạng {@code /dat-lai-mat-khau?token=<mã tài khoản>.<hạn dùng>.<chữ ký>}. Chữ ký là
 * HMAC-SHA256 trên mã tài khoản, hạn dùng VÀ chuỗi băm mật khẩu hiện tại. Nhờ vậy không
 * cần bảng lưu token mà vẫn có đủ ba tính chất:
 *
 * - Không làm giả được: không có khoá bí mật thì không ký được.
 * - Tự hết hạn sau {@link #RESET_LINK_MINUTES} phút.
 * - Chỉ dùng được một lần: đặt mật khẩu mới xong thì chuỗi băm đổi, chữ ký cũ không còn khớp.
 *   Đổi mật khẩu bằng cách khác (trang Đổi mật khẩu) cũng làm link cũ hết hiệu lực.
 */
@Service
public class PasswordResetService {

    /** Link đặt lại mật khẩu dùng được trong bao lâu. */
    public static final int RESET_LINK_MINUTES = 30;

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String INVALID_LINK_MESSAGE =
            "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Bạn hãy yêu cầu gửi link mới.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailDelivery mailDelivery;
    private final byte[] secret;
    private final String baseUrl;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public PasswordResetService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                MailDelivery mailDelivery,
                                @Value("${app.password-reset.secret:}") String configuredSecret,
                                @Value("${app.base-url:http://localhost:8082}") String baseUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailDelivery = mailDelivery;
        this.secret = resolveSecret(configuredSecret);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * Gửi link đặt lại mật khẩu nếu email có tài khoản.
     *
     * Email không có tài khoản thì lặng lẽ bỏ qua: Controller luôn báo cùng một câu cho
     * cả hai trường hợp, để người lạ không dùng trang này dò xem email nào đã đăng ký.
     *
     * Cố ý không mở transaction: gửi thư mất vài giây, không nên giữ kết nối database suốt lúc đó.
     */
    public void requestReset(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Optional<User> user = userRepository.findByEmail(normalizedEmail);
        if (user.isEmpty()) {
            log.info("Yeu cau dat lai mat khau cho email chua dang ky, bo qua.");
            return;
        }
        String link = buildResetLink(createToken(user.get(), Instant.now()));
        mailDelivery.send(user.get().getEmail(), "UTE Cinema - Đặt lại mật khẩu", buildMailBody(user.get(), link));
    }

    /** Tạo mã đặt lại mật khẩu cho một tài khoản, hết hạn sau {@link #RESET_LINK_MINUTES} phút. */
    public String createToken(User user, Instant now) {
        long expiresAt = now.plusSeconds(RESET_LINK_MINUTES * 60L).getEpochSecond();
        return user.getId() + "." + expiresAt + "." + sign(user.getId(), expiresAt, user.getPasswordHash());
    }

    /** Tài khoản ứng với mã đặt lại mật khẩu, nếu mã còn hợp lệ. */
    @Transactional(readOnly = true)
    public User findUserByToken(String token, Instant now) {
        String[] parts = token == null ? new String[0] : token.trim().split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(INVALID_LINK_MESSAGE);
        }
        long userId;
        long expiresAt;
        try {
            userId = Long.parseLong(parts[0]);
            expiresAt = Long.parseLong(parts[1]);
        } catch (NumberFormatException exception) {
            throw new BusinessException(INVALID_LINK_MESSAGE);
        }
        if (now.getEpochSecond() >= expiresAt) {
            throw new BusinessException(INVALID_LINK_MESSAGE);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(INVALID_LINK_MESSAGE));
        byte[] expected = sign(userId, expiresAt, user.getPasswordHash()).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = parts[2].getBytes(StandardCharsets.US_ASCII);
        // So sánh thời gian cố định: không để lộ chữ ký đúng qua việc đo thời gian phản hồi.
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new BusinessException(INVALID_LINK_MESSAGE);
        }
        return user;
    }

    /** Đặt mật khẩu mới bằng mã trong link. Dùng xong mã tự hết hiệu lực. */
    @Transactional
    public User resetPassword(String token, String newPassword, Instant now) {
        AccountValidation.password(newPassword);
        User user = findUserByToken(token, now);
        userRepository.findByIdForBookingUpdate(user.getId())
                .orElseThrow(() -> new BusinessException(INVALID_LINK_MESSAGE));
        entityManager.refresh(user);
        // Đọc lại chữ ký dưới khoá: hai yêu cầu đồng thời không dùng lại được cùng token.
        user = findUserByToken(token, now);
        if (newPassword == null || newPassword.length() < AuthService.MIN_PASSWORD_LENGTH) {
            throw new BusinessException("Mật khẩu mới phải dài ít nhất " + AuthService.MIN_PASSWORD_LENGTH + " ký tự.");
        }
        if (newPassword.length() > 72) {
            throw new BusinessException("Mật khẩu mới không được dài quá 72 ký tự.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    /**
     * Link đầy đủ gửi trong thư. Lấy tên miền từ cấu hình {@code app.base-url} chứ không từ
     * request: nếu đọc header Host của request thì kẻ xấu gửi yêu cầu kèm Host giả là thư
     * gửi tới khách chứa link sang trang của họ (lỗi "password reset poisoning").
     */
    public String buildResetLink(String token) {
        return baseUrl + "/dat-lai-mat-khau?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private String buildMailBody(User user, String link) {
        return "Chào " + user.getFullName() + ",\n\n"
                + "Có người vừa yêu cầu đặt lại mật khẩu cho tài khoản UTE Cinema dùng email này.\n\n"
                + "Bấm vào link dưới đây để đặt mật khẩu mới (link dùng được trong "
                + RESET_LINK_MINUTES + " phút và chỉ dùng được một lần):\n\n"
                + link + "\n\n"
                + "Nếu không phải bạn yêu cầu thì cứ bỏ qua thư này, mật khẩu cũ vẫn giữ nguyên.\n\n"
                + "UTE Cinema\n";
    }

    private String sign(long userId, long expiresAt, String passwordHash) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            String payload = userId + "." + expiresAt + "." + passwordHash;
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Không tạo được chữ ký HMAC.", exception);
        }
    }

    private static byte[] resolveSecret(String configuredSecret) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return configuredSecret.getBytes(StandardCharsets.UTF_8);
        }
        log.warn("Chua cau hinh app.password-reset.secret - dung khoa ngau nhien, "
                + "link dat lai mat khau se het hieu luc moi lan khoi dong lai ung dung.");
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return random;
    }
}
