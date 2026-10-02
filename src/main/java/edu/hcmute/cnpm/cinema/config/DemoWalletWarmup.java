package edu.hcmute.cnpm.cinema.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

/** Wake the online simulator once when the local cinema starts. No transaction is created. */
@Component
@ConditionalOnProperty(name = "demo-wallet.warmup-enabled", havingValue = "true")
public class DemoWalletWarmup {
    private static final Logger log = LoggerFactory.getLogger(DemoWalletWarmup.class);
    private final DemoWalletSettings settings;
    private final ObjectMapper json;
    private final HttpClient client;
    private Thread worker;

    @Autowired
    public DemoWalletWarmup(DemoWalletSettings settings, ObjectMapper json) {
        this(settings, json, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    DemoWalletWarmup(DemoWalletSettings settings, ObjectMapper json, HttpClient client) {
        this.settings = settings; this.json = json; this.client = client;
        if (settings.isEnabled()) settings.getPublicBaseUrl(); // Validate before customers reach checkout.
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (!settings.isEnabled() || worker != null) return;
        worker = Thread.ofVirtual().name("demo-wallet-warmup").start(this::warmUp);
    }

    void warmUp() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(settings.getPublicBaseUrl() + "/demo-wallet/health"))
                    .timeout(Duration.ofSeconds(60)).header("Accept", "application/json").GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && "UP".equals(json.readTree(response.body()).path("status").asText())) {
                log.info("Vi MoMo gia lap Nhom 8 da san sang; QR mo giao dich HTTPS tren Render.");
            } else {
                log.warn("Vi MoMo gia lap chua san sang. Mo trang vi Render truoc khi giu ghe; web rap van hoat dong.");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException unavailable) {
            log.warn("Chua danh thuc duoc vi MoMo gia lap. Kiem tra mang hoac mo trang vi Render; web rap van hoat dong.");
        }
    }

    @PreDestroy
    public synchronized void stop() {
        if (worker != null) worker.interrupt();
        client.close();
    }
}
