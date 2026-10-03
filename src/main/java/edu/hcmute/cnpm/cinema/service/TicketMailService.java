package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketRefund;
import edu.hcmute.cnpm.cinema.entity.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Soạn nội dung thư gửi khách về vé: xác nhận đặt vé và xác nhận hoàn tiền.
 *
 * Việc gửi thật (công tắc bật/tắt, bỏ qua địa chỉ thử nghiệm, nuốt lỗi SMTP) do
 * {@link MailDelivery} lo. Vé đã thanh toán xong rồi thì thư gửi hỏng cũng không được
 * làm khách mất vé, nên các hàm ở đây không bao giờ ném lỗi.
 */
@Service
public class TicketMailService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final MailDelivery mailDelivery;
    private final TicketCodeService codes;

    public TicketMailService(MailDelivery mailDelivery, TicketCodeService codes) {
        this.codes = codes;
        this.mailDelivery = mailDelivery;
    }

    /**
     * Gửi thư xác nhận đặt vé. Không bao giờ ném lỗi ra ngoài.
     *
     * @return true nếu đã gửi đi, false nếu bỏ qua hoặc gửi hỏng
     */
    public boolean sendTicketConfirmation(User customer, List<Ticket> tickets, BigDecimal total) {
        // Chưa bật email thì dừng ngay, khỏi đọc phim/phòng/ghế của vé (chạy ngoài
        // transaction như trong test là dính LazyInitializationException).
        if (!mailDelivery.isReady()) {
            return false;
        }
        if (customer == null || customer.getEmail() == null || tickets.isEmpty()) {
            return false;
        }
        return mailDelivery.send(customer.getEmail(), "UTE Cinema - Xác nhận vé xem phim",
                buildConfirmationBody(customer, tickets, total));
    }

    /** Gửi thư xác nhận đã huỷ vé và số tiền hoàn. Không bao giờ ném lỗi ra ngoài. */
    public boolean sendRefundConfirmation(User customer, TicketRefund refund) {
        if (!mailDelivery.isReady()) {
            return false;
        }
        if (customer == null || customer.getEmail() == null || refund == null) {
            return false;
        }
        return mailDelivery.send(customer.getEmail(), "UTE Cinema - Xác nhận huỷ vé #" + codes.codeFor(refund.getOriginalTicketId()),
                buildRefundBody(customer, refund));
    }

    private String buildConfirmationBody(User customer, List<Ticket> tickets, BigDecimal total) {
        Ticket first = tickets.get(0);
        StringBuilder body = new StringBuilder();
        body.append("Chào ").append(customer.getFullName()).append(",\n\n");
        body.append("UTE Cinema xác nhận bạn đã đặt vé thành công.\n\n");
        body.append("Phim   : ").append(first.getShowtime().getMovie().getTitle()).append('\n');
        body.append("Suất   : ").append(TIME_FORMAT.format(first.getShowtime().getStartTime())).append('\n');
        body.append("Phòng  : ").append(first.getShowtime().getRoom().getName()).append('\n');
        body.append("Ghế    : ");
        for (int index = 0; index < tickets.size(); index++) {
            if (index > 0) {
                body.append(", ");
            }
            Ticket ticket = tickets.get(index);
            body.append(ticket.getSeat().getSeatRow()).append(ticket.getSeat().getSeatColumn())
                .append(" (mã vé #").append(codes.codeFor(ticket.getId())).append(')');
        }
        body.append('\n');
        body.append("Tổng   : ").append(formatMoney(total)).append("\n\n");
        body.append("Tới rạp bạn đọc mã vé cho nhân viên soát vé ở cửa phòng chiếu.\n");
        body.append("Cần huỷ vé thì vào mục Vé của tôi trên trang web.\n\n");
        body.append("Cảm ơn bạn đã chọn UTE Cinema.\n");
        return body.toString();
    }

    private String buildRefundBody(User customer, TicketRefund refund) {
        StringBuilder body = new StringBuilder();
        body.append("Chào ").append(customer.getFullName()).append(",\n\n");
        body.append("UTE Cinema xác nhận đã huỷ vé #").append(refund.getOriginalTicketId()).append(".\n\n");
        body.append("Phim      : ").append(refund.getMovieTitle()).append('\n');
        body.append("Suất      : ").append(TIME_FORMAT.format(refund.getShowtimeStart())).append('\n');
        body.append("Phòng/Ghế : ").append(refund.getRoomName()).append(" / ").append(refund.getSeatLabel()).append('\n');
        body.append("Giá vé    : ").append(formatMoney(refund.getPaidPrice())).append('\n');
        body.append("Hoàn lại  : ").append(formatMoney(refund.getRefundAmount()))
            .append(" (").append(refund.getRefundPercent()).append("%)\n\n");
        if (refund.getRefundRef() != null) {
            body.append("Tiền được hoàn về ví MoMo đã dùng để thanh toán (mã giao dịch hoàn ")
                .append(refund.getRefundRef()).append(").\n");
        } else if (refund.getRefundAmount().signum() > 0) {
            body.append("Vé trả tại quầy nên bạn nhận lại tiền mặt tại quầy vé, nhớ mang theo thư này.\n");
        }
        body.append("\nCảm ơn bạn đã chọn UTE Cinema.\n");
        return body.toString();
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(Locale.US, "%,d đ", amount.longValue()).replace(',', '.');
    }
}
