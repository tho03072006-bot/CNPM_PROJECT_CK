package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.exception.BusinessException;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Mã OTP không lưu dạng thô; phiên chỉ lưu ID. Mỗi mục đích có mã riêng. */
@Service
public class OtpService {
    public enum Purpose { REGISTER, RESET, PAYMENT }
    public record View(String email, Instant expiresAt, Instant resendAfter, int remainingAttempts) {}
    private static final Duration LIFETIME = Duration.ofMinutes(5);
    private static final Duration SESSION_LIFETIME = Duration.ofMinutes(30);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private static final Duration SEND_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_ENTRIES = 1000;
    private final MailDelivery mail;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Challenge> challenges = new HashMap<>();
    private final Map<String, Window> windows = new HashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public OtpService(MailDelivery mail) { this(mail, Clock.systemUTC()); }
    OtpService(MailDelivery mail, Clock clock) { this.mail = mail; this.clock = clock; }

    public synchronized String begin(Purpose purpose, String email, Object payload, boolean deliver) {
        cleanup();
        if (challenges.size() >= MAX_ENTRIES)
            throw new BusinessException("Có nhiều yêu cầu xác thực. Vui lòng thử lại sau.");
        reserve(purpose, email);
        Challenge challenge = new Challenge(purpose, email, payload, clock.instant().plus(SESSION_LIFETIME), deliver);
        issue(challenge);
        String id = UUID.randomUUID().toString();
        challenges.put(id, challenge);
        return id;
    }

    public synchronized View describe(String id, Purpose purpose) {
        Challenge c = find(id, purpose);
        return new View(maskEmail(c.email), c.expiresAt, c.resendAfter, MAX_ATTEMPTS - c.attempts);
    }

    public synchronized void resend(String id, Purpose purpose) {
        Challenge c = find(id, purpose);
        reserve(purpose, c.email);
        issue(c);
    }

    public synchronized <T> T payload(String id, Purpose purpose, Class<T> type) {
        return type.cast(find(id, purpose).payload);
    }

    /** Chỉ huỷ mã sau khi nghiệp vụ commit thành công; khoá chặn hai lần xác thực đồng thời. */
    public synchronized <T> T verify(String id, Purpose purpose, String code, Supplier<T> action) {
        Challenge c = find(id, purpose);
        if (!clock.instant().isBefore(c.expiresAt))
            throw new BusinessException("Mã OTP đã hết hạn. Bạn hãy gửi lại mã.");
        if (c.attempts >= MAX_ATTEMPTS)
            throw new BusinessException("Bạn đã nhập sai 5 lần. Hãy yêu cầu mã mới.");
        c.attempts++;
        if (code == null || !code.matches("[0-9]{6}") || !MessageDigest.isEqual(c.hash, hash(c.salt, code)))
            throw new BusinessException("Mã OTP chưa đúng. Còn " + (MAX_ATTEMPTS - c.attempts) + " lần thử.");
        T result = action.get();
        challenges.remove(id);
        return result;
    }

    public synchronized void cancel(String id) { challenges.remove(id); }

    private Challenge find(String id, Purpose purpose) {
        cleanup();
        Challenge c = challenges.get(id);
        if (c == null || c.purpose != purpose)
            throw new BusinessException("Phiên xác thực đã hết hạn. Bạn hãy bắt đầu lại.");
        return c;
    }

    private void issue(Challenge c) {
        String code;
        do { code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000)); }
        while (c.hash != null && MessageDigest.isEqual(c.hash, hash(c.salt, code)));
        String salt = UUID.randomUUID().toString();
        String label = switch (c.purpose) {
            case REGISTER -> "đăng ký tài khoản";
            case RESET -> "khôi phục tài khoản";
            case PAYMENT -> "xác nhận thanh toán";
        };
        boolean sent = c.deliver && mail.send(c.email, "UTE Cinema - Mã OTP " + label,
                "Mã OTP " + label + " của bạn: " + code + "\n\nMã có hiệu lực 5 phút và chỉ dùng một lần. "
                + "Không chia sẻ mã này với bất kỳ ai. Nếu bạn không yêu cầu, hãy bỏ qua thư này.");
        // RESET luôn phản hồi giống nhau, không tiết lộ email có tài khoản hoặc gửi thư thất bại.
        if (!sent && c.purpose != Purpose.RESET)
            throw new BusinessException("Chưa gửi được email xác thực. Hãy kiểm tra địa chỉ email hoặc thử lại sau.");
        c.salt = salt;
        c.hash = hash(salt, code);
        c.expiresAt = clock.instant().plus(LIFETIME);
        c.resendAfter = clock.instant().plus(COOLDOWN);
        c.attempts = 0;
    }

    private void reserve(Purpose purpose, String email) {
        cleanup();
        String key = purpose + ":" + email;
        Window w = windows.get(key);
        if (w == null) {
            if (windows.size() >= MAX_ENTRIES)
                throw new BusinessException("Có nhiều yêu cầu gửi mã. Vui lòng thử lại sau.");
            w = new Window(clock.instant().plus(SEND_WINDOW));
            windows.put(key, w);
        }
        if (w.count >= 5) throw new BusinessException("Bạn đã yêu cầu 5 mã. Hãy thử lại sau 15 phút.");
        if (w.next != null && clock.instant().isBefore(w.next))
            throw new BusinessException("Vui lòng chờ 60 giây giữa các lần gửi mã.");
        w.count++;
        w.next = clock.instant().plus(COOLDOWN);
    }

    private void cleanup() {
        Instant now = clock.instant();
        challenges.entrySet().removeIf(e -> !now.isBefore(e.getValue().sessionExpiresAt));
        windows.entrySet().removeIf(e -> !now.isBefore(e.getValue().expiresAt));
    }
    private static byte[] hash(String salt, String code) {
        try { return MessageDigest.getInstance("SHA-256").digest((salt + code).getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        return email.substring(0, 1) + "***" + email.substring(at);
    }
    private static final class Challenge {
        final Purpose purpose;
        final String email;
        final Object payload;
        final Instant sessionExpiresAt;
        final boolean deliver;
        String salt;
        byte[] hash;
        Instant expiresAt;
        Instant resendAfter;
        int attempts;
        Challenge(Purpose purpose, String email, Object payload, Instant expiresAt, boolean deliver) {
            this.purpose = purpose; this.email = email; this.payload = payload;
            this.sessionExpiresAt = expiresAt; this.deliver = deliver;
        }
    }
    private static final class Window {
        final Instant expiresAt;
        Instant next;
        int count;
        Window(Instant expiresAt) { this.expiresAt = expiresAt; }
    }
}
