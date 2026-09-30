package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.payment.MomoPaymentResult;
import edu.hcmute.cnpm.cinema.dto.payment.MomoQrPayment;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.MomoPaymentService;
import edu.hcmute.cnpm.cinema.service.PaymentService;
import edu.hcmute.cnpm.cinema.service.QrCodeService;
import edu.hcmute.cnpm.cinema.service.PaymentOtpService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Thanh toán qua ví MoMo, hai cách:
 * <ul>
 *   <li>quét mã QR ngay trên trang của rạp (ưu tiên, không phải nhập thẻ);</li>
 *   <li>chuyển sang trang MoMo để trả bằng thẻ ATM hoặc thẻ quốc tế.</li>
 * </ul>
 */
@Controller
@RequestMapping("/thanh-toan")
public class MomoPaymentController {

    private static final Logger log = LoggerFactory.getLogger(MomoPaymentController.class);
    /** Giữ tối đa chừng này mã QR trong session; khách bấm tạo mã nhiều lần thì bỏ mã cũ nhất. */
    private static final int MAX_QR_PAYMENTS_IN_SESSION = 5;

    private final MomoPaymentService momoPaymentService;
    private final PaymentService paymentService;
    private final QrCodeService qrCodeService;
    private final PaymentOtpService paymentOtp;

    public MomoPaymentController(MomoPaymentService momoPaymentService, PaymentService paymentService,
                                 QrCodeService qrCodeService, PaymentOtpService paymentOtp) {
        this.momoPaymentService = momoPaymentService;
        this.paymentService = paymentService;
        this.qrCodeService = qrCodeService;
        this.paymentOtp = paymentOtp;
    }

