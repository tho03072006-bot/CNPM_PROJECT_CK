package edu.hcmute.cnpm.cinema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Lối vào khu quản trị. Gõ {@code /admin} thì vào thẳng trang thống kê - trang tổng quan
 * nhất - thay vì báo lỗi 404 như trước.
 */
@Controller
public class AdminHomeController {

    @GetMapping("/admin")
    public String home() {
        return "redirect:/admin/thong-ke";
    }
}
