package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.UserManagementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/** Quản trị tài khoản người dùng. Quyền truy cập do {@link AdminAccessInterceptor} lo. */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserManagementService userManagementService;

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String keyword,
                       HttpSession session, Model model) {
        User currentAdmin = SessionUsers.current(session);
        model.addAttribute("users", userManagementService.searchUsers(keyword));
        Map<Role, Long> roleCounts = userManagementService.countUsersByRole();
        model.addAttribute("customerCount", roleCounts.get(Role.CUSTOMER));
        model.addAttribute("staffCount", roleCounts.get(Role.STAFF));
        model.addAttribute("adminCount", roleCounts.get(Role.ADMIN));
        model.addAttribute("roles", Role.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentUserId", currentAdmin == null ? null : currentAdmin.getId());
        return "admin/user-list";
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam("role") Role role,
                             @RequestParam(name = "q", required = false) String keyword,
                             HttpSession session, RedirectAttributes redirectAttributes) {
        User currentAdmin = SessionUsers.current(session);
        try {
            User updated = userManagementService.changeRole(
                    currentAdmin == null ? null : currentAdmin.getId(), id, role);
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                    "Đã đổi vai trò của " + updated.getEmail() + ". Người này cần đăng nhập lại để quyền mới có hiệu lực.");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
        }
        if (keyword != null && !keyword.isBlank()) {
            redirectAttributes.addAttribute("q", keyword);
        }
        return "redirect:/admin/users";
    }
}
