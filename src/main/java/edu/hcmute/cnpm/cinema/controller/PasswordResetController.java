package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.form.ResetPasswordForm;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.PasswordResetService;
import edu.hcmute.cnpm.cinema.service.RecoveryOtpService;
import edu.hcmute.cnpm.cinema.service.OtpService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.Instant;

/** Khôi phục tài khoản: xác minh OTP email rồi cấp token đặt mật khẩu mới. */
@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final RecoveryOtpService recovery;
    private final OtpService otp;
    private static final String RESET_OTP = "resetOtp";

    public PasswordResetController(PasswordResetService passwordResetService, RecoveryOtpService recovery, OtpService otp) {
        this.passwordResetService = passwordResetService;
        this.recovery = recovery;
        this.otp = otp;
    }

    @GetMapping({"/quen-mat-khau", "/khoi-phuc-tai-khoan"})
    public String showRequestForm() {
        return "account/forgot-password";
    }

    @PostMapping("/quen-mat-khau")
    public String requestReset(@RequestParam(name = "email", required = false) String email,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (email == null || email.isBlank()) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, "Bạn hãy nhập email đã dùng để đăng ký.");
            return "redirect:/quen-mat-khau";
        }
        try {
            String id = recovery.begin(email);
            otp.cancel((String) session.getAttribute(RESET_OTP));
            session.setAttribute(RESET_OTP, id);
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                    "Nếu email đã đăng ký, bạn sẽ nhận mã OTP khôi phục có hiệu lực 5 phút. Hãy kiểm tra cả Thư rác.");
            return "redirect:/quen-mat-khau/xac-thuc";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage());
            return "redirect:/quen-mat-khau";
        }
    }

    @GetMapping("/quen-mat-khau/xac-thuc")
    public String showOtp(HttpSession session, Model model, RedirectAttributes flash) {
        try {
            OtpPages.populate(model, otp.describe((String) session.getAttribute(RESET_OTP), OtpService.Purpose.RESET),
                    "Khôi phục tài khoản", "Nhập mã trong email để tiếp tục đặt mật khẩu mới.",
                    "/quen-mat-khau/xac-thuc", "/quen-mat-khau/gui-lai", "/quen-mat-khau", "Xác thực và tiếp tục");
            return "account/verify-otp";
        } catch (BusinessException e) {
            flash.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage());
            return "redirect:/quen-mat-khau";
        }
    }

    @PostMapping("/quen-mat-khau/xac-thuc")
    public String verifyOtp(@RequestParam(required = false) String code, HttpSession session, RedirectAttributes flash) {
        try {
            String token = recovery.verify((String) session.getAttribute(RESET_OTP), code);
            session.removeAttribute(RESET_OTP);
            return "redirect:/dat-lai-mat-khau?token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            flash.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage());
            return "redirect:/quen-mat-khau/xac-thuc";
        }
    }

    @PostMapping("/quen-mat-khau/gui-lai")
    public String resendOtp(HttpSession session, RedirectAttributes flash) {
        try {
            otp.resend((String) session.getAttribute(RESET_OTP), OtpService.Purpose.RESET);
            flash.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Nếu email đã đăng ký, mã mới sẽ được gửi tới hộp thư.");
        } catch (BusinessException e) { flash.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage()); }
        return "redirect:/quen-mat-khau/xac-thuc";
    }

    @GetMapping("/dat-lai-mat-khau")
    public String showResetForm(@RequestParam(name = "token", required = false) String token,
                                Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("email", passwordResetService.findUserByToken(token, Instant.now()).getEmail());
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "redirect:/quen-mat-khau";
        }
        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("resetPasswordForm", form);
        return "account/reset-password";
    }

    @PostMapping("/dat-lai-mat-khau")
    public String resetPassword(@Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
                                BindingResult bindingResult, Model model,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!form.isNewPasswordConfirmed()) {
            bindingResult.rejectValue("confirmPassword", "khongKhop", "Hai lần nhập mật khẩu mới chưa giống nhau.");
        }
        try {
            // Kiểm tra lại mã trước cả lỗi nhập liệu: mã hỏng thì hiện form cũng vô ích.
            model.addAttribute("email", passwordResetService.findUserByToken(form.getToken(), Instant.now()).getEmail());
            if (bindingResult.hasErrors()) {
                return "account/reset-password";
            }
            passwordResetService.resetPassword(form.getToken(), form.getNewPassword(), Instant.now());
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "redirect:/quen-mat-khau";
        }
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                "Đã đặt mật khẩu mới. Bạn đăng nhập bằng mật khẩu mới nhé.");
        session.invalidate();
        return "redirect:/dang-nhap";
    }
}
