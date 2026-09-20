package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Chặn trang quản trị cho đến khi Module 3 cung cấp phiên đăng nhập ADMIN. */
@Component
public class AdminAccessInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Object currentUser = request.getSession(false) == null ? null
                : request.getSession(false).getAttribute(Constants.SESSION_USER);
        if (currentUser instanceof User user && user.getRole() == Role.ADMIN) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn cần đăng nhập bằng tài khoản quản trị.");
        return false;
    }
}
