package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.form.SupportRequestForm;
import edu.hcmute.cnpm.cinema.entity.SupportCategory;
import edu.hcmute.cnpm.cinema.entity.SupportConversation;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.CustomerSupportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ho-tro")
public class CustomerSupportController {

    private final CustomerSupportService supportService;

    public CustomerSupportController(CustomerSupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        User customer = SessionUsers.current(session);
        if (customer == null) return SessionUsers.redirectToLogin("/ho-tro");
        model.addAttribute("conversations", supportService.findForCustomer(customer.getId()));
        return "support/customer-list";
    }

    @GetMapping("/moi")
    public String newRequest(HttpSession session, Model model) {
        if (SessionUsers.current(session) == null) return SessionUsers.redirectToLogin("/ho-tro/moi");
        if (!model.containsAttribute("supportForm")) {
            model.addAttribute("supportForm", new SupportRequestForm());
        }
        model.addAttribute("categories", SupportCategory.values());
        return "support/customer-new";
    }

    @PostMapping
    public String create(@ModelAttribute("supportForm") SupportRequestForm form,
                         HttpSession session, RedirectAttributes redirectAttributes) {
        User customer = SessionUsers.current(session);
        if (customer == null) return SessionUsers.redirectToLogin("/ho-tro/moi");
        try {
            SupportConversation conversation = supportService.create(customer.getId(), form.getSubject(),
                    form.getCategory(), form.getContent());
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                    "Đã gửi yêu cầu. Nhân viên rạp sẽ phản hồi ngay trong cuộc trò chuyện này.");
            return "redirect:/ho-tro/" + conversation.getId();
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute("supportForm", form);
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "redirect:/ho-tro/moi";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        User customer = SessionUsers.current(session);
        if (customer == null) return SessionUsers.redirectToLogin("/ho-tro/" + id);
        model.addAttribute("conversation", supportService.findForCustomer(id, customer.getId()));
        return "support/customer-conversation";
    }

    @PostMapping("/{id}/tra-loi")
    public String reply(@PathVariable Long id, @RequestParam String content,
                        HttpSession session, RedirectAttributes redirectAttributes) {
        User customer = SessionUsers.current(session);
        if (customer == null) return SessionUsers.redirectToLogin("/ho-tro/" + id);
        try {
            supportService.replyAsCustomer(id, customer.getId(), content);
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
        }
        return "redirect:/ho-tro/" + id;
    }
}
