package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.entity.BookingOrder;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import edu.hcmute.cnpm.cinema.service.MomoPaymentService;
import edu.hcmute.cnpm.cinema.service.BookingOrderService;
import edu.hcmute.cnpm.cinema.service.PaymentService;
import edu.hcmute.cnpm.cinema.service.PaymentOtpService;
import edu.hcmute.cnpm.cinema.service.OtpService;
import edu.hcmute.cnpm.cinema.service.PromotionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/** Trang xác nhận thanh toán cho các ghế khách vừa giữ. */
@Controller
@RequestMapping("/thanh-toan")
public class PaymentController {

    private final edu.hcmute.cnpm.cinema.config.DemoWalletSettings demoWalletSettings;
    private final DemoWalletSessions demoWalletSessions;
    private final PaymentService paymentService;
    private final TicketRepository ticketRepository;
    private final MomoPaymentService momoPaymentService;
    private final BookingOrderService bookingOrderService;
    private final PaymentOtpService paymentOtp;
    private final OtpService otp;
    private final PromotionService promotionService;
    static final String PAYMENT_OTP = "paymentOtp";
    private static final String PAYMENT_METHOD = "paymentOtpMethod";

    public PaymentController(PaymentService paymentService,
                             TicketRepository ticketRepository, MomoPaymentService momoPaymentService,
                             BookingOrderService bookingOrderService,
                             edu.hcmute.cnpm.cinema.config.DemoWalletSettings demoWalletSettings,
                             DemoWalletSessions demoWalletSessions,
                             PaymentOtpService paymentOtp, OtpService otp,
                             PromotionService promotionService) {
        this.promotionService = promotionService;
        this.demoWalletSettings = demoWalletSettings;
        this.demoWalletSessions = demoWalletSessions;
        this.paymentService = paymentService;
        this.ticketRepository = ticketRepository;
        this.momoPaymentService = momoPaymentService;
        this.bookingOrderService = bookingOrderService;
        this.paymentOtp = paymentOtp;
        this.otp = otp;
    }

