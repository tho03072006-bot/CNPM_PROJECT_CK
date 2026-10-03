package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.TicketLookupService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/nhan-vien")
public class StaffController {
    private final TicketLookupService tickets;
    private final StaffCheckInSecurity security;
    public StaffController(TicketLookupService tickets, StaffCheckInSecurity security) {
        this.tickets = tickets; this.security = security;
    }
    @GetMapping
    public String home() { return "redirect:/nhan-vien/soat-ve"; }

    @GetMapping("/soat-ve")
    public String checkTicket(@RequestParam(name="ma", required=false) String code,
                              @RequestParam(name="qr", required=false) String qr,
                              @RequestParam(name="email", required=false) String email,
                              HttpSession session, Model model) {
        model.addAttribute("checkInCsrf", security.token(session));
        model.addAttribute("ticketCode", code);
        model.addAttribute("email", email);
        try {
            if (qr != null) {
                var result = tickets.checkBookingQr(qr);
                model.addAttribute("bookingResult", result);
                model.addAttribute("bookingQr", qr);
            } else if (code != null) {
                model.addAttribute("result", tickets.checkTicketCode(code));
            } else if (email != null) {
                model.addAttribute("emailResults", tickets.findUpcomingTicketsByEmail(email));
            }
        } catch (BusinessException e) {
            model.addAttribute(Constants.MODEL_ERROR_MESSAGE,
                    code != null || qr != null ? "Vé không hợp lệ. " + e.getMessage() : e.getMessage());
        }
        return "staff/ticket-check";
    }
    @PostMapping("/soat-ve/{ticketId}/vao-phong")
    public String checkIn(@PathVariable Long ticketId,
                          @RequestParam(name="ma", required=false) String code,
                          @RequestParam(name="email", required=false) String email,
                          @RequestParam(name="checkInCsrf", required=false) String csrf,
                          HttpSession session, RedirectAttributes redirect) {
        try {
            security.verify(session, csrf);
            var result = tickets.checkIn(ticketId, code);
            redirect.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                    "Đã cho vé " + result.getTicketCode() + " vào phòng. " + result.getMessage());
        } catch (BusinessException e) {
            redirect.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage());
        }
        if (email != null && !email.isBlank()) redirect.addAttribute("email", email);
        else if (code != null) redirect.addAttribute("ma", code);
        return "redirect:/nhan-vien/soat-ve";
    }
    @PostMapping("/soat-ve/hoa-don/{receiptCode}/vao-phong")
    public String checkInBooking(@PathVariable String receiptCode,
                                @RequestParam(name="qr", required=false) String qr,
                                @RequestParam(name="checkInCsrf", required=false) String csrf,
                                HttpSession session, RedirectAttributes redirect) {
        try {
            security.verify(session, csrf);
            int count = tickets.checkInBooking(receiptCode, qr);
            redirect.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                    "Đã cho " + count + " vé vào phòng. Các vé đã được đánh dấu sử dụng.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage());
        }
        if (qr != null) redirect.addAttribute("qr", qr);
        return "redirect:/nhan-vien/soat-ve";
    }
}
