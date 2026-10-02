package edu.hcmute.cnpm.cinema.config;

import edu.hcmute.cnpm.cinema.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.Set;

/** Địa chỉ ví công khai lấy từ cấu hình, không lấy từ header Host của khách. */
@Component
public class DemoWalletSettings {
    private final boolean enabled;
    private final String baseUrl;
    private final boolean requirePublicUrl;
    @org.springframework.beans.factory.annotation.Autowired
    public DemoWalletSettings(@Value("${demo-wallet.enabled:false}") boolean enabled, @Value("${demo-wallet.public-base-url:${app.base-url:http://localhost:8082}}") String baseUrl, @Value("${demo-wallet.require-public-url:false}") boolean requirePublicUrl) {
        this.requirePublicUrl = requirePublicUrl;
        this.enabled = enabled;
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip().replaceAll("/+$", "");
    }
    public DemoWalletSettings(boolean enabled, String baseUrl) { this(enabled, baseUrl, false); }
    public boolean isEnabled() { return enabled; }
    public void requireEnabled() {
        if (!enabled) throw new BusinessException("Ví MoMo giả lập Nhóm 8 chưa được bật.");
    }
    public String getPublicBaseUrl() {
        try {
            URI uri = URI.create(baseUrl);
            boolean local = localHost(uri.getHost());
            if (requirePublicUrl && (local || !"https".equals(uri.getScheme())))
                throw new BusinessException("Chưa cấu hình địa chỉ ví HTTPS công khai. Hãy đặt demo-wallet.public-base-url bằng URL ví Render trước khi tạo QR.");
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || (uri.getPath() != null && !uri.getPath().isEmpty())
                    || uri.getPort() == 0 || uri.getPort() > 65535
                    || (!"https".equals(uri.getScheme()) && !("http".equals(uri.getScheme()) && local)))
                throw new IllegalArgumentException();
            return baseUrl;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException("Địa chỉ ví phải là HTTPS hợp lệ, hoặc localhost để thử trên cùng máy.");
        }
    }
    private static boolean localHost(String host) {
        return host != null && (Set.of("localhost","0.0.0.0","::1","[::1]").contains(host)
                || host.startsWith("127.") || host.endsWith(".localhost"));
    }
    public boolean isLocalOnly() { return localHost(URI.create(getPublicBaseUrl()).getHost()); }
}
