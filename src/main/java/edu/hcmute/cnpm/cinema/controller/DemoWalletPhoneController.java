package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.config.DemoWalletSettings;
import edu.hcmute.cnpm.cinema.dto.payment.DemoWalletRequest;
import edu.hcmute.cnpm.cinema.service.DemoWalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Ví và API thanh toán chỉ được bật trên backend online. © Nhóm 8. */
@Controller
@ConditionalOnProperty(name="demo-wallet.phone-enabled", havingValue="true")
public class DemoWalletPhoneController {
    private final DemoWalletService wallet;
    private final DemoWalletSettings settings;
    private final DemoWalletSessions sessions;
    public DemoWalletPhoneController(DemoWalletService wallet, DemoWalletSettings settings, DemoWalletSessions sessions) {
        this.wallet=wallet;this.settings=settings;this.sessions=sessions;
    }
    @GetMapping({"/demo-wallet","/demo-wallet/pay/{publicId}"})
    public String phone(@PathVariable(required=false) String publicId, HttpSession session, Model model) {
        settings.requireEnabled();model.addAttribute("publicId",publicId);model.addAttribute("walletCsrf",sessions.csrf(session));
        return "account/demo-wallet";
    }
    @PostMapping(value="/demo-wallet/api/{publicId}/status",consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE) @ResponseBody
    public DemoWalletService.View status(@PathVariable String publicId,@RequestBody DemoWalletRequest request,
            @RequestHeader(value="X-Demo-Wallet-CSRF",required=false) String csrf,HttpSession session) {
        sessions.verify(session,csrf);return wallet.wallet(publicId,request.token());
    }
    @PostMapping(value="/demo-wallet/api/{publicId}/confirm",consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE) @ResponseBody
    public DemoWalletService.View confirm(@PathVariable String publicId,@RequestBody DemoWalletRequest request,
            @RequestHeader(value="X-Demo-Wallet-CSRF",required=false) String csrf,HttpSession session) {
        sessions.verify(session,csrf);return wallet.confirm(publicId,request);
    }
    @PostMapping(value="/demo-wallet/api/{publicId}/cancel",consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE) @ResponseBody
    public DemoWalletService.View cancel(@PathVariable String publicId,@RequestBody DemoWalletRequest request,
            @RequestHeader(value="X-Demo-Wallet-CSRF",required=false) String csrf,HttpSession session) {
        sessions.verify(session,csrf);return wallet.cancel(publicId,request.token());
    }
}
