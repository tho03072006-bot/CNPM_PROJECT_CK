package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Chặn khu vực nhân viên {@code /nhan-vien/**}: chỉ tài khoản nhân viên và quản trị vào được.
 *
 * Quản trị viên cũng được vào vì ở rạp nhỏ quản lý thường đứng soát vé thay khi đông khách.
 * Ngược lại nhân viên KHÔNG vào được {@code /admin/**} - xem {@link AdminAccessInterceptor}.
 */
@Component
public class StaffAccessInterceptor implements HandlerInterceptor {
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
        if (user.getRole() == Role.STAFF || user.getRole() == Role.ADMIN) {
            return true;
        }
        // Đã đăng nhập nhưng không đủ quyền: báo 403, trang error.html giải thích bằng tiếng Việt.
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn cần đăng nhập bằng tài khoản nhân viên hoặc quản trị.");
        return false;
    }
}
