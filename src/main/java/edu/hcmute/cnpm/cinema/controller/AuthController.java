package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.form.LoginForm;
import edu.hcmute.cnpm.cinema.controller.form.RegisterForm;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
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
            User user = authService.register(form.getFullName(), form.getEmail(),
                    form.getPhone(), form.getPassword());
            session.setAttribute(Constants.SESSION_USER, user);
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                    "Tạo tài khoản thành công. Chào mừng " + user.getFullName() + "!");
            return "redirect:/";
        } catch (BusinessException exception) {
            // Bắt ở đây thay vì để GlobalExceptionHandler đổi thành trang lỗi 400:
            // với form thì hiện lại đúng form kèm lời nhắc dễ sửa hơn nhiều.
            bindingResult.reject("dangKyThatBai", exception.getMessage());
            return "account/register";
        }
    }

    // ==================== Đăng nhập ====================

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
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "account/login";
        }

        try {
            User user = authService.login(form.getEmail(), form.getPassword());
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
        boolean internalPath = trimmed.startsWith("/") && !trimmed.startsWith("//");
        return internalPath ? trimmed : "/";
    }
}
