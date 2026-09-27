package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.staff.TicketCheckResult;
import edu.hcmute.cnpm.cinema.dto.staff.TicketCheckResult.Verdict;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Soát vé ở cửa phòng chiếu: tra một tấm vé theo mã, hoặc tra mọi vé của một khách
 * theo email khi khách quên mã vé.
 *
 * Chỉ đọc, không ghi gì xuống database. Chưa lưu trạng thái "đã vào phòng" vì như vậy
 * phải thêm cột vào bảng tickets, mà database dùng chung trên cloud chạy
 * ddl-auto=validate - thêm cột là phải cả nhóm thống nhất và sửa schema-cloud.sql trước.
 */
@Service
public class TicketLookupService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketLookupService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public TicketCheckResult checkTicketCode(String ticketCode) {
        return checkTicketCode(ticketCode, LocalDateTime.now());
    }

    /**
     * Soát một tấm vé theo mã in trên vé.
     *
     * Nhận cả "12" lẫn "#12" vì trên vé in kèm dấu thăng, nhân viên gõ kiểu nào cũng được.
     */
    @Transactional(readOnly = true)
    public TicketCheckResult checkTicketCode(String ticketCode, LocalDateTime now) {
        Long ticketId = parseTicketCode(ticketCode);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy vé có mã #" + ticketId + "."));
        return assess(ticket, now);
    }

    @Transactional(readOnly = true)
    public List<TicketCheckResult> findUpcomingTicketsByEmail(String email) {
        return findUpcomingTicketsByEmail(email, LocalDateTime.now());
    }

    /**
     * Mọi vé của một khách cho các suất chiếu chưa kết thúc, suất sớm nhất lên đầu.
     *
     * Vé của suất đã chiếu xong thì bỏ qua: đứng ở cửa phòng không ai cần xem lại chúng.
     */
    @Transactional(readOnly = true)
    public List<TicketCheckResult> findUpcomingTicketsByEmail(String email, LocalDateTime now) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isEmpty()) {
            throw new BusinessException("Bạn hãy nhập email của khách.");
        }
        User customer = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Không có tài khoản nào dùng email " + normalizedEmail + "."));

        return ticketRepository.findByUserIdOrderByHeldAtDesc(customer.getId()).stream()
                .filter(ticket -> ticket.getShowtime().getEndTime().isAfter(now))
                .sorted(Comparator.comparing((Ticket ticket) -> ticket.getShowtime().getStartTime())
                        .thenComparing(ticket -> ticket.getSeat().getSeatRow())
                        .thenComparing(ticket -> ticket.getSeat().getSeatColumn()))
                .map(ticket -> assess(ticket, now))
                .toList();
    }

    /** Đưa ra kết luận cho một tấm vé tại thời điểm {@code now}. */
    TicketCheckResult assess(Ticket ticket, LocalDateTime now) {
        Showtime showtime = ticket.getShowtime();

        if (ticket.getStatus() != TicketStatus.PAID) {
            String message = ticket.getStatus() == TicketStatus.HELD
                    ? "Vé chưa thanh toán. Khách cần thanh toán xong mới được vào phòng."
                    : "Vé không còn hiệu lực.";
            return new TicketCheckResult(ticket, Verdict.NOT_PAID, message);
        }
        if (!showtime.getEndTime().isAfter(now)) {
            return new TicketCheckResult(ticket, Verdict.ENDED,
                    "Suất chiếu đã kết thúc lúc " + showtime.getEndTime().format(TIME_FORMAT)
                            + " ngày " + showtime.getEndTime().format(DATE_FORMAT) + ".");
        }
        if (showtime.getStartTime().toLocalDate().isAfter(now.toLocalDate())) {
            return new TicketCheckResult(ticket, Verdict.WRONG_DAY,
                    "Vé của suất chiếu ngày " + showtime.getStartTime().format(DATE_FORMAT)
                            + ", chưa tới ngày chiếu.");
        }
        return new TicketCheckResult(ticket, Verdict.VALID,
                "Vé hợp lệ. Mời khách vào " + showtime.getRoom().getName() + ", ghế "
                        + ticket.getSeat().getSeatRow() + ticket.getSeat().getSeatColumn() + ".");
    }

    private Long parseTicketCode(String ticketCode) {
        String digits = ticketCode == null ? "" : ticketCode.trim();
        if (digits.startsWith("#")) {
            digits = digits.substring(1).trim();
        }
        if (digits.isEmpty()) {
            throw new BusinessException("Bạn hãy nhập mã vé.");
        }
        if (!digits.matches("\\d{1,18}")) {
            throw new BusinessException("Mã vé chỉ gồm chữ số, ví dụ #12.");
        }
        return Long.valueOf(digits);
    }
}
