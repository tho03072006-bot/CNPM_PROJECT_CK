package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.staff.TicketCheckResult;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.TicketLookupService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /**
     * Nhân viên bấm "Cho vào": ghi giờ vào phòng rồi quay lại đúng kết quả tra lúc nãy,
     * để thấy ngay vé đã chuyển sang "đã vào phòng".
     */
    @PostMapping("/soat-ve/{ticketId}/vao-phong")
    public String checkIn(@PathVariable Long ticketId,
                          @RequestParam(name = "email", required = false) String email,
                          RedirectAttributes redirectAttributes) {
        try {
            TicketCheckResult result = ticketLookupService.checkIn(ticketId);
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                    "Đã cho vé #" + ticketId + " vào phòng. " + result.getMessage());
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
        }
        if (email != null && !email.isBlank()) {
            redirectAttributes.addAttribute("email", email);
        } else {
            redirectAttributes.addAttribute("ma", ticketId);
        }
        return "redirect:/nhan-vien/soat-ve";
    }
}
