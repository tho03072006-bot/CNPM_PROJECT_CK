package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import jakarta.servlet.http.HttpSession;

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
    public static String redirectToLogin(String nextPath) {
        if (nextPath == null || nextPath.isBlank()) {
            return "redirect:/dang-nhap";
        }
        return "redirect:/dang-nhap?next=" + URLEncoder.encode(nextPath, StandardCharsets.UTF_8);
    }
}
