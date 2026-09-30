package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.service.CustomerSupportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Dữ liệu dùng chung cho thanh điều hướng trên mọi trang HTML. */
@ControllerAdvice
public class NavigationModelAdvice {

    private final CustomerSupportService supportService;

    public NavigationModelAdvice(CustomerSupportService supportService) {
        this.supportService = supportService;
    }

    @ModelAttribute
    public void addNavigationState(HttpServletRequest request, HttpSession session, Model model) {
        model.addAttribute("currentPath", request.getRequestURI());

        User currentUser = SessionUsers.current(session);
        boolean employee = currentUser != null
                && (currentUser.getRole() == Role.STAFF || currentUser.getRole() == Role.ADMIN);
        model.addAttribute("pendingSupportCount", employee ? supportService.countWaitingForStaff() : 0L);
    }
}
