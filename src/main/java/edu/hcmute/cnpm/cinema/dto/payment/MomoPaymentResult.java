package edu.hcmute.cnpm.cinema.dto.payment;

import java.util.List;

/** Kết quả xử lý một lần MoMo báo về. */
public class MomoPaymentResult {

    public enum Outcome {
        /** Vừa chuyển vé sang đã thanh toán. */
        PAID,
        /** Giao dịch này đã xử lý từ trước (khách tải lại trang, hoặc IPN về trước). */
        ALREADY_PAID,
        /** Khách huỷ hoặc thanh toán không thành công bên MoMo. Ghế vẫn đang giữ. */
        FAILED,
        /** Tiền đã trừ nhưng không xuất được vé, nên đã hoàn lại (hoặc báo khách ra quầy). */
        REFUNDED,
        /** Thanh toán QR: khách chưa quét hoặc chưa xác nhận trên app MoMo. */
        PENDING
    }

    private final Outcome outcome;
    private final Long showtimeId;
    private final Long userId;
    private final List<Long> ticketIds;
    private final String message;

    public MomoPaymentResult(Outcome outcome, Long showtimeId, Long userId, List<Long> ticketIds, String message) {
        this.outcome = outcome;
        this.showtimeId = showtimeId;
        this.userId = userId;
        this.ticketIds = ticketIds;
        this.message = message;
    }

    public boolean isPaid() {
        return outcome == Outcome.PAID || outcome == Outcome.ALREADY_PAID;
    }

    public Outcome getOutcome() { return outcome; }
    public Long getShowtimeId() { return showtimeId; }
    public Long getUserId() { return userId; }
    public List<Long> getTicketIds() { return ticketIds; }
    public String getMessage() { return message; }
}
