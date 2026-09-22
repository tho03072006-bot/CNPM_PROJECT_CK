package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.dto.stats.RevenueRow;
import edu.hcmute.cnpm.cinema.service.StatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Trang thống kê doanh thu cho quản trị viên.
 *
 * Nằm dưới {@code /admin/**} nên đã được {@code AdminAccessInterceptor} chặn sẵn:
 * khách hàng thường gọi thẳng địa chỉ này sẽ nhận 403.
 */
@Controller
@RequestMapping("/admin/thong-ke")
public class AdminStatsController {

    /** Giới hạn số ngày báo cáo, tránh ai đó gõ ?ngay=100000 làm nặng máy. */
    private static final int MAX_DAYS = 90;
    private static final int TOP_MOVIE_LIMIT = 5;

    private final StatsService statsService;

    public AdminStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public String showStats(@RequestParam(name = "ngay", required = false) Integer requestedDays,
                            Model model) {
        int days = normalizeDays(requestedDays);
        List<RevenueRow> revenueByDay = statsService.findRevenueByDay(days);

        model.addAttribute("days", days);
        model.addAttribute("revenueByDay", revenueByDay);
        model.addAttribute("topMovies", statsService.findTopMovies(days, TOP_MOVIE_LIMIT));
        model.addAttribute("totalRevenue", statsService.sumRevenue(revenueByDay));
        model.addAttribute("totalTickets", statsService.sumTickets(revenueByDay));
        return "admin/stats";
    }

    private int normalizeDays(Integer requestedDays) {
        if (requestedDays == null || requestedDays < 1) {
            return StatsService.DEFAULT_DAYS;
        }
        return Math.min(requestedDays, MAX_DAYS);
    }
}
