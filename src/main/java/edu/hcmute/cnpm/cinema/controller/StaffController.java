package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.TicketLookupService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Khu vực nhân viên: soát vé ở cửa phòng chiếu.
 *
 * Quyền truy cập do {@link StaffAccessInterceptor} lo, Controller này không tự kiểm tra lại.
 */
@Controller
@RequestMapping("/nhan-vien")
public class StaffController {

    private final TicketLookupService ticketLookupService;

    public StaffController(TicketLookupService ticketLookupService) {
        this.ticketLookupService = ticketLookupService;
    }

    @GetMapping
    public String home() {
        return "redirect:/nhan-vien/soat-ve";
    }

    /**
     * Một trang, hai cách tra: theo mã vé ({@code ?ma=12}) hoặc theo email khách
     * ({@code ?email=...}). Dùng GET để nhân viên bấm F5 là soát lại được ngay.
     */
    @GetMapping("/soat-ve")
    public String checkTicket(@RequestParam(name = "ma", required = false) String ticketCode,
                              @RequestParam(name = "email", required = false) String email,
                              Model model) {
        model.addAttribute("ticketCode", ticketCode);
        model.addAttribute("email", email);

        try {
            if (ticketCode != null) {
                model.addAttribute("result", ticketLookupService.checkTicketCode(ticketCode));
            } else if (email != null) {
                model.addAttribute("emailResults", ticketLookupService.findUpcomingTicketsByEmail(email));
            }
        } catch (BusinessException exception) {
            // Tra sai mã hay sai email là chuyện thường ở quầy, hiện ngay trên trang
            // thay vì đá sang trang lỗi 400.
            model.addAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
        }
        return "staff/ticket-check";
    }
}
