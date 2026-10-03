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
    private final TicketCodeService codes;
    private final BookingClock clock;
    private final BookingLockService locks;
    private final edu.hcmute.cnpm.cinema.repository.BookingOrderRepository orders;
    private final ReceiptService receipts;

    public TicketLookupService(TicketRepository ticketRepository, UserRepository userRepository,
                               TicketCodeService codes, BookingClock clock, BookingLockService locks,
                               edu.hcmute.cnpm.cinema.repository.BookingOrderRepository orders, ReceiptService receipts) {
        this.orders = orders; this.receipts = receipts;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.codes = codes;
        this.clock = clock;
        this.locks = locks;
    }

    @Transactional
    public TicketCheckResult checkTicketCode(String ticketCode) {
        return checkTicketCode(ticketCode, clock.now());
    }

    /**
     * Soát một tấm vé theo mã in trên vé.
     *
     * Chỉ nhận mã công khai 8 số hoặc QR phiên bản 2, không nhận ID nội bộ.
     */
    @Transactional
    public TicketCheckResult checkTicketCode(String ticketCode, LocalDateTime now) {
        String publicCode = codes.parse(ticketCode);
        Long ticketId = codes.resolve(publicCode);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Vé không còn hiệu lực hoặc đã hủy."));
        ticket.setAdmissionCode(publicCode);
        return assess(ticket, now);
    }

    @Transactional
    public List<TicketCheckResult> findUpcomingTicketsByEmail(String email) {
        return findUpcomingTicketsByEmail(email, clock.now());
    }

    /**
     * Mọi vé của một khách cho các suất chiếu chưa kết thúc, suất sớm nhất lên đầu.
     *
     * Vé của suất đã chiếu xong thì bỏ qua: đứng ở cửa phòng không ai cần xem lại chúng.
     */
    @Transactional
    public List<TicketCheckResult> findUpcomingTicketsByEmail(String email, LocalDateTime now) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isEmpty()) {
            throw new BusinessException("Bạn hãy nhập email của khách.");
        }
        if (normalizedEmail.length() > 150 || !normalizedEmail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new BusinessException("Email của khách không hợp lệ.");
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
        return checkIn(ticketId, (LocalDateTime) null);
    }

    /**
     * Ghi nhận khách đã vào phòng. Chỉ vé đang hợp lệ mới vào được.
     *
     * Câu UPDATE có điều kiện "chưa vào phòng" nằm ngay trong database, nên hai nhân viên
     * soát cùng một vé ở hai cửa cùng lúc thì chỉ một người được - người kia nhận báo lỗi.
     */
    @Transactional
    public TicketCheckResult checkIn(Long ticketId, LocalDateTime now) {
        if (ticketId == null || ticketId <= 0) throw new BusinessException("Mã vé phải là số nguyên dương.");
        Long showtimeId = ticketRepository.findShowtimeIdByTicketId(ticketId)
                .orElseThrow(() -> new BusinessException("Vé không còn hiệu lực hoặc đã hủy."));
        // Chung khóa với hoàn vé, thanh toán và cập nhật suất chiếu.
        locks.lock(showtimeId);
        if (now == null) now = clock.now();
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Vé không còn hiệu lực hoặc đã hủy."));
        TicketCheckResult current = assess(ticket, now);
        if (!current.isValid()) {
            throw new BusinessException(current.getMessage());
        }
        if (ticketRepository.markCheckedIn(ticketId, now, TicketStatus.PAID, now.toLocalDate().plusDays(1).atStartOfDay(), null) == 0) {
            throw new BusinessException("Vé vừa thay đổi trạng thái hoặc đã được soát ở cửa khác. Hãy tra cứu lại.");
        }
        return current;
    }

    /** Đưa ra kết luận cho một tấm vé tại thời điểm {@code now}. */
    TicketCheckResult assess(Ticket ticket, LocalDateTime now) {
        Showtime showtime = ticket.getShowtime();
        if (ticket.getStatus() == TicketStatus.PAID && ticket.getAdmissionCode() == null)
            ticket.setAdmissionCode(codes.codeFor(ticket.getId()));

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
        if (!Boolean.TRUE.equals(showtime.getMovie().getActive()))
            return new TicketCheckResult(ticket, Verdict.INVALID, "Vé không hợp lệ: phim đã ngừng chiếu. Vui lòng liên hệ quầy hỗ trợ.");
        if (showtime.getStartTime() == null || showtime.getEndTime() == null
                || !showtime.getEndTime().isAfter(showtime.getStartTime())
                || !ticket.getSeat().getRoom().getId().equals(showtime.getRoom().getId()))
            return new TicketCheckResult(ticket, Verdict.INVALID, "Vé không hợp lệ: thông tin suất chiếu hoặc ghế không khớp.");
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

    @Transactional
    public TicketCheckResult checkIn(Long ticketId, String expectedCode) {
        if (!codes.resolve(expectedCode).equals(ticketId))
            throw new BusinessException("Mã vé và ghế cần xác nhận không khớp. Hãy quét lại.");
        return checkIn(ticketId);
    }
    @Transactional
    public edu.hcmute.cnpm.cinema.dto.staff.BookingTicketCheckResult checkBookingQr(String payload) {
        return assessBooking(codes.parseBooking(payload), clock.now());
    }
    private edu.hcmute.cnpm.cinema.dto.staff.BookingTicketCheckResult assessBooking(String receiptCode, LocalDateTime now) {
        var order = orders.findByReceiptCode(receiptCode)
                .filter(o -> o.getStatus() == edu.hcmute.cnpm.cinema.entity.BookingOrderStatus.PAID)
                .orElseThrow(() -> new BusinessException("QR hóa đơn không tồn tại hoặc chưa thanh toán."));
        var result = order.getTickets().stream().sorted(Comparator
                .comparing((Ticket t) -> t.getSeat().getSeatRow()).thenComparing(t -> t.getSeat().getSeatColumn()))
                .map(t -> {
                    if (!t.getUser().getId().equals(order.getUser().getId())
                            || !t.getShowtime().getId().equals(order.getShowtime().getId())) {
                        t.setAdmissionCode(codes.codeFor(t.getId()));
                        return new TicketCheckResult(t, Verdict.INVALID, "Vé không khớp khách hoặc suất chiếu của hóa đơn.");
                    }
                    return assess(t, now);
                }).toList();
        var refunded = receipts.findReceipt(receiptCode, order.getUser()).admissionTickets().stream()
                .filter(t -> "REFUNDED".equals(t.state())).toList();
        return new edu.hcmute.cnpm.cinema.dto.staff.BookingTicketCheckResult(order, result, refunded);
    }
    @Transactional
    public int checkInBooking(String receiptCode, String qrPayload) {
        if (!codes.parseBooking(qrPayload).equals(receiptCode))
            throw new BusinessException("QR và hóa đơn cần xác nhận không khớp.");
        Long showtimeId = orders.findShowtimeIdByReceiptCode(receiptCode)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hóa đơn cần soát vé."));
        locks.lock(showtimeId);
        LocalDateTime now = clock.now();
        var booking = assessBooking(receiptCode, now);
        if (!booking.canCheckIn())
            throw new BusinessException("Đơn vé không còn ghế hợp lệ để cho vào. Hãy kiểm tra trạng thái từng ghế.");
        var ids = booking.tickets().stream().filter(TicketCheckResult::isValid).map(t -> t.getTicket().getId()).toList();
        for (Long id : ids)
            if (ticketRepository.markCheckedIn(id, now, TicketStatus.PAID, now.toLocalDate().plusDays(1).atStartOfDay(), booking.order().getId()) != 1)
                throw new BusinessException("Một vé vừa thay đổi trạng thái. Chưa xác nhận nhóm vé; hãy quét lại.");
        return ids.size();
    }
}
