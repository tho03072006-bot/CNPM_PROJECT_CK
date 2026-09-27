package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.form.ChangePasswordForm;
import edu.hcmute.cnpm.cinema.controller.form.ProfileForm;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Sửa hồ sơ và đổi mật khẩu của người đang đăng nhập.
 *
 * Tách khỏi {@link AccountController} vì bên đó chỉ đọc (trang hồ sơ, vé của tôi),
 * còn ở đây là các form có ghi dữ liệu.
 */
@Controller
@RequestMapping("/tai-khoan")
public class AccountSettingsController {

    private static final String VIEW_PROFILE_EDIT = "account/profile-edit";
    private static final String VIEW_CHANGE_PASSWORD = "account/change-password";

    private final AuthService authService;

    public AccountSettingsController(AuthService authService) {
        this.authService = authService;
    }

    // ==================== Sửa hồ sơ ====================

    @GetMapping("/sua")
    public String showProfileForm(HttpSession session, Model model) {
        User sessionUser = SessionUsers.current(session);
        if (sessionUser == null) {
            return SessionUsers.redirectToLogin("/tai-khoan/sua");
        }
        User user = authService.findById(sessionUser.getId());
        model.addAttribute("profileForm", ProfileForm.from(user));
        model.addAttribute("email", user.getEmail());
        return VIEW_PROFILE_EDIT;
    }

    @PostMapping("/sua")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult bindingResult, HttpSession session, Model model,
                                RedirectAttributes redirectAttributes) {
        User sessionUser = SessionUsers.current(session);
        if (sessionUser == null) {
            return SessionUsers.redirectToLogin("/tai-khoan/sua");
        }
        model.addAttribute("email", sessionUser.getEmail());
        if (bindingResult.hasErrors()) {
            return VIEW_PROFILE_EDIT;
        }

        try {
            User updated = authService.updateProfile(sessionUser.getId(), form.getFullName(), form.getPhone());
            // Cập nhật luôn bản trong session, nếu không thanh điều hướng vẫn hiện tên cũ
            // cho tới lần đăng nhập sau.
            session.setAttribute(Constants.SESSION_USER, updated);
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã lưu hồ sơ của bạn.");
            return "redirect:/tai-khoan";
        } catch (BusinessException exception) {
            bindingResult.reject("suaHoSoThatBai", exception.getMessage());
            return VIEW_PROFILE_EDIT;
        }
    }

    // ==================== Đổi mật khẩu ====================

    @GetMapping("/doi-mat-khau")
    public String showChangePasswordForm(HttpSession session, Model model) {
        if (SessionUsers.current(session) == null) {
            return SessionUsers.redirectToLogin("/tai-khoan/doi-mat-khau");
        }
        model.addAttribute("changePasswordForm", new ChangePasswordForm());
        return VIEW_CHANGE_PASSWORD;
    }

    @PostMapping("/doi-mat-khau")
    public String changePassword(@Valid @ModelAttribute("changePasswordForm") ChangePasswordForm form,
                                 BindingResult bindingResult, HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User sessionUser = SessionUsers.current(session);
        if (sessionUser == null) {
            return SessionUsers.redirectToLogin("/tai-khoan/doi-mat-khau");
        }
        if (!form.isNewPasswordConfirmed()) {
            bindingResult.rejectValue("confirmPassword", "khongKhop",
                    "Hai lần nhập mật khẩu mới chưa giống nhau.");
        }
        if (bindingResult.hasErrors()) {
            return showFormAgainWithoutPasswords(form);
        }

        try {
            authService.changePassword(sessionUser.getId(), form.getCurrentPassword(), form.getNewPassword());
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                    "Đã đổi mật khẩu. Lần đăng nhập sau hãy dùng mật khẩu mới.");
            return "redirect:/tai-khoan";
        } catch (BusinessException exception) {
            bindingResult.reject("doiMatKhauThatBai", exception.getMessage());
            return showFormAgainWithoutPasswords(form);
        }
    }

    /** Không bao giờ gửi mật khẩu người dùng vừa gõ ngược về trình duyệt trong HTML. */
    private String showFormAgainWithoutPasswords(ChangePasswordForm form) {
        form.setCurrentPassword(null);
        form.setNewPassword(null);
        form.setConfirmPassword(null);
        return VIEW_CHANGE_PASSWORD;
    }
}
