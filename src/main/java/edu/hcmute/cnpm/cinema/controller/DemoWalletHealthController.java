package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.config.DemoWalletSettings;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kiểm tra ví và kết nối database, không công khai thông tin cấu hình hoặc đơn hàng. */
@RestController
public class DemoWalletHealthController {
    private final DemoWalletSettings settings;
    private final DataSource dataSource;

    public DemoWalletHealthController(DemoWalletSettings settings, DataSource dataSource) {
        this.settings = settings;
        this.dataSource = dataSource;
    }

    @GetMapping("/demo-wallet/health")
    public ResponseEntity<Map<String, String>> health() {
        if (!settings.isEnabled()) return unavailable();
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) return ResponseEntity.ok(Map.of("status", "UP"));
        } catch (SQLException ignored) {
            // Không trả lỗi JDBC hay thông tin database cho người truy cập Internet.
        }
        return unavailable();
    }

    private ResponseEntity<Map<String, String>> unavailable() {
        return ResponseEntity.status(503).body(Map.of("status", "UNAVAILABLE"));
    }
}
