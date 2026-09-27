package edu.hcmute.cnpm.cinema.dto.staff;

import edu.hcmute.cnpm.cinema.entity.Ticket;

/**
 * Kết quả soát một tấm vé ở cửa phòng chiếu.
 *
 * Gồm tấm vé, kết luận cho vào hay không, và câu giải thích để nhân viên đọc cho khách.
 */
public class TicketCheckResult {

    /** Kết luận khi soát vé. */
    public enum Verdict {
        /** Đã thanh toán, đúng ngày chiếu và suất chưa kết thúc: cho vào. */
        VALID,
        /** Khách mới giữ ghế, chưa trả tiền. */
        NOT_PAID,
        /** Vé đã được soát vào phòng rồi, không dùng lại được. */
        CHECKED_IN,
        /** Vé của suất chiếu ngày khác, chưa tới ngày. */
        WRONG_DAY,
        /** Suất chiếu đã kết thúc. */
        ENDED
    }

    private final Ticket ticket;
    private final Verdict verdict;
    private final String message;

    public TicketCheckResult(Ticket ticket, Verdict verdict, String message) {
        this.ticket = ticket;
        this.verdict = verdict;
        this.message = message;
    }

    public Ticket getTicket() { return ticket; }
    public Verdict getVerdict() { return verdict; }
    public String getMessage() { return message; }

    public boolean isValid() {
        return verdict == Verdict.VALID;
    }
}
