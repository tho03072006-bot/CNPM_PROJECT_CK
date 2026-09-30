package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.service.OtpService;
import org.springframework.ui.Model;
import java.time.Duration;
import java.time.Instant;

final class OtpPages {
    private OtpPages() {}
    static void populate(Model model, OtpService.View view, String title, String description,
                         String action, String resend, String back, String submit) {
        model.addAttribute("otpView", view);
        model.addAttribute("otpTitle", title);
        model.addAttribute("otpDescription", description);
        model.addAttribute("otpAction", action);
        model.addAttribute("otpResend", resend);
        model.addAttribute("otpBack", back);
        model.addAttribute("otpSubmit", submit);
        model.addAttribute("secondsLeft", Math.max(0, Duration.between(Instant.now(), view.expiresAt()).getSeconds()));
        model.addAttribute("resendSeconds", Math.max(0, Duration.between(Instant.now(), view.resendAfter()).getSeconds()));
    }
}
