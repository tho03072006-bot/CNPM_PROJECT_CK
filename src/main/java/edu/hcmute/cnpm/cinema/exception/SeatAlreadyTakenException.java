package edu.hcmute.cnpm.cinema.exception;

/**
 * Ném ra khi ghế đã có người khác giữ hoặc đặt mất trước đó cho cùng một suất chiếu.
 *
 * ĐÂY LÀ MẮT XÍCH CUỐI CỦA ADR-1. Luồng xử lý bắt buộc của Module 2:
 *
 * <pre>
 *   try {
 *       ticketRepository.saveAndFlush(ticket);
 *   } catch (DataIntegrityViolationException ex) {
 *       // database đã chặn bằng UNIQUE (showtime_id, seat_id)
 *       throw new SeatAlreadyTakenException(showtimeId, seatId, ex);
 *   }
 * </pre>
 *
 * KHÔNG được kiểm tra kiểu "select xem ghế có trống không rồi mới insert" rồi bỏ qua try/catch:
 * giữa lúc select và lúc insert vẫn có thể có người khác chen vào (race-condition).
 * Chỉ có ràng buộc UNIQUE ở database mới chặn chắc chắn được — xem test
 * SeatBookingConcurrencyIntegrationTest.
 *
 * Phụ trách: Thọ (Module 4) - người dùng chính: Thắng (Module 2).
 */
public class SeatAlreadyTakenException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private static final String USER_MESSAGE = "Ghế này vừa có người khác giữ mất rồi. Vui lòng chọn ghế khác.";

    private final Long showtimeId;
    private final Long seatId;

    public SeatAlreadyTakenException(Long showtimeId, Long seatId) {
        super(USER_MESSAGE);
        this.showtimeId = showtimeId;
        this.seatId = seatId;
    }

    public SeatAlreadyTakenException(Long showtimeId, Long seatId, Throwable cause) {
        super(USER_MESSAGE, cause);
        this.showtimeId = showtimeId;
        this.seatId = seatId;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public Long getSeatId() {
        return seatId;
    }
}
