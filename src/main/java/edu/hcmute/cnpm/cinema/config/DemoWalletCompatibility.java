package edu.hcmute.cnpm.cinema.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.cnpm.cinema.exception.DemoWalletUnavailableException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.function.LongSupplier;

/** Check the online checkout contract before issuing a QR for a shared database. */
@Component
public class DemoWalletCompatibility {
    public static final String CHECKOUT_VERSION = "2";
    private enum State { READY, INCOMPATIBLE, UNAVAILABLE }
    private final DemoWalletSettings settings;
    private final boolean phoneBackend;
    private final ObjectMapper json;
    private final HttpClient client;
    private final LongSupplier nanoTime;
    private record Cached(State state, long checkedAt, long validUntil) {}
    private volatile Cached cached;

    @Autowired
    public DemoWalletCompatibility(DemoWalletSettings settings, ObjectMapper json,
            @Value("${demo-wallet.phone-enabled:false}") boolean phoneBackend) {
        this(settings, json, phoneBackend, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NEVER).build(), System::nanoTime);
    }

    DemoWalletCompatibility(DemoWalletSettings settings, ObjectMapper json, boolean phoneBackend,
            HttpClient client, LongSupplier nanoTime) {
        this.settings = settings; this.json = json; this.phoneBackend = phoneBackend;
        this.client = client; this.nanoTime = nanoTime;
    }

    public void requireCompatible() {
        settings.requireEnabled();
        if (phoneBackend || settings.isLocalOnly()) return;
        requireReady(check(Duration.ofSeconds(6)));
    }

    public void warmUp() {
        settings.requireEnabled();
        requireReady(check(Duration.ofSeconds(60)));
    }

    private State check(Duration timeout) {
        long started = nanoTime.getAsLong();
        Cached previous = cached;
        if (previous != null && started - previous.validUntil() < 0) return previous.state();
        State result = State.UNAVAILABLE;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(settings.getPublicBaseUrl() + "/demo-wallet/health"))
                    .timeout(timeout).header("Accept", "application/json").GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && response.body().length() <= 4096) {
                var body = json.readTree(response.body());
                if (body != null && "UP".equals(body.path("status").asText())) {
                    result = CHECKOUT_VERSION.equals(body.path("checkoutVersion").asText())
                            ? State.READY : State.INCOMPATIBLE;
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException unavailable) {
            // Do not expose HTTP/JDBC details or accept a redirect as a compatible wallet.
        }
        long validUntil = nanoTime.getAsLong() + Duration.ofSeconds(result == State.UNAVAILABLE ? 3 : 15).toNanos();
        synchronized (this) {
            if (cached == null || started - cached.checkedAt() >= 0) cached = new Cached(result, started, validUntil);
        }
        return result;
    }

    private void requireReady(State state) {
        if (state == State.INCOMPATIBLE)
            throw new DemoWalletUnavailableException("Ví MoMo giả lập chưa đồng bộ với web rạp. Vui lòng chọn cách trả khác hoặc thử lại sau khi ví được cập nhật.");
        if (state != State.READY)
            throw new DemoWalletUnavailableException("Chưa kết nối được ví MoMo giả lập. Vui lòng thử lại sau ít giây hoặc chọn cách trả khác; thời hạn giữ ghế vẫn giữ nguyên.");
    }

    @PreDestroy
    public void close() { client.close(); }
}
