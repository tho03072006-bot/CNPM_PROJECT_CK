package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.dto.receipt.ReceiptView;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.service.ReceiptService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/hoa-don/{receiptCode}")
    public String show(@PathVariable String receiptCode, HttpSession session, Model model) {
        User viewer = SessionUsers.current(session);
        if (viewer == null) {
            return SessionUsers.redirectToLogin("/hoa-don/" + receiptCode);
        }
        ReceiptView receipt = receiptService.findReceipt(receiptCode, viewer);
        model.addAttribute("receipt", receipt);
        model.addAttribute("order", receipt.order());
        return "account/receipt";
    }
}
