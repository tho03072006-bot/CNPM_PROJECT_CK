package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.config.DemoWalletSettings;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Local chỉ chuyển sang ví HTTPS; không có giao diện/API xác nhận ví trên local. */
@RestController
@ConditionalOnProperty(name="demo-wallet.phone-enabled", havingValue="false", matchIfMissing=true)
public class DemoWalletRedirectController {
    private final DemoWalletSettings settings;
    public DemoWalletRedirectController(DemoWalletSettings settings) { this.settings=settings; }

    @GetMapping({"/demo-wallet","/demo-wallet/pay/{publicId}"})
    public ResponseEntity<Void> redirect(@PathVariable(required=false) String publicId) {
        settings.requireEnabled();
        if (publicId!=null && !publicId.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"))
            throw new ResourceNotFoundException("Mã giao dịch không hợp lệ.");
        if (settings.isLocalOnly()) throw new ResourceNotFoundException("Ví thanh toán chỉ hoạt động trên địa chỉ HTTPS online.");
        String path=publicId==null ? "/demo-wallet" : "/demo-wallet/pay/"+publicId;
        // Không nhận Host/query để ghép URL; Location không có fragment để trình duyệt giữ khóa QR.
        return ResponseEntity.status(302).location(URI.create(settings.getPublicBaseUrl()+path)).build();
    }
}
