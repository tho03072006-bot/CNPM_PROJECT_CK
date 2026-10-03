package edu.hcmute.cnpm.cinema.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Chỗ duy nhất trong ứng dụng thực sự gửi email đi. Mọi thư (xác nhận vé, đặt lại mật
 * khẩu, hoàn tiền) đều qua đây để cùng tuân theo ba luật:
 *
 * 1. Có công tắc {@code app.mail.enabled}. Profile test tắt nó đi: file bí mật có mật
 *    khẩu Gmail thật, không tắt thì mỗi lần chạy mvn test sẽ gửi thư thật.
 * 2. Không gửi tới tên miền dành riêng cho thử nghiệm (.local, .test, example.com...).
 *    Tài khoản mẫu dùng @utecinema.local; gửi tới đó Gmail sẽ trả thư báo lỗi về hộp
 *    thư của rạp.
 * 3. Nuốt mọi lỗi gửi thư, chỉ ghi log. Thư là phần phụ; server mail chập chờn không
 *    được làm hỏng giao dịch chính (khách đã trả tiền, đã hoàn tiền...).
 */
@Component
public class MailDelivery {

    private static final Logger log = LoggerFactory.getLogger(MailDelivery.class);

    /** Tên miền dành riêng cho thử nghiệm theo RFC 2606 và RFC 6762 - không bao giờ nhận được thư. */
    private static final List<String> RESERVED_SUFFIXES = List.of(
            ".local", ".test", ".example", ".invalid", ".localhost",
            "@example.com", "@example.net", "@example.org");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean isEnabled;
    private final String username;
    private final String fromAddress;

    public MailDelivery(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${app.mail.enabled:true}") boolean isEnabled,
                        @Value("${spring.mail.username:}") String username,
                        @Value("${app.mail.from:}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.isEnabled = isEnabled;
        this.username = username;
        // Gmail luôn ghi đè người gửi thành tài khoản đăng nhập, nên mặc định lấy luôn địa chỉ đó.
        this.fromAddress = !fromAddress.isBlank() ? fromAddress
                : username.isBlank() ? "khong-tra-loi@utecinema.local" : "UTE Cinema <" + username + ">";
    }

    /** Đã đủ cấu hình để gửi thư thật chưa. */
    public boolean isReady() {
        return isEnabled && !username.isBlank() && mailSenderProvider.getIfAvailable() != null;
    }

    /**
     * Gửi một thư chữ thường. Không bao giờ ném lỗi ra ngoài.
     *
     * @return true nếu máy chủ mail đã nhận thư, false nếu bỏ qua hoặc gửi hỏng
     */
    public boolean send(String to, String subject, String body) {
        if (!isEnabled) {
            log.info("app.mail.enabled=false, bo qua thu '{}'.", subject);
            return false;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || username.isBlank()) {
            log.info("Chua cau hinh email (spring.mail.username), bo qua thu '{}'.", subject);
            return false;
        }
        if (to == null || to.isBlank() || isReservedAddress(to)) {
            log.info("Dia chi {} thuoc ten mien thu nghiem, khong gui thu '{}'.", to, subject);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Da gui thu '{}' toi {}", subject, to);
            return true;
        } catch (RuntimeException exception) {
            log.warn("Gui thu '{}' toi {} that bai: {}", subject, to, exception.getMessage());
            return false;
        }
    }

    static boolean isReservedAddress(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return RESERVED_SUFFIXES.stream().anyMatch(normalized::endsWith);
    }
}
