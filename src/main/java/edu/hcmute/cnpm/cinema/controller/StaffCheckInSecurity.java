package edu.hcmute.cnpm.cinema.controller;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class StaffCheckInSecurity {
    public static final String SESSION_KEY = "staffCheckInCsrf";
    private final SecureRandom random = new SecureRandom();
    public String token(HttpSession session) {
        synchronized (session) {
            Object current = session.getAttribute(SESSION_KEY);
            if (current instanceof String value) return value;
            byte[] bytes = new byte[32]; random.nextBytes(bytes);
            String value = HexFormat.of().formatHex(bytes);
            session.setAttribute(SESSION_KEY, value);
            return value;
        }
    }
    public void verify(HttpSession session, String supplied) {
        Object current = session.getAttribute(SESSION_KEY);
        if (!(current instanceof String expected) || supplied == null || supplied.length() != 64
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8)))
            throw new BusinessException("Yêu cầu xác nhận vé không hợp lệ hoặc phiên đã hết hạn. Hãy tải lại trang soát vé.");
    }
}
