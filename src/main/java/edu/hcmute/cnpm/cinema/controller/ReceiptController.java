package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.service.BookingOrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ReceiptController {

    private final BookingOrderService bookingOrderService;

    public ReceiptController(BookingOrderService bookingOrderService) {
        this.bookingOrderService = bookingOrderService;
    }

    @GetMapping("/hoa-don/{receiptCode}")
    public String show(@PathVariable String receiptCode, HttpSession session, Model model) {
        User viewer = SessionUsers.current(session);
        if (viewer == null) {
            return SessionUsers.redirectToLogin("/hoa-don/" + receiptCode);
        }
        model.addAttribute("order", bookingOrderService.findReceiptForUser(receiptCode, viewer));
        return "account/receipt";
    }
}
