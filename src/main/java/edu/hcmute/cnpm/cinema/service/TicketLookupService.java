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
 * Vé hợp lệ thì nhân viên bấm "Cho vào" để ghi lại giờ vào phòng ({@link #checkIn}). Từ đó
 * soát lại cùng mã vé sẽ báo "đã vào phòng", chặn một vé dùng hai lần.
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

    @Transactional
    public TicketCheckResult checkIn(Long ticketId) {
        return checkIn(ticketId, LocalDateTime.now());
    }

    /**
     * Ghi nhận khách đã vào phòng. Chỉ vé đang hợp lệ mới vào được.
     *
     * Câu UPDATE có điều kiện "chưa vào phòng" nằm ngay trong database, nên hai nhân viên
     * soát cùng một vé ở hai cửa cùng lúc thì chỉ một người được - người kia nhận báo lỗi.
     */
    @Transactional
    public TicketCheckResult checkIn(Long ticketId, LocalDateTime now) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy vé có mã #" + ticketId + "."));
        TicketCheckResult current = assess(ticket, now);
        if (!current.isValid()) {
            throw new BusinessException(current.getMessage());
        }
        if (ticketRepository.markCheckedIn(ticketId, now, TicketStatus.PAID) == 0) {
            throw new BusinessException("Vé #" + ticketId + " vừa được soát ở cửa khác, không cho vào lần hai.");
        }
        return current;
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
        if (ticket.getCheckedInAt() != null) {
            return new TicketCheckResult(ticket, Verdict.CHECKED_IN,
                    "Vé đã được soát vào phòng lúc " + ticket.getCheckedInAt().format(TIME_FORMAT)
                            + " ngày " + ticket.getCheckedInAt().format(DATE_FORMAT) + ", không dùng lại được.");
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
