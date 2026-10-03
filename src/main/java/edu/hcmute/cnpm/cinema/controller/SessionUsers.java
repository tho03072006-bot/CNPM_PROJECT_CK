package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * Đọc người dùng đang đăng nhập ra khỏi session.
 *
 * Gom vào một chỗ để mọi Controller đọc session theo cùng một cách, tránh
 * chuyện mỗi người tự gõ lại tên thuộc tính rồi gõ sai.
 */
public final class SessionUsers {

    private SessionUsers() {}

    /** Người đang đăng nhập, hoặc null nếu chưa đăng nhập. */
    public static User current(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(Constants.SESSION_USER);
        return value instanceof User user ? user : null;
    }

    /** Người đang đăng nhập có phải quản trị viên không. */
    public static boolean isAdmin(HttpSession session) {
        User user = current(session);
        return user != null && user.getRole() == Role.ADMIN;
    }

    /**
     * Đường dẫn chuyển tới trang đăng nhập, có nhớ trang khách đang muốn vào.
     *
     * Đăng nhập xong khách quay lại đúng chỗ cũ thay vì bị đá về trang chủ.
     */
    /**
     * Dùng trong bộ chặn quyền (interceptor), nơi không trả về được chuỗi "redirect:".
     * Chỉ nhớ trang cũ với yêu cầu GET: gửi form (POST) sau khi phiên hết hạn thì quay
     * lại địa chỉ POST cũng không dùng được.
     */
    public static void sendToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String loginUrl = request.getContextPath() + "/dang-nhap";
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            String query = request.getQueryString();
            String next = query == null ? path : path + "?" + query;
            loginUrl += "?next=" + URLEncoder.encode(next, StandardCharsets.UTF_8);
        }
        response.sendRedirect(loginUrl);
    }

    public static String redirectToLogin(String nextPath) {
        if (nextPath == null || nextPath.isBlank()) {
            return "redirect:/dang-nhap";
        }
        return "redirect:/dang-nhap?next=" + URLEncoder.encode(nextPath, StandardCharsets.UTF_8);
    }
}
