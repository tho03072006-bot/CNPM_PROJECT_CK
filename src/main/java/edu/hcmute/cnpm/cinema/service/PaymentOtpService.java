package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.function.Supplier;

@Service
public class PaymentOtpService {
    private record Checkout(Long userId, Long showtimeId, String method, String fingerprint) {}
    private final AuthService auth;
    private final PaymentService payments;
    private final OtpService otp;
    public PaymentOtpService(AuthService auth, PaymentService payments, OtpService otp) {
        this.auth = auth; this.payments = payments; this.otp = otp;
    }
    public String begin(Long userId, Long showtimeId, String method) {
        validateMethod(method);
        User user = auth.findById(userId);
        Checkout checkout = new Checkout(userId, showtimeId, method, fingerprint(userId, showtimeId));
        return otp.begin(OtpService.Purpose.PAYMENT, user.getEmail(), checkout, true);
    }
    public <T> T verify(String id, Long userId, Long showtimeId, String method, String code, Supplier<T> action) {
        validateMethod(method);
        Checkout checkout = otp.payload(id, OtpService.Purpose.PAYMENT, Checkout.class);
        if (!checkout.userId.equals(userId) || !checkout.showtimeId.equals(showtimeId) || !checkout.method.equals(method)
                || !checkout.fingerprint.equals(fingerprint(userId, showtimeId)))
            throw new BusinessException("Thông tin đơn vé đã thay đổi. Hãy yêu cầu OTP thanh toán mới.");
        return otp.verify(id, OtpService.Purpose.PAYMENT, code, action);
    }
    public static void validateMethod(String method) {
        if (method == null || !List.of("momo", "momo-qr").contains(method))
            throw new BusinessException("Vé online chỉ hỗ trợ thanh toán bằng QR hoặc thẻ qua MoMo.");
    }
    private String fingerprint(Long userId, Long showtimeId) {
        List<Ticket> tickets = payments.findPayableTickets(userId, showtimeId);
        if (tickets.isEmpty()) throw new BusinessException("Đã hết thời gian giữ ghế. Bạn hãy chọn ghế lại.");
        return tickets.stream().map(t -> t.getId() + ":" + t.getPrice() + ":" + t.getHeldAt()).sorted()
                .reduce("", (a, b) -> a + "|" + b) + "|total:" + payments.totalDue(userId, showtimeId);
    }
}
