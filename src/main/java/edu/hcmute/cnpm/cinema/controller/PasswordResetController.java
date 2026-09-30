package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.form.ResetPasswordForm;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.PasswordResetService;
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

/** Quên mật khẩu: xin link qua email, rồi đặt mật khẩu mới bằng link đó. */
@Controller
public class PasswordResetController {

    /** Cùng một câu cho email có và không có tài khoản - không cho người lạ dò email. */
    static final String REQUEST_ACCEPTED_MESSAGE = "Nếu email này đã đăng ký tài khoản, chúng tôi vừa gửi link "
            + "đặt lại mật khẩu tới đó. Link dùng được trong " + PasswordResetService.RESET_LINK_MINUTES
            + " phút. Không thấy thư thì bạn xem thêm trong mục Thư rác.";

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/quen-mat-khau")
    public String showRequestForm() {
        return "account/forgot-password";
    }

    @PostMapping("/quen-mat-khau")
    public String requestReset(@RequestParam(name = "email", required = false) String email,
                               RedirectAttributes redirectAttributes) {
        if (email == null || email.isBlank()) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, "Bạn hãy nhập email đã dùng để đăng ký.");
            return "redirect:/quen-mat-khau";
        }
        passwordResetService.requestReset(email);
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, REQUEST_ACCEPTED_MESSAGE);
        return "redirect:/quen-mat-khau";
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
        return "redirect:/dang-nhap";
    }
}
