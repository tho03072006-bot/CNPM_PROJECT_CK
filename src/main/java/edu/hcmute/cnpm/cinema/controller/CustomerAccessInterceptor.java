package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Chỉ tài khoản khách hàng được dùng hộp thư hỗ trợ tại {@code /ho-tro/**}. */
@Component
public class CustomerAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Object currentUser = request.getSession(false) == null ? null
                : request.getSession(false).getAttribute(Constants.SESSION_USER);
        if (!(currentUser instanceof User user)) {
            SessionUsers.sendToLogin(request, response);
            return false;
        }
        if (user.getRole() == Role.CUSTOMER) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Hộp thư này chỉ dành cho khách hàng. Nhân viên hãy dùng mục CSKH.");
        return false;
    }
}
