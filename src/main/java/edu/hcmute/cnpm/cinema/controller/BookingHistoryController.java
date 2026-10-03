package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.dto.booking.BookingDetailView;
import edu.hcmute.cnpm.cinema.entity.BookingOrderStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.service.BookingHistoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BookingHistoryController {
    private final BookingHistoryService history;

    public BookingHistoryController(BookingHistoryService history) { this.history = history; }

    @GetMapping("/lich-su-dat-ve")
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) BookingOrderStatus status,
                       HttpSession session, Model model) {
        User user = SessionUsers.current(session);
        if (user == null) { return SessionUsers.redirectToLogin("/lich-su-dat-ve"); }
        model.addAttribute("history", history.findHistory(user.getId(), page, status));
        model.addAttribute("selectedStatus", status);
        return "account/booking-history";
    }

    @GetMapping("/lich-su-dat-ve/{receiptCode}")
    public String detail(@PathVariable String receiptCode, HttpSession session, Model model) {
        User user = SessionUsers.current(session);
        if (user == null) { return SessionUsers.redirectToLogin("/lich-su-dat-ve/" + receiptCode); }
        BookingDetailView receipt = history.findDetail(receiptCode, user.getId());
        model.addAttribute("receipt", receipt);
        model.addAttribute("order", receipt.order());
        model.addAttribute("bookingStatusLabel", BookingHistoryService.statusLabel(receipt.order().getStatus()));
        model.addAttribute("refundQuotes", history.cancellationQuotes(user.getId()));
        return "account/booking-detail";
    }

    @GetMapping(value = "/lich-su-dat-ve/{receiptCode}/ve/{ticketId}/qr.svg", produces = "image/svg+xml")
    public org.springframework.http.ResponseEntity<String> downloadQr(@PathVariable String receiptCode,
            @PathVariable Long ticketId, HttpSession session) {
        User user = SessionUsers.current(session);
        if (user == null) return org.springframework.http.ResponseEntity.status(401).build();
        String svg = history.findTicketQr(receiptCode, ticketId, user.getId());
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"ute-cinema-ve-" + ticketId + ".svg\"")
                .header("Cache-Control", "no-store, private")
                .header("X-Content-Type-Options", "nosniff").body(svg);
    }
}
