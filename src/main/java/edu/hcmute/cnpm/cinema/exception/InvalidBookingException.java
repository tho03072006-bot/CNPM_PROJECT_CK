package edu.hcmute.cnpm.cinema.exception;

/**
 * Ném ra khi yêu cầu đặt vé không hợp lệ về mặt nghiệp vụ, ví dụ:
 *   - suất chiếu đã bắt đầu hoặc đã chiếu xong;
 *   - vé đã hết hạn giữ (quá {@code Constants.SEAT_HOLD_MINUTES} phút);
 *   - đặt quá số ghế tối đa cho 1 lần;
 *   - thanh toán cho vé không còn ở trạng thái HELD;
 *   - ghế được chọn không thuộc phòng chiếu của suất chiếu đó.
 *
 * Message truyền vào phải nói RÕ lý do cho người dùng biết phải làm gì tiếp,
 * ví dụ: "Suất chiếu này đã bắt đầu, bạn không thể đặt vé nữa."
 *
 * Phụ trách: Thọ (Module 4) - người dùng chính: Thắng (Module 2), Thanh (Module 3).
 */
public class InvalidBookingException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InvalidBookingException(String message) {
        super(message);
    }

    public InvalidBookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
