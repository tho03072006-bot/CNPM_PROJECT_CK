package edu.hcmute.cnpm.cinema.dto.payment;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Một lần thanh toán bằng mã QR đang chờ khách quét. Lưu trong session để trang QR tải lại
 * vẫn hiện đúng mã cũ, không tạo giao dịch MoMo mới mỗi lần bấm F5.
 *
 * @param expiresAt lúc ghế hết thời gian giữ - quá giờ này thì trả tiền cũng không xuất được vé
 */
public record MomoQrPayment(String orderId, Long showtimeId, Long userId, long amount,
                            String qrCodeUrl, String payUrl, LocalDateTime expiresAt) implements Serializable {
}
