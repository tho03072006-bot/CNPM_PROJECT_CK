package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.payment.MomoPaymentResult;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.MomoPaymentService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/** Thanh toán qua ví MoMo: chuyển khách sang MoMo và nhận kết quả MoMo gửi về. */
@Controller
@RequestMapping("/thanh-toan")
public class MomoPaymentController {

    private static final Logger log = LoggerFactory.getLogger(MomoPaymentController.class);

    private final MomoPaymentService momoPaymentService;

    public MomoPaymentController(MomoPaymentService momoPaymentService) {
        this.momoPaymentService = momoPaymentService;
    }

    /** Khách bấm "Thanh toán bằng MoMo": tạo giao dịch rồi chuyển thẳng sang trang của MoMo. */
    @PostMapping("/{showtimeId}/momo")
    public String startPayment(@PathVariable Long showtimeId, HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User customer = SessionUsers.current(session);
        if (customer == null) {
            return SessionUsers.redirectToLogin("/thanh-toan/" + showtimeId);
        }
        try {
            return "redirect:" + momoPaymentService.startPayment(customer.getId(), showtimeId);
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "redirect:/thanh-toan/" + showtimeId;
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
}