    @GetMapping("/{showtimeId}")
    public String showPaymentPage(@PathVariable Long showtimeId, HttpSession session, Model model) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/thanh-toan/" + showtimeId);
        }

        List<Ticket> tickets = paymentService.findPayableTickets(customer.getId(), showtimeId);
        BookingOrder order = tickets.isEmpty() ? null
                : bookingOrderService.prepareForPayment(customer.getId(), showtimeId);
        model.addAttribute("tickets", tickets);
        model.addAttribute("ticketSubtotal", paymentService.sumPrice(tickets));
        model.addAttribute("order", order);
        model.addAttribute("offers", promotionService.availableOffers());
        model.addAttribute("total", order == null ? java.math.BigDecimal.ZERO : order.getTotalAmount());
        model.addAttribute("showtimeId", showtimeId);
        model.addAttribute("momoEnabled", momoPaymentService.isEnabled() && !demoWalletSettings.isEnabled());
        model.addAttribute("demoWalletEnabled", demoWalletSettings.isEnabled());
        model.addAttribute("walletCsrf", demoWalletSessions.csrf(session));
        return "account/payment";
    }

    @PostMapping("/{showtimeId}/uu-dai")
    public String applyVoucher(@PathVariable Long showtimeId, @RequestParam(defaultValue = "") String voucherCode,
                               @RequestParam(required = false) List<Long> ticketIds,
                               @RequestParam String walletCsrf, HttpSession session, RedirectAttributes flash) {
        User user = SessionUsers.current(session);
        if (user == null) return SessionUsers.redirectToLogin("/thanh-toan/" + showtimeId);
        try {
            demoWalletSessions.verify(session, walletCsrf);
            BookingOrder order = bookingOrderService.applyVoucher(user.getId(), showtimeId, voucherCode, ticketIds);
            otp.cancel((String) session.getAttribute(PAYMENT_OTP));
            session.removeAttribute(PAYMENT_OTP);
            session.removeAttribute(PAYMENT_METHOD);
            flash.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, order.getVoucherCode() == null
                    ? "Đã bỏ mã ưu đãi." : "Đã áp dụng mã " + order.getVoucherCode() + ". Tổng tiền đã được cập nhật.");
        } catch (BusinessException e) {
            flash.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage());
        }
        return "redirect:/thanh-toan/" + showtimeId;
    }

    @PostMapping("/{showtimeId}")
    public String confirmPayment(@PathVariable Long showtimeId, HttpSession session,
                                 @RequestParam(required = false) String code,
                                 @RequestParam(required = false) List<Long> ticketIds,
                                 RedirectAttributes redirectAttributes) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/thanh-toan/" + showtimeId);
        }

        // Giữ route cũ để form đã mở trước khi cập nhật không tạo vé trả tại quầy.
        redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE,
                "Vé online cần thanh toán bằng QR hoặc thẻ. Bạn hãy chọn phương thức bên dưới.");
        return "redirect:/thanh-toan/" + showtimeId;
    }

    @PostMapping("/{showtimeId}/otp")
    public String requestOtp(@PathVariable Long showtimeId, @RequestParam String method, HttpSession session,
                             RedirectAttributes flash) {
        User user = SessionUsers.current(session);
        if (user == null) return SessionUsers.redirectToLogin("/thanh-toan/" + showtimeId);
        try {
            PaymentOtpService.validateMethod(method);
            if (!momoPaymentService.isEnabled())
                throw new BusinessException("Thanh toán MoMo chưa được cấu hình.");
            String id = paymentOtp.begin(user.getId(), showtimeId, method);
            otp.cancel((String) session.getAttribute(PAYMENT_OTP));
            session.setAttribute(PAYMENT_OTP, id);
            session.setAttribute(PAYMENT_METHOD, method);
            flash.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã gửi mã OTP xác nhận đơn vé tới email của bạn.");
            return "redirect:/thanh-toan/" + showtimeId + "/otp";
        } catch (BusinessException e) {
            flash.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage());
            return "redirect:/thanh-toan/" + showtimeId;
        }
    }

    @GetMapping("/{showtimeId}/otp")
    public String showOtp(@PathVariable Long showtimeId, HttpSession session, Model model, RedirectAttributes flash) {
        User user = SessionUsers.current(session);
        if (user == null) return SessionUsers.redirectToLogin("/thanh-toan/" + showtimeId);
        try {
            String method = (String) session.getAttribute(PAYMENT_METHOD);
            PaymentOtpService.validateMethod(method);
            String action = "/thanh-toan/" + showtimeId + "/" + method;
            OtpPages.populate(model, otp.describe((String) session.getAttribute(PAYMENT_OTP), OtpService.Purpose.PAYMENT),
                    "Xác thực thanh toán", "Kiểm tra đơn vé, sau đó nhập OTP để xác nhận phương thức thanh toán.",
                    action, "/thanh-toan/" + showtimeId + "/otp/gui-lai", "/thanh-toan/" + showtimeId,
                    "Tiếp tục tới MoMo");
            var tickets = paymentService.findPayableTickets(user.getId(), showtimeId);
            if (tickets.isEmpty()) throw new BusinessException("Đã hết thời gian giữ ghế. Hãy chọn ghế lại.");
            model.addAttribute("paymentTotal", paymentService.totalDue(user.getId(), showtimeId));
            model.addAttribute("paymentTickets", tickets);
            return "account/verify-otp";
        } catch (BusinessException e) {
            flash.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage());
            return "redirect:/thanh-toan/" + showtimeId;
        }
    }

    @PostMapping("/{showtimeId}/otp/gui-lai")
    public String resendOtp(@PathVariable Long showtimeId, HttpSession session, RedirectAttributes flash) {
        if (SessionUsers.current(session) == null) return SessionUsers.redirectToLogin("/thanh-toan/" + showtimeId);
        try {
            otp.resend((String) session.getAttribute(PAYMENT_OTP), OtpService.Purpose.PAYMENT);
            flash.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã gửi mã mới. Mã cũ không còn hiệu lực.");
        } catch (BusinessException e) { flash.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, e.getMessage()); }
        return "redirect:/thanh-toan/" + showtimeId + "/otp";
    }

    /**
     * Trang báo đặt vé thành công.
     *
     * Mã vé truyền qua flash attribute nên chỉ hiện đúng một lần ngay sau khi trả
     * tiền. Khách tải lại trang thì không còn dữ liệu, lúc đó chuyển sang trang
     * vé của tôi - vé vẫn ở đó, không mất đi đâu.
     */
    @GetMapping("/hoan-tat")
    public String showPaymentResult(HttpSession session, Model model) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/ve-cua-toi");
        }

        @SuppressWarnings("unchecked")
        List<Long> ticketIds = (List<Long>) model.getAttribute("paidTicketIds");
        if (ticketIds == null || ticketIds.isEmpty()) {
            return "redirect:/ve-cua-toi";
        }

        List<Ticket> tickets = new ArrayList<>();
        for (Ticket ticket : ticketRepository.findAllById(ticketIds)) {
            // Chốt chặn: chỉ hiện vé của chính người đang đăng nhập.
            if (ticket.getUser() != null && ticket.getUser().getId().equals(customer.getId())) {
                tickets.add(ticket);
            }
        }
        model.addAttribute("tickets", tickets);
        model.addAttribute("total", paymentService.sumPrice(tickets));
        Object receiptCode = model.getAttribute("receiptCode");
        if (receiptCode == null && !tickets.isEmpty()) {
            receiptCode = bookingOrderService.findReceiptCodeByTicketId(tickets.get(0).getId(), customer.getId())
                    .orElse(null);
        }
        model.addAttribute("receiptCode", receiptCode);
        if (receiptCode instanceof String code) {
            model.addAttribute("total", bookingOrderService.findReceiptForUser(code, customer).getTotalAmount());
        }
        return "account/payment-success";
    }
}
