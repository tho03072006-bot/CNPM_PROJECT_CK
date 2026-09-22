package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.service.AuthService;
import edu.hcmute.cnpm.cinema.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

/** Trang tài khoản cá nhân và lịch sử vé đã đặt. */
@Controller
public class AccountController {

    private final AuthService authService;
    private final PaymentService paymentService;

    public AccountController(AuthService authService, PaymentService paymentService) {
        this.authService = authService;
        this.paymentService = paymentService;
    }

    @GetMapping("/tai-khoan")
    public String showProfile(HttpSession session, Model model) {
        User sessionUser = SessionUsers.current(session);
        if (sessionUser == null) {
            return SessionUsers.redirectToLogin("/tai-khoan");
        }

        // Đọc lại từ database thay vì tin bản lưu trong session: tên hoặc số điện
        // thoại có thể đã đổi từ lúc đăng nhập tới giờ.
        User customer = authService.findById(sessionUser.getId());
        List<Ticket> tickets = paymentService.findTicketHistory(customer.getId());

        model.addAttribute("customer", customer);
        model.addAttribute("tickets", tickets);
        model.addAttribute("paidCount", countByStatus(tickets, TicketStatus.PAID));
        model.addAttribute("totalPaid", sumPaid(tickets));
        return "account/profile";
    }

    @GetMapping("/ve-cua-toi")
    public String showMyTickets(HttpSession session, Model model) {
        User sessionUser = SessionUsers.current(session);
        if (sessionUser == null) {
            return SessionUsers.redirectToLogin("/ve-cua-toi");
        }
        List<Ticket> tickets = paymentService.findTicketHistory(sessionUser.getId());
        model.addAttribute("tickets", tickets);
        model.addAttribute("total", sumPaid(tickets));
        return "account/my-tickets";
    }

    private long countByStatus(List<Ticket> tickets, TicketStatus status) {
        return tickets.stream().filter(ticket -> ticket.getStatus() == status).count();
    }

    /** Chỉ cộng tiền của vé đã thanh toán - vé đang giữ chưa phải là tiền thật. */
    private BigDecimal sumPaid(List<Ticket> tickets) {
        BigDecimal total = BigDecimal.ZERO;
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.PAID && ticket.getPrice() != null) {
                total = total.add(ticket.getPrice());
            }
        }
        return total;
    }
}
