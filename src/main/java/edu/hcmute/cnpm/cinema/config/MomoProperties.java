package edu.hcmute.cnpm.cinema.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Cấu hình cổng thanh toán MoMo và hàm ký HMAC-SHA256 theo đúng cách MoMo yêu cầu.
 *
 * Khoá đặt trong application-secrets.properties (không commit). Đang dùng bộ khoá của
 * MÔI TRƯỜNG THỬ mà MoMo công bố trong tài liệu cho nhà phát triển: tiền không đi thật.
 * Muốn nhận tiền thật thì rạp đăng ký doanh nghiệp với MoMo, rồi chỉ cần đổi endpoint
 * và ba khoá - code giữ nguyên.
 *
 * Thiếu một trong ba khoá thì tắt tạo giao dịch: trang thanh toán báo chưa sẵn sàng.
 */
@Component
public class MomoProperties {

    private final String endpoint;
    private final String partnerCode;
    private final String accessKey;
    private final String secretKey;
    private final String requestType;
    private final String baseUrl;

    public MomoProperties(@Value("${momo.endpoint:https://test-payment.momo.vn}") String endpoint,
                          @Value("${momo.partner-code:}") String partnerCode,
                          @Value("${momo.access-key:}") String accessKey,
                          @Value("${momo.secret-key:}") String secretKey,
                          @Value("${momo.request-type:payWithMethod}") String requestType,
                          @Value("${app.base-url:http://localhost:8082}") String baseUrl) {
        this.endpoint = trimSlash(endpoint);
        this.partnerCode = partnerCode.trim();
        this.accessKey = accessKey.trim();
        this.secretKey = secretKey.trim();
        this.requestType = requestType.trim();
        this.baseUrl = trimSlash(baseUrl);
    }

    public boolean isEnabled() {
        return !partnerCode.isEmpty() && !accessKey.isEmpty() && !secretKey.isEmpty();
    }

    /** HMAC-SHA256 dạng hex chữ thường - MoMo dùng cách này cho mọi chữ ký. */
    public String sign(String rawData) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(rawData.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Không tạo được chữ ký MoMo.", exception);
        }
    }

    /** Trang MoMo đưa khách quay về sau khi trả tiền. */
    public String getRedirectUrl() {
        return baseUrl + "/thanh-toan/momo/ket-qua";
    }

    /** Địa chỉ MoMo gọi thẳng về máy chủ để báo kết quả. Chạy trên localhost thì MoMo không gọi tới được. */
    public String getIpnUrl() {
        return baseUrl + "/thanh-toan/momo/ipn";
    }

    public String getEndpoint() { return endpoint; }
    public String getPartnerCode() { return partnerCode; }
    public String getAccessKey() { return accessKey; }
    public String getRequestType() { return requestType; }

    private static String trimSlash(String url) {
        String trimmed = url == null ? "" : url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
