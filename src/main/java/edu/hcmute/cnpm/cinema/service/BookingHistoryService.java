package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.booking.BookingHistoryEntry;
import edu.hcmute.cnpm.cinema.dto.booking.BookingDetailView;
import edu.hcmute.cnpm.cinema.entity.BookingOrder;
import edu.hcmute.cnpm.cinema.entity.BookingOrderStatus;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.BookingHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lịch sử theo giao dịch, tách khỏi danh sách từng vé dùng để vào phòng chiếu. */
@Service
public class BookingHistoryService {
    private static final int PAGE_SIZE = 12;
    private final BookingHistoryRepository orders;
    private final BookingDetailService details;
    private final TicketRefundService refunds;
    private final BookingClock clock;

    public BookingHistoryService(BookingHistoryRepository orders, BookingDetailService details,
                                 TicketRefundService refunds, BookingClock clock) {
        this.orders = orders;
        this.details = details;
        this.refunds = refunds; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<BookingHistoryEntry> findHistory(Long userId, int page, BookingOrderStatus status) {
        // JPA chỉ hỗ trợ offset trong phạm vi int, dù số trang nhận vào vẫn là int hợp lệ.
        if (page > Integer.MAX_VALUE / PAGE_SIZE) {
            throw new BusinessException("Số trang lịch sử không hợp lệ. Vui lòng chọn lại trang.");
        }
        PageRequest request = PageRequest.of(Math.max(0, page), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<BookingOrder> result = status == null ? orders.findByUserId(userId, request)
                : orders.findByUserIdAndStatus(userId, status, request);
        return result.map(order -> {
            Movie movie = order.getShowtime().getMovie();
            return new BookingHistoryEntry(order.getReceiptCode(), order.getCreatedAt(), order.getMovieTitle(),
                    movie.getPosterUrl(), movie.getGenre(), movie.getDurationMin(), movie.getAgeRating(),
                    order.getRoomName(), order.getShowtimeStart(), order.getTotalAmount(), order.getStatus(),
                    statusLabel(order.getStatus()));
        });
    }

    @Transactional
    public BookingDetailView findDetail(String receiptCode, Long userId) {
        // Lọc chủ sở hữu ngay trong truy vấn, kể cả người xem là nhân viên/quản trị.
        BookingOrder order = orders.findByReceiptCodeAndUserId(receiptCode, userId)
                .orElseThrow(() -> new ResourceNotFoundException("lần đặt vé", receiptCode));
        return details.describe(order);
    }

    public java.util.Map<Long, edu.hcmute.cnpm.cinema.dto.refund.RefundQuote> cancellationQuotes(Long userId) {
        return refunds.quoteCancellableTickets(userId, clock.now());
    }

    @Transactional
    public String findTicketQr(String receiptCode, Long ticketId, Long userId) {
        return findDetail(receiptCode, userId).tickets().stream()
                .filter(ticket -> ticket.ticketId().equals(ticketId) && ticket.qrSvg() != null)
                .map(edu.hcmute.cnpm.cinema.dto.booking.BookingHistoryTicket::qrSvg).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("mã QR vé", ticketId));
    }

    public static String statusLabel(BookingOrderStatus status) {
        return switch (status) {
            case PAID -> "Đã thanh toán";
            case DRAFT -> "Chưa thanh toán";
            case CANCELLED -> "Đã hủy";
        };
    }
}
