package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.booking.ActiveSeatHoldView;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Trả ghế về trạng thái trống: dọn vé giữ quá hạn (M2.6) và cho khách tự huỷ
 * giữ ghế trước khi thanh toán (M2.7).
 *
 * <h2>Vì sao XOÁ hẳn dòng vé chứ không đổi sang EXPIRED / CANCELLED</h2>
 *
 * Ràng buộc {@code UNIQUE (showtime_id, seat_id)} của ADR-1 không nhìn cột
 * {@code status}. Chỉ cần dòng vé còn nằm trong bảng là chỗ đó vẫn bị chiếm, nên
 * đổi trạng thái KHÔNG giải phóng được ghế - ghế sẽ chết luôn tới hết suất chiếu.
 * Quyết định xoá hẳn dòng được ghi ở mục ADR-2 trong {@code docs/DATABASE.md}.
 *
 * Chỉ vé CHƯA thanh toán mới bị xoá. Vé đã thanh toán không bao giờ đụng tới.
 *
 * <h2>Vì sao tách khỏi SeatBookingService</h2>
 *
 * Để phần giữ ghế của Module 2 không bị sửa đổi, tránh đụng nhau khi nhiều người
 * cùng làm trên file đó.
 */
@Service
public class SeatHoldService {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldService.class);

    private final TicketRepository ticketRepository;

    public SeatHoldService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Dọn toàn bộ vé đang giữ đã quá {@link Constants#SEAT_HOLD_MINUTES} phút.
     *
     * @return số vé đã trả về trạng thái trống
     */
    @Transactional
    public int releaseExpiredHolds() {
        LocalDateTime heldBefore = LocalDateTime.now().minusMinutes(Constants.SEAT_HOLD_MINUTES);
        List<Ticket> expired = ticketRepository
                .findByStatusAndHeldAtLessThanEqual(TicketStatus.HELD, heldBefore);
        if (expired.isEmpty()) {
            return 0;
        }
        ticketRepository.deleteAll(expired);
        log.info("Da tra {} ghe ve trang thai trong do qua han giu.", expired.size());
        return expired.size();
    }

    /**
     * Lấy lượt giữ ghế còn hiệu lực để người dùng có thể tải lại trang mà không
     * mất đồng hồ, danh sách ghế và đường dẫn thanh toán.
     *
     * Vé vừa hết hạn được xoá ngay trong lần đọc này, không phải chờ tác vụ nền.
     */
    @Transactional
    public Optional<ActiveSeatHoldView> findActiveHold(Long userId, Long showtimeId) {
        if (userId == null || userId <= 0 || showtimeId == null || showtimeId <= 0) {
            return Optional.empty();
        }

        List<Ticket> heldTickets = ticketRepository
                .findByUserIdAndShowtimeIdAndStatus(userId, showtimeId, TicketStatus.HELD);
        if (heldTickets.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime currentTime = LocalDateTime.now();
        List<Ticket> expiredTickets = heldTickets.stream()
                .filter(ticket -> isExpired(ticket, currentTime))
                .toList();
        if (!expiredTickets.isEmpty()) {
            ticketRepository.deleteAll(expiredTickets);
        }

        List<Ticket> activeTickets = heldTickets.stream()
                .filter(ticket -> !isExpired(ticket, currentTime))
                .sorted(Comparator.comparing((Ticket ticket) -> ticket.getSeat().getSeatRow())
                        .thenComparing(ticket -> ticket.getSeat().getSeatColumn()))
                .toList();
        if (activeTickets.isEmpty()) {
            return Optional.empty();
        }

        List<Long> ticketIds = activeTickets.stream().map(Ticket::getId).toList();
        List<Long> seatIds = activeTickets.stream().map(ticket -> ticket.getSeat().getId()).toList();
        List<String> seatLabels = activeTickets.stream()
                .map(ticket -> ticket.getSeat().getSeatRow() + ticket.getSeat().getSeatColumn())
                .toList();
        BigDecimal totalPrice = activeTickets.stream()
                .map(Ticket::getPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime expiresAt = activeTickets.stream()
                .map(ticket -> ticket.getHeldAt().plusMinutes(Constants.SEAT_HOLD_MINUTES))
                .min(LocalDateTime::compareTo)
                .orElse(currentTime);

        return Optional.of(new ActiveSeatHoldView(
                ticketIds, seatIds, seatLabels, totalPrice, expiresAt));
    }

    /**
     * Khách tự huỷ các ghế đang giữ của mình ở một suất chiếu.
     *
     * @return số ghế đã trả lại
     */
    @Transactional
    public int cancelHold(Long userId, Long showtimeId) {
        if (userId == null || showtimeId == null) {
            throw new InvalidBookingException("Thiếu thông tin để huỷ giữ ghế.");
        }
        List<Ticket> held = ticketRepository
                .findByUserIdAndShowtimeIdAndStatus(userId, showtimeId, TicketStatus.HELD);
        if (held.isEmpty()) {
            throw new InvalidBookingException("Bạn không có ghế nào đang giữ ở suất chiếu này.");
        }
        ticketRepository.deleteAll(held);
        return held.size();
    }

    private boolean isExpired(Ticket ticket, LocalDateTime currentTime) {
        return ticket.getHeldAt() == null
                || !ticket.getHeldAt().plusMinutes(Constants.SEAT_HOLD_MINUTES).isAfter(currentTime);
    }
}
