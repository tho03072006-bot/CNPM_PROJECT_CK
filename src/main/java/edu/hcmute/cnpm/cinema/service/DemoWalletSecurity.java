package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.exception.BusinessException;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Khoá ngẫu nhiên 256 bit; chỉ lưu bản băm khoá thanh toán trong database. */
@Service
public class DemoWalletSecurity {
    private final SecureRandom random = new SecureRandom();
    public String newToken() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public String hash(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}"))
            throw new BusinessException("Liên kết thanh toán không hợp lệ hoặc thiếu khoá xác nhận.");
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    public void requireToken(String token, String storedHash) {
        if (!equal(hash(token), storedHash)) throw new BusinessException("Liên kết thanh toán không hợp lệ hoặc đã bị thay thế.");
    }
    public void requireCsrf(String expected, String actual) {
        if (expected == null || actual == null || !equal(expected, actual))
            throw new BusinessException("Phiên xác nhận không hợp lệ. Vui lòng tải lại trang ví.");
    }
    private boolean equal(String first, String second) {
        return second != null && MessageDigest.isEqual(first.getBytes(StandardCharsets.US_ASCII),
                second.getBytes(StandardCharsets.US_ASCII));
    }
}
