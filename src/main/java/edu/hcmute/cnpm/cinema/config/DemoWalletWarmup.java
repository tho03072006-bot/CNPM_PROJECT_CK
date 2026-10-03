package edu.hcmute.cnpm.cinema.config;

import edu.hcmute.cnpm.cinema.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Wake the online simulator once when the local cinema starts. No transaction is created. */
@Component
@ConditionalOnProperty(name = "demo-wallet.warmup-enabled", havingValue = "true")
public class DemoWalletWarmup {
    private static final Logger log = LoggerFactory.getLogger(DemoWalletWarmup.class);
    private final DemoWalletSettings settings;
    private final DemoWalletCompatibility compatibility;
    private Thread worker;

    @Autowired
    public DemoWalletWarmup(DemoWalletSettings settings, DemoWalletCompatibility compatibility) {
        this.settings = settings; this.compatibility = compatibility;
        if (settings.isEnabled()) settings.getPublicBaseUrl(); // Validate before customers reach checkout.
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (!settings.isEnabled() || worker != null) return;
        worker = Thread.ofVirtual().name("demo-wallet-warmup").start(this::warmUp);
    }

    void warmUp() {
        try {
            compatibility.warmUp();
            log.info("Vi MoMo gia lap Nhom 8 da san sang va dong bo checkout version {}.", DemoWalletCompatibility.CHECKOUT_VERSION);
        } catch (BusinessException incompatible) {
            log.warn("Vi MoMo gia lap chua san sang hoac chua dong bo; can cap nhat Render cung ban voi web rap.");
        } catch (RuntimeException unavailable) {
            log.warn("Chua danh thuc duoc vi MoMo gia lap. Kiem tra mang hoac mo trang vi Render; web rap van hoat dong.");
        }
    }

    @PreDestroy
    public synchronized void stop() {
        if (worker != null) worker.interrupt();
    }
}
