package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.refund.RefundQuote;
import edu.hcmute.cnpm.cinema.entity.PaymentMethod;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketRefund;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.TicketRefundRepository;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Khách tự huỷ vé đã thanh toán và nhận lại tiền theo chính sách của rạp.
 *
 * CHÍNH SÁCH HOÀN TIỀN (quyết định ngày 27/09/2026):
 * - Còn từ {@value #FULL_REFUND_HOURS} giờ trở lên trước giờ chiếu: hoàn 100%.
 * - Còn từ {@value #MIN_CANCEL_HOURS} tới dưới {@value #FULL_REFUND_HOURS} giờ: hoàn {@value #PARTIAL_REFUND_PERCENT}%.
 * - Còn dưới {@value #MIN_CANCEL_HOURS} giờ, suất đã chiếu, hoặc vé đã soát vào phòng: không nhận huỷ.
 *
 * Lý do: huỷ sớm thì rạp còn nhiều thời gian bán lại ghế nên hoàn đủ; huỷ sát giờ thì ghế
 * khó bán lại, rạp giữ một nửa bù phần thiệt; dưới hai tiếng thì gần như chắc chắn ghế bỏ trống.
 *
 * CÁCH GHI SỔ: vé huỷ bị XOÁ khỏi bảng tickets giống ADR-2 - để lại dòng thì ràng buộc
 * UNIQUE (showtime_id, seat_id) chặn người khác mua ghế đó. Trước khi xoá, chép thông tin
 * tiền bạc sang {@link TicketRefund} để doanh thu vẫn tính đúng phần rạp giữ lại.
 */
@Service
public class TicketRefundService {

    public static final int FULL_REFUND_HOURS = 24;
    public static final int MIN_CANCEL_HOURS = 2;
    public static final int PARTIAL_REFUND_PERCENT = 50;

    private final BookingLockService locks;
    private final TicketRepository ticketRepository;
    private final TicketRefundRepository refundRepository;
    private final MomoApiClient momoApiClient;

    public TicketRefundService(TicketRepository ticketRepository, TicketRefundRepository refundRepository,
                               MomoApiClient momoApiClient, BookingLockService locks) {
        this.locks = locks;
        this.ticketRepository = ticketRepository;
        this.refundRepository = refundRepository;
        this.momoApiClient = momoApiClient;
    }

    /** Tính xem vé này huỷ được không và được hoàn bao nhiêu. Không huỷ được thì ném lỗi kèm lý do. */
    @Transactional(readOnly = true)
    public RefundQuote quote(Long userId, Long ticketId, LocalDateTime now) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("vé", ticketId));
        // Vé của người khác thì báo "không tìm thấy", không để lộ là mã vé đó có tồn tại.
        if (ticket.getUser() == null || !ticket.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("vé", ticketId);
        }
        return quote(ticket, now);
    }

    /** Báo giá cho mọi vé huỷ được của một khách - để trang Vé của tôi biết hiện nút Huỷ ở vé nào. */
    @Transactional(readOnly = true)
    public Map<Long, RefundQuote> quoteCancellableTickets(Long userId, LocalDateTime now) {
        Map<Long, RefundQuote> quotes = new LinkedHashMap<>();
        for (Ticket ticket : ticketRepository.findByUserIdOrderByHeldAtDesc(userId)) {
            if (ticket.getStatus() != TicketStatus.PAID) {
                continue;
            }
            try {
                quotes.put(ticket.getId(), quote(ticket, now));
            } catch (BusinessException notCancellable) {
                // Vé này không huỷ được nữa, không hiện nút.
            }
        }
        return quotes;
    }

    /**
     * Huỷ vé và hoàn tiền trong cùng một transaction.
     *
     * Thứ tự cố ý: ghi biên nhận và xoá vé trước, gọi MoMo hoàn tiền sau cùng. MoMo từ chối
     * thì ném lỗi, transaction rollback, vé còn nguyên - khách không bao giờ mất vé mà chưa
     * được hoàn tiền.
     */
    @Transactional
    public TicketRefund cancelPaidTicket(Long userId, Long ticketId, LocalDateTime now) {
        Long showtimeId = ticketRepository.findShowtimeIdByTicketId(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("vé", ticketId));
        locks.lock(showtimeId);
        RefundQuote quote = quote(userId, ticketId, now);
        Ticket ticket = quote.getTicket();
        TicketRefund refund = snapshot(ticket, quote, now);

        // Xoá có điều kiện ngay trong câu lệnh: nhân viên vừa soát vé ở cửa đúng lúc khách
        // bấm huỷ thì chỉ một bên thắng, không có chuyện vừa vào phòng vừa được hoàn tiền.
        if (ticketRepository.deletePaidTicketNotCheckedIn(ticketId, TicketStatus.PAID) == 0) {
            throw new BusinessException("Vé vừa được soát vào phòng hoặc đã huỷ trước đó, không huỷ được nữa.");
        }
        refund = refundRepository.save(refund);

        boolean isPaidByMomo = refund.getPaymentMethod() == PaymentMethod.MOMO && refund.getPaymentRef() != null;
        if (isPaidByMomo && refund.getRefundAmount().signum() > 0) {
            String refundRef = momoApiClient.refund("UTE-HUY-" + ticketId + "-" + System.currentTimeMillis(),
                    refund.getPaymentRef(), refund.getRefundAmount().longValueExact(),
                    "Hoan tien huy ve " + ticketId + " UTE Cinema");
            refund.setRefundRef(refundRef);
            refund = refundRepository.save(refund);
        }
        return refund;
    }

    /** Các vé khách đã huỷ, mới nhất lên đầu. */
    @Transactional(readOnly = true)
    public List<TicketRefund> findRefundHistory(Long userId) {
        return refundRepository.findByUserIdOrderByRefundedAtDesc(userId);
    }

    private RefundQuote quote(Ticket ticket, LocalDateTime now) {
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessException("Chỉ huỷ được vé đã thanh toán. Ghế đang giữ thì bấm \"Huỷ giữ ghế\" ở trang chọn ghế.");
        }
        if (ticket.getCheckedInAt() != null) {
            throw new BusinessException("Vé đã được soát vào phòng nên không huỷ được nữa.");
        }
        LocalDateTime startTime = ticket.getShowtime().getStartTime();
        if (!startTime.isAfter(now)) {
            throw new BusinessException("Suất chiếu đã bắt đầu nên không huỷ được vé nữa.");
        }
        Duration timeLeft = Duration.between(now, startTime);
        if (timeLeft.compareTo(Duration.ofHours(MIN_CANCEL_HOURS)) < 0) {
            throw new BusinessException("Rạp chỉ nhận huỷ vé trước giờ chiếu ít nhất " + MIN_CANCEL_HOURS + " tiếng.");
        }

        boolean isFullRefund = timeLeft.compareTo(Duration.ofHours(FULL_REFUND_HOURS)) >= 0;
        int percent = isFullRefund ? 100 : PARTIAL_REFUND_PERCENT;
        BigDecimal amount = ticket.getPrice().multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).setScale(2, RoundingMode.UNNECESSARY);
        String explanation = isFullRefund
                ? "Còn từ " + FULL_REFUND_HOURS + " giờ trở lên trước giờ chiếu nên được hoàn đủ 100%."
                : "Còn dưới " + FULL_REFUND_HOURS + " giờ trước giờ chiếu nên được hoàn " + PARTIAL_REFUND_PERCENT + "%.";
        return new RefundQuote(ticket, percent, amount, explanation);
    }

    private TicketRefund snapshot(Ticket ticket, RefundQuote quote, LocalDateTime now) {
        TicketRefund refund = new TicketRefund();
        refund.setOriginalTicketId(ticket.getId());
        refund.setUserId(ticket.getUser().getId());
        refund.setShowtimeId(ticket.getShowtime().getId());
        refund.setMovieTitle(ticket.getShowtime().getMovie().getTitle());
        refund.setRoomName(ticket.getShowtime().getRoom().getName());
        refund.setSeatLabel(ticket.getSeat().getSeatRow() + ticket.getSeat().getSeatColumn());
        refund.setShowtimeStart(ticket.getShowtime().getStartTime());
        refund.setPaidPrice(ticket.getPrice());
        refund.setOriginalPrice(ticket.getOriginalPrice());
        refund.setRefundPercent(quote.getRefundPercent());
        refund.setRefundAmount(quote.getRefundAmount());
        // Vé trả trước ngày có cột payment_method để trống: coi như trả tại quầy.
        refund.setPaymentMethod(ticket.getPaymentMethod() == null ? PaymentMethod.COUNTER : ticket.getPaymentMethod());
        refund.setPaymentRef(ticket.getPaymentRef());
        refund.setPaidAt(ticket.getPaidAt());
        refund.setRefundedAt(now);
        return refund;
    }
}
