package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gửi email xác nhận vé sau khi thanh toán thành công.
 *
 * Hai điều cố ý làm ở đây:
 *
 * 1. Nhận {@link JavaMailSender} qua {@link ObjectProvider}. Spring chỉ tạo bean này
 *    khi có cấu hình {@code spring.mail.host}. Máy nào chưa điền cấu hình email thì
 *    ứng dụng vẫn chạy bình thường, chỉ là không gửi thư - tiện lúc chạy thử và
 *    lúc chạy test.
 *
 * 2. Nuốt mọi lỗi gửi thư. Vé đã thanh toán xong rồi; nếu để lỗi SMTP ném ngược lên
 *    thì giao dịch bị huỷ và khách mất vé chỉ vì server mail chập chờn. Gửi hỏng thì
 *    ghi log để xử lý sau, tiền vé vẫn phải được ghi nhận.
 */
@Service
public class TicketMailService {

    private static final Logger log = LoggerFactory.getLogger(TicketMailService.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;

    public TicketMailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                             @Value("${app.mail.from:khong-tra-loi@utecinema.local}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
    }

    /**
     * Gửi thư xác nhận. Không bao giờ ném lỗi ra ngoài.
     *
     * @return true nếu đã gửi đi, false nếu bỏ qua hoặc gửi hỏng
     */
    public boolean sendTicketConfirmation(User customer, List<Ticket> tickets, BigDecimal total) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("Chua cau hinh email (spring.mail.host), bo qua buoc gui thu xac nhan ve.");
            return false;
        }
        if (customer == null || customer.getEmail() == null || tickets.isEmpty()) {
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(customer.getEmail());
            message.setSubject("UTE Cinema - Xac nhan ve xem phim");
            message.setText(buildBody(customer, tickets, total));
            mailSender.send(message);
            log.info("Da gui thu xac nhan ve toi {}", customer.getEmail());
            return true;
        } catch (RuntimeException exception) {
            log.warn("Gui thu xac nhan ve that bai cho {}: {}",
                    customer.getEmail(), exception.getMessage());
            return false;
        }
    }

    private String buildBody(User customer, List<Ticket> tickets, BigDecimal total) {
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
            body.append(tickets.get(index).getSeat().getSeatRow())
                .append(tickets.get(index).getSeat().getSeatColumn());
        }
        body.append('\n');
        body.append("Tổng   : ").append(total.toPlainString()).append(" đ\n\n");
        body.append("Bạn vui lòng tới quầy trước giờ chiếu 15 phút để nhận vé.\n");
        body.append("Cảm ơn bạn đã chọn UTE Cinema.\n");
        return body.toString();
    }
}