    /** Khách chọn "Thẻ ATM / thẻ quốc tế": tạo giao dịch rồi chuyển thẳng sang trang của MoMo. */
    @PostMapping("/{showtimeId}/momo")
    public String startPayment(@PathVariable Long showtimeId, HttpSession session,
                               @RequestParam(required = false) String code,
                               RedirectAttributes redirectAttributes) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/thanh-toan/" + showtimeId);
        }
        try {
            String url = paymentOtp.verify((String) session.getAttribute(PaymentController.PAYMENT_OTP),
                    customer.getId(), showtimeId, "momo", code,
                    () -> momoPaymentService.startPayment(customer.getId(), showtimeId));
            session.removeAttribute(PaymentController.PAYMENT_OTP);
            return "redirect:" + url;
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "redirect:/thanh-toan/" + showtimeId;
        }
    }

    /** Khách chọn "Quét mã QR MoMo": tạo giao dịch rồi mở trang hiện mã QR của rạp. */
    @PostMapping("/{showtimeId}/momo-qr")
    public String startQrPayment(@PathVariable Long showtimeId, HttpSession session,
                                 @RequestParam(required = false) String code,
                                 RedirectAttributes redirectAttributes) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/thanh-toan/" + showtimeId);
        }
        try {
            MomoQrPayment qrPayment = paymentOtp.verify((String) session.getAttribute(PaymentController.PAYMENT_OTP),
                    customer.getId(), showtimeId, "momo-qr", code,
                    () -> momoPaymentService.startQrPayment(customer.getId(), showtimeId));
            session.removeAttribute(PaymentController.PAYMENT_OTP);
            rememberQrPayment(session, qrPayment);
            return "redirect:/thanh-toan/momo/qr/" + qrPayment.orderId();
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "redirect:/thanh-toan/" + showtimeId;
        }
    }

    /** Trang hiện mã QR. Tải lại trang vẫn là mã cũ, không tạo thêm giao dịch MoMo. */
    @GetMapping("/momo/qr/{orderId}")
    public String showQrPayment(@PathVariable String orderId, HttpSession session, Model model,
                                RedirectAttributes redirectAttributes) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/thanh-toan/momo/qr/" + orderId);
        }
        MomoQrPayment qrPayment = qrPayments(session).get(orderId);
        if (qrPayment == null || !customer.getId().equals(qrPayment.userId())) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE,
                    "Mã QR này không còn dùng được. Nếu bạn đã trả tiền, vé sẽ nằm trong Vé của tôi.");
            return "redirect:/ve-cua-toi";
        }

        long secondsLeft = Math.max(0, Duration.between(LocalDateTime.now(), qrPayment.expiresAt()).getSeconds());
        model.addAttribute("qrPayment", qrPayment);
        model.addAttribute("qrSvg", qrCodeService.toSvg(qrPayment.qrCodeUrl(),
                "Mã QR thanh toán " + qrPayment.amount() + " đồng qua MoMo"));
        model.addAttribute("secondsLeft", secondsLeft);
        model.addAttribute("tickets", paymentService.findPayableTickets(customer.getId(), qrPayment.showtimeId()));
        return "account/momo-qr";
    }

    /**
     * Trang QR hỏi vài giây một lần: khách đã trả chưa? Đã trả thì xuất vé luôn và báo trình
     * duyệt chuyển sang trang hoàn tất.
     */
    @GetMapping(value = "/momo/qr/{orderId}/trang-thai", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public QrStatusResponse checkQrPayment(@PathVariable String orderId, HttpSession session) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return new QrStatusResponse("ERROR", "Phiên đăng nhập đã hết. Bạn đăng nhập lại để xem vé.",
                    "/dang-nhap");
        }
        MomoPaymentResult result;
        try {
            result = momoPaymentService.checkQrPayment(orderId, customer.getId());
        } catch (BusinessException exception) {
            // Mất kết nối MoMo một lúc thì trang cứ hỏi tiếp, không bỏ cuộc.
            return new QrStatusResponse("ERROR", exception.getMessage(), null);
        }
        return switch (result.getOutcome()) {
            case PAID, ALREADY_PAID -> new QrStatusResponse("PAID", "MoMo đã nhận tiền. Đang mở vé của bạn…",
                    "/thanh-toan/momo/qr/" + orderId + "/xong");
            case PENDING -> new QrStatusResponse("PENDING", result.getMessage(), null);
            case FAILED -> new QrStatusResponse("FAILED", result.getMessage(), "/thanh-toan/" + result.getShowtimeId());
            case REFUNDED -> new QrStatusResponse("REFUNDED", result.getMessage(),
                    "/booking/showtime/" + result.getShowtimeId());
        };
    }

    /**
     * Bước cuối sau khi MoMo báo đã nhận tiền, cũng là đích của nút "Tôi đã thanh toán" (dùng
     * được cả khi trình duyệt tắt JavaScript). Hỏi lại MoMo lần nữa chứ không tin trình duyệt.
     */
    @GetMapping("/momo/qr/{orderId}/xong")
    public String finishQrPayment(@PathVariable String orderId, HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/ve-cua-toi");
        }
        MomoPaymentResult result;
        try {
            result = momoPaymentService.checkQrPayment(orderId, customer.getId());
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "redirect:/ve-cua-toi";
        }

        switch (result.getOutcome()) {
            case PAID, ALREADY_PAID -> {
                qrPayments(session).remove(orderId);
                redirectAttributes.addFlashAttribute("paidTicketIds", result.getTicketIds());
                return "redirect:/thanh-toan/hoan-tat";
            }
            case PENDING -> {
                redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE,
                        "MoMo chưa báo nhận được tiền. Bạn quét mã và bấm xác nhận trên app MoMo trước nhé.");
                return "redirect:/thanh-toan/momo/qr/" + orderId;
            }
            case FAILED -> {
                qrPayments(session).remove(orderId);
                redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, result.getMessage());
                return "redirect:/thanh-toan/" + result.getShowtimeId();
            }
            default -> {
                qrPayments(session).remove(orderId);
                redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, result.getMessage());
                return "redirect:/booking/showtime/" + result.getShowtimeId();
            }
        }
    }

    /** MoMo đưa khách quay về đây sau khi trả tiền (hoặc bấm huỷ) bên MoMo. */
    @GetMapping("/momo/ket-qua")
    public String handleReturn(@RequestParam Map<String, String> params, RedirectAttributes redirectAttributes) {
        MomoPaymentResult result;
        try {
            result = momoPaymentService.handleResult(params);
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "redirect:/";
        }

        switch (result.getOutcome()) {
            case PAID -> {
                redirectAttributes.addFlashAttribute("paidTicketIds", result.getTicketIds());
                return "redirect:/thanh-toan/hoan-tat";
            }
            case ALREADY_PAID -> {
                redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE,
                        "Giao dịch MoMo này đã được xác nhận trước đó. Vé của bạn ở ngay dưới đây.");
                return "redirect:/ve-cua-toi";
            }
            case FAILED -> {
                redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, result.getMessage());
                return "redirect:/thanh-toan/" + result.getShowtimeId();
            }
            default -> {
                redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, result.getMessage());
                return "redirect:/booking/showtime/" + result.getShowtimeId();
            }
        }
    }

    /**
     * MoMo gọi thẳng về máy chủ để báo kết quả (IPN). Khi chạy trên localhost thì MoMo
     * không gọi tới được, trang kết quả ở trên sẽ lo việc xác nhận. Đưa lên máy chủ có tên
     * miền thật thì đường này bảo đảm vé vẫn được xác nhận dù khách tắt trình duyệt giữa chừng.
     */
    @PostMapping("/momo/ipn")
    @ResponseBody
    public ResponseEntity<Void> handleIpn(@RequestBody Map<String, Object> body) {
        Map<String, String> params = new HashMap<>();
        body.forEach((key, value) -> params.put(key, value == null ? "" : String.valueOf(value)));
        try {
            momoPaymentService.handleResult(params);
        } catch (BusinessException exception) {
            log.warn("IPN MoMo bi tu choi: {}", exception.getMessage());
        }
        return ResponseEntity.noContent().build();
    }

    private void rememberQrPayment(HttpSession session, MomoQrPayment qrPayment) {
        Map<String, MomoQrPayment> payments = qrPayments(session);
        while (payments.size() >= MAX_QR_PAYMENTS_IN_SESSION) {
            payments.values().stream()
                    .min((first, second) -> first.expiresAt().compareTo(second.expiresAt()))
                    .ifPresent(oldest -> payments.remove(oldest.orderId()));
        }
        payments.put(qrPayment.orderId(), qrPayment);
        // Gán lại để session lưu ra đĩa hoặc chia sẻ giữa nhiều máy chủ vẫn thấy thay đổi.
        session.setAttribute(Constants.SESSION_MOMO_QR_PAYMENTS, payments);
    }

    @SuppressWarnings("unchecked")
    private Map<String, MomoQrPayment> qrPayments(HttpSession session) {
        Object stored = session.getAttribute(Constants.SESSION_MOMO_QR_PAYMENTS);
        if (stored instanceof Map<?, ?> map) {
            return (Map<String, MomoQrPayment>) map;
        }
        Map<String, MomoQrPayment> created = new HashMap<>();
        session.setAttribute(Constants.SESSION_MOMO_QR_PAYMENTS, created);
        return created;
    }

    /**
     * Trả lời cho trang QR.
     *
     * @param state       PENDING, PAID, FAILED, REFUNDED, hoặc ERROR (lỗi tạm thời, hỏi tiếp)
     * @param redirectUrl trang nên chuyển sang, nếu có
     */
    public record QrStatusResponse(String state, String message, String redirectUrl) {}
}
