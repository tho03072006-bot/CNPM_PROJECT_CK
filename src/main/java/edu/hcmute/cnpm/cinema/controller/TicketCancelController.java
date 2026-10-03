package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.util.MoneyFormatter;
import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.TicketRefund;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.TicketMailService;
import edu.hcmute.cnpm.cinema.service.TicketRefundService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

/**
 * Khách tự huỷ vé đã thanh toán. Hai bước: xem trước được hoàn bao nhiêu, rồi mới bấm
 * xác nhận - để không ai lỡ tay huỷ vé mà chưa biết mình chỉ được hoàn một nửa.
 */
@Controller
@RequestMapping("/ve-cua-toi/{ticketId}/huy")
public class TicketCancelController {

    private final TicketRefundService ticketRefundService;
    private final TicketMailService ticketMailService;

    public TicketCancelController(TicketRefundService ticketRefundService, TicketMailService ticketMailService) {
        this.ticketRefundService = ticketRefundService;
        this.ticketMailService = ticketMailService;
    }

    @GetMapping
    public String showConfirmPage(@PathVariable Long ticketId, HttpSession session, Model model,
                                  RedirectAttributes redirectAttributes) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/ve-cua-toi");
        }
        try {
            model.addAttribute("quote", ticketRefundService.quote(customer.getId(), ticketId, LocalDateTime.now()));
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "redirect:/ve-cua-toi";
        }
        model.addAttribute("fullRefundHours", TicketRefundService.FULL_REFUND_HOURS);
        model.addAttribute("minCancelHours", TicketRefundService.MIN_CANCEL_HOURS);
        model.addAttribute("partialRefundPercent", TicketRefundService.PARTIAL_REFUND_PERCENT);
        return "account/cancel-ticket";
    }

    @PostMapping
    public String cancel(@PathVariable Long ticketId, HttpSession session, RedirectAttributes redirectAttributes) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/ve-cua-toi");
        }
        try {
            TicketRefund refund = ticketRefundService.cancelPaidTicket(customer.getId(), ticketId, LocalDateTime.now());
            ticketMailService.sendRefundConfirmation(customer, refund);
            String amount = MoneyFormatter.format(refund.getRefundAmount());
            String where = refund.getRefundRef() != null ? "về ví MoMo" : "tại quầy vé khi bạn tới rạp";
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                    "Đã huỷ vé #" + ticketId + ". Bạn được hoàn " + amount + " (" + refund.getRefundPercent()
                            + "%) " + where + ". Ghế đã được trả lại cho người khác đặt.");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
        }
        return "redirect:/ve-cua-toi";
    }
}
