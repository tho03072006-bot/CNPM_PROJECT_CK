package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Chặn khu vực quản trị {@code /admin/**}: chỉ tài khoản quản trị vào được.
 * Chưa đăng nhập thì chuyển sang trang đăng nhập, đăng nhập rồi mà không phải quản trị thì báo 403.
 */
@Component
public class AdminAccessInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Object currentUser = request.getSession(false) == null ? null
                : request.getSession(false).getAttribute(Constants.SESSION_USER);
        // Chưa đăng nhập: mời đăng nhập, xong quay lại đúng trang đang muốn vào.
        if (!(currentUser instanceof User user)) {
            SessionUsers.sendToLogin(request, response);
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        // Đã đăng nhập nhưng không đủ quyền: báo 403, trang error.html giải thích bằng tiếng Việt.
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn cần đăng nhập bằng tài khoản quản trị.");
        return false;
    }
}
