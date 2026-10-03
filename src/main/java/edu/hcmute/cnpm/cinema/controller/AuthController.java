package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.form.LoginForm;
import edu.hcmute.cnpm.cinema.controller.form.RegisterForm;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.AuthService;
import edu.hcmute.cnpm.cinema.service.RegistrationOtpService;
import edu.hcmute.cnpm.cinema.service.OtpService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Đăng ký, đăng nhập, đăng xuất.
 *
 * Phiên đăng nhập lưu trong session dưới tên {@link Constants#SESSION_USER} -
 * đúng tên mà Module 2 đọc khi giữ ghế, nên hai bên khớp nhau.
 */
@Controller
public class AuthController {

    private final AuthService authService;
    private final RegistrationOtpService registrationOtp;
    private final OtpService otp;
    static final String REGISTRATION_OTP = "registrationOtp";

    public AuthController(AuthService authService, RegistrationOtpService registrationOtp, OtpService otp) {
        this.authService = authService;
        this.registrationOtp = registrationOtp;
        this.otp = otp;
    }

    // ==================== Đăng ký ====================

    @GetMapping("/dang-ky")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "account/register";
    }

    @PostMapping("/dang-ky")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult bindingResult, HttpSession session,
                           RedirectAttributes redirectAttributes) {
        if (!form.isPasswordConfirmed()) {
            bindingResult.rejectValue("confirmPassword", "khongKhop",
                    "Hai lần nhập mật khẩu chưa giống nhau.");
        }
        if (bindingResult.hasErrors()) {
            return "account/register";
        }

        try {
            String id = registrationOtp.begin(form);
            otp.cancel((String) session.getAttribute(REGISTRATION_OTP));
            session.setAttribute(REGISTRATION_OTP, id);
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                    "Đã gửi mã xác thực tới email của bạn. Nhập mã để hoàn tất đăng ký.");
            return "redirect:/dang-ky/xac-thuc";
        } catch (BusinessException exception) {
            // Bắt ở đây thay vì để GlobalExceptionHandler đổi thành trang lỗi 400:
            // với form thì hiện lại đúng form kèm lời nhắc dễ sửa hơn nhiều.
            bindingResult.reject("dangKyThatBai", exception.getMessage());
            return "account/register";
        }
    }

    // ==================== Đăng nhập ====================

    @GetMapping("/dang-ky/xac-thuc")
    public String showRegistrationOtp(HttpSession session, Model model, RedirectAttributes flash) {
        try {
            OtpPages.populate(model, otp.describe((String) session.getAttribute(REGISTRATION_OTP), OtpService.Purpose.REGISTER),
                    "Xác thực đăng ký", "Xác nhận email để tạo tài khoản UTE Cinema.",
                    "/dang-ky/xac-thuc", "/dang-ky/gui-lai", "/dang-ky", "Hoàn tất đăng ký");
            return "account/verify-otp";
        } catch (BusinessException e) {
            flash.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage());
            return "redirect:/dang-ky";
        }
    }

    @PostMapping("/dang-ky/xac-thuc")
    public String verifyRegistration(@RequestParam(required = false) String code, HttpSession session, RedirectAttributes flash) {
        try {
            registrationOtp.verify((String) session.getAttribute(REGISTRATION_OTP), code);
            session.removeAttribute(REGISTRATION_OTP);
            flash.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Tạo tài khoản thành công. Bạn hãy đăng nhập để đặt vé.");
            return "redirect:/dang-nhap";
        } catch (BusinessException e) {
            flash.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage());
            return "redirect:/dang-ky/xac-thuc";
        }
    }

    @PostMapping("/dang-ky/gui-lai")
    public String resendRegistration(HttpSession session, RedirectAttributes flash) {
        try {
            otp.resend((String) session.getAttribute(REGISTRATION_OTP), OtpService.Purpose.REGISTER);
            flash.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã gửi mã mới. Mã cũ không còn hiệu lực.");
        } catch (BusinessException e) { flash.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage()); }
        return "redirect:/dang-ky/xac-thuc";
    }

    @GetMapping("/dang-nhap")
    public String showLoginForm(@RequestParam(name = "next", required = false) String next,
                                Model model) {
        LoginForm form = new LoginForm();
        form.setNext(next);
        model.addAttribute("loginForm", form);
        return "account/login";
    }

    @PostMapping("/dang-nhap")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm form,
                        BindingResult bindingResult, HttpSession session,
                        jakarta.servlet.http.HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "account/login";
        }

        try {
            User user = authService.login(form.getEmail(), form.getPassword());
            request.changeSessionId();
            session.setAttribute(Constants.SESSION_USER, user);
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                    "Xin chào " + user.getFullName() + "!");
            return "redirect:" + safeNextPath(form.getNext());
        } catch (BusinessException exception) {
            bindingResult.reject("dangNhapThatBai", exception.getMessage());
            return "account/login";
        }
    }

    // ==================== Đăng xuất ====================

    @GetMapping("/dang-xuat")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Bạn đã đăng xuất.");
        return "redirect:/";
    }

    /**
     * Lọc đường dẫn quay lại sau khi đăng nhập.
     *
     * Chỉ nhận đường dẫn nội bộ bắt đầu bằng một dấu "/". Nếu nhận bừa thì kẻ xấu
     * gửi link dạng {@code /dang-nhap?next=https://trang-gia-mao} là đăng nhập xong
     * khách bị đá sang trang của họ mà vẫn tưởng đang ở trang mình.
     */
    private String safeNextPath(String next) {
        if (next == null || next.isBlank()) {
            return "/";
        }
        String trimmed = next.trim();
        boolean internalPath = trimmed.startsWith("/") && !trimmed.startsWith("//")
                && !trimmed.contains("\\") && !trimmed.contains("\r") && !trimmed.contains("\n")
                && !trimmed.toLowerCase(java.util.Locale.ROOT).matches(".*%(?:2f|5c|0a|0d).*" );
        return internalPath ? trimmed : "/";
    }
}
