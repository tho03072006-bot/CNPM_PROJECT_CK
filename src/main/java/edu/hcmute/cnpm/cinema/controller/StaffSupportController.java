package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.SupportStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.CustomerSupportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/nhan-vien/ho-tro")
public class StaffSupportController {

    private final CustomerSupportService supportService;

    public StaffSupportController(CustomerSupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) SupportStatus status, Model model) {
        model.addAttribute("conversations", supportService.findForStaff(status));
        model.addAttribute("statuses", SupportStatus.values());
        model.addAttribute("selectedStatus", status);
        return "support/staff-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("conversation", supportService.findForStaff(id));
        return "support/staff-conversation";
    }

    @PostMapping("/{id}/tra-loi")
    public String reply(@PathVariable Long id, @RequestParam String content,
                        HttpSession session, RedirectAttributes redirectAttributes) {
        User staff = SessionUsers.current(session);
        try {
            supportService.replyAsStaff(id, staff.getId(), content);
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã gửi phản hồi cho khách hàng.");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
        }
        return "redirect:/nhan-vien/ho-tro/" + id;
    }

    @PostMapping("/{id}/dong")
    public String resolve(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User staff = SessionUsers.current(session);
        supportService.resolve(id, staff.getId());
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã đánh dấu yêu cầu là đã giải quyết.");
        return "redirect:/nhan-vien/ho-tro/" + id;
    }
}
