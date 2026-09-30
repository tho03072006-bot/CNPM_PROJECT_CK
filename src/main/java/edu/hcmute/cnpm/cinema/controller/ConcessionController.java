package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.form.ConcessionSelectionForm;
import edu.hcmute.cnpm.cinema.entity.BookingOrder;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.BookingOrderService;
import edu.hcmute.cnpm.cinema.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Bước hỏi mua bắp nước nằm giữa giữ ghế và thanh toán. */
@Controller
@RequestMapping("/bap-nuoc")
public class ConcessionController {

    private final BookingOrderService bookingOrderService;
    private final PaymentService paymentService;

    public ConcessionController(BookingOrderService bookingOrderService, PaymentService paymentService) {
        this.bookingOrderService = bookingOrderService;
        this.paymentService = paymentService;
    }

    @GetMapping("/{showtimeId}")
    public String show(@PathVariable Long showtimeId, HttpSession session, Model model) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/bap-nuoc/" + showtimeId);
        }
        BookingOrder order = bookingOrderService.prepareForPayment(customer.getId(), showtimeId);
        ConcessionSelectionForm form = new ConcessionSelectionForm();
        form.setQuantities(bookingOrderService.selectedQuantities(customer.getId(), showtimeId));
        model.addAttribute("selectionForm", form);
        model.addAttribute("products", bookingOrderService.findActiveProducts());
        model.addAttribute("tickets", paymentService.findPayableTickets(customer.getId(), showtimeId));
        model.addAttribute("order", order);
        model.addAttribute("showtimeId", showtimeId);
        return "booking/concessions";
    }

    @PostMapping("/{showtimeId}")
    public String save(@PathVariable Long showtimeId,
                       @ModelAttribute("selectionForm") ConcessionSelectionForm form,
                       HttpSession session, RedirectAttributes redirectAttributes) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/bap-nuoc/" + showtimeId);
        }
        try {
            bookingOrderService.saveConcessions(customer.getId(), showtimeId, form.getQuantities());
            return "redirect:/thanh-toan/" + showtimeId;
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "redirect:/bap-nuoc/" + showtimeId;
        }
    }
}
