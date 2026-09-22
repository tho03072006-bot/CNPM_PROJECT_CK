package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import edu.hcmute.cnpm.cinema.service.PaymentService;
import edu.hcmute.cnpm.cinema.service.TicketMailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Trang xác nhận thanh toán cho các ghế khách vừa giữ. */
@Controller
@RequestMapping("/thanh-toan")
public class PaymentController {

    private final PaymentService paymentService;
    private final TicketMailService ticketMailService;
    private final TicketRepository ticketRepository;

    public PaymentController(PaymentService paymentService, TicketMailService ticketMailService,
                             TicketRepository ticketRepository) {
        this.paymentService = paymentService;
        this.ticketMailService = ticketMailService;
        this.ticketRepository = ticketRepository;
    }

    @GetMapping("/{showtimeId}")
    public String showPaymentPage(@PathVariable Long showtimeId, HttpSession session, Model model) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/thanh-toan/" + showtimeId);
        }

        List<Ticket> tickets = paymentService.findPayableTickets(customer.getId(), showtimeId);
        model.addAttribute("tickets", tickets);
        model.addAttribute("total", paymentService.sumPrice(tickets));
        model.addAttribute("showtimeId", showtimeId);
        return "account/payment";
    }

    @PostMapping("/{showtimeId}")
    public String confirmPayment(@PathVariable Long showtimeId, HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/thanh-toan/" + showtimeId);
        }

        try {
            List<Ticket> paid = paymentService.confirmPayment(customer.getId(), showtimeId);
            BigDecimal total = paymentService.sumPrice(paid);

            // Gửi thư sau khi đã ghi nhận thanh toán. Hàm này không bao giờ ném lỗi
            // nên server mail chập chờn cũng không làm khách mất vé.
            ticketMailService.sendTicketConfirmation(customer, paid, total);

            List<Long> ticketIds = new ArrayList<>();
            for (Ticket ticket : paid) {
                ticketIds.add(ticket.getId());
            }
            redirectAttributes.addFlashAttribute("paidTicketIds", ticketIds);
            return "redirect:/thanh-toan/hoan-tat";
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "redirect:/thanh-toan/" + showtimeId;
        }
    }

    /**
     * Trang báo đặt vé thành công.
     *
     * Mã vé truyền qua flash attribute nên chỉ hiện đúng một lần ngay sau khi trả
     * tiền. Khách tải lại trang thì không còn dữ liệu, lúc đó chuyển sang trang
     * vé của tôi - vé vẫn ở đó, không mất đi đâu.
     */
    @GetMapping("/hoan-tat")
    public String showPaymentResult(HttpSession session, Model model) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/ve-cua-toi");
        }

        @SuppressWarnings("unchecked")
        List<Long> ticketIds = (List<Long>) model.getAttribute("paidTicketIds");
        if (ticketIds == null || ticketIds.isEmpty()) {
            return "redirect:/ve-cua-toi";
        }

        List<Ticket> tickets = new ArrayList<>();
        for (Ticket ticket : ticketRepository.findAllById(ticketIds)) {
            // Chốt chặn: chỉ hiện vé của chính người đang đăng nhập.
            if (ticket.getUser() != null && ticket.getUser().getId().equals(customer.getId())) {
                tickets.add(ticket);
            }
        }
        model.addAttribute("tickets", tickets);
        model.addAttribute("total", paymentService.sumPrice(tickets));
        return "account/payment-success";
    }
}
