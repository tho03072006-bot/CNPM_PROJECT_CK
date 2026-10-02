package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.booking.*;
import edu.hcmute.cnpm.cinema.dto.receipt.ReceiptLine;
import edu.hcmute.cnpm.cinema.entity.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.text.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Một nguồn dữ liệu vé cho cả hóa đơn và Chi tiết đặt vé. */
@Service
public class BookingTicketDataService {
    private final BookingSnapshotService snapshots;
    private final TicketRefundService refunds;
    private final QrCodeService qrCodes;
    private final BookingClock clock;
    private static final DateTimeFormatter REFUND_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public BookingTicketDataService(BookingSnapshotService snapshots, TicketRefundService refunds,
                                    QrCodeService qrCodes, BookingClock clock) {
        this.snapshots = snapshots; this.refunds = refunds; this.qrCodes = qrCodes; this.clock = clock;
    }

    public record Data(List<BookingHistoryTicket> tickets, List<ReceiptLine> lines, String seats,
                        BigDecimal refundedAmount) {}

    public Data describe(BookingOrder order) {
        Map<Long, Ticket> live = order.getTickets().stream().collect(Collectors.toMap(Ticket::getId, Function.identity()));
        Map<Long, TicketRefund> cancelled = findRefunds(order).stream()
                .collect(Collectors.toMap(TicketRefund::getOriginalTicketId, Function.identity()));
        List<BookingHistoryTicket> tickets = new ArrayList<>();
        List<ReceiptLine> lines = new ArrayList<>();
        StringJoiner seats = new StringJoiner(", ");
        Set<Long> included = new HashSet<>();
        for (BookedTicketSnapshot saved : snapshots.read(order)) {
            included.add(saved.ticketId());
            Ticket ticket = live.get(saved.ticketId());
            TicketRefund refund = cancelled.get(saved.ticketId());
            boolean paid = order.getStatus() == BookingOrderStatus.PAID && ticket != null && ticket.getStatus() == TicketStatus.PAID;
            boolean expired = saved.heldAt() == null || !saved.heldAt().plusMinutes(Constants.SEAT_HOLD_MINUTES).isAfter(clock.now());
            String status = refund != null ? refundState(refund)
                    : paid ? (ticket.getCheckedInAt() == null ? "Đã thanh toán" : "Đã sử dụng")
                    : order.getStatus() == BookingOrderStatus.PAID ? "Vé không còn hiệu lực"
                    : order.getStatus() == BookingOrderStatus.CANCELLED ? "Đã hủy"
                    : expired ? "Hết hạn giữ ghế" : "Chưa thanh toán";
            BigDecimal price = saved.price() == null ? BigDecimal.ZERO : saved.price();
            tickets.add(new BookingHistoryTicket(saved.ticketId(), saved.seatLabel(), seatType(saved.seatType()),
                    price, status, refund != null,
                    paid ? qrCodes.toSvg(saved.ticketId().toString(), "Mã QR vé " + saved.ticketId()) : null));
            seats.add(saved.seatLabel());
            lines.add(line(lines.size() + 1, saved.seatLabel(), seatType(saved.seatType()), price, refund));
        }
        // Chứng từ cũ trước khi có snapshot vẫn khôi phục được vé hủy từ biên nhận.
        for (TicketRefund refund : cancelled.values().stream().sorted(Comparator.comparing(TicketRefund::getOriginalTicketId)).toList()) {
            if (included.contains(refund.getOriginalTicketId())) continue;
            tickets.add(new BookingHistoryTicket(refund.getOriginalTicketId(), refund.getSeatLabel(), "Vé đã hủy",
                    refund.getPaidPrice(), refundState(refund), true, null));
            seats.add(refund.getSeatLabel());
            lines.add(line(lines.size() + 1, refund.getSeatLabel(), "Vé đã hủy", refund.getPaidPrice(), refund));
        }
        return new Data(List.copyOf(tickets), List.copyOf(lines), seats.toString(), cancelled.values().stream()
                .map(TicketRefund::getRefundAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private ReceiptLine line(int number, String seat, String type, BigDecimal price, TicketRefund refund) {
        String detail = refund == null ? type : "Đã hủy lúc " + refund.getRefundedAt().format(REFUND_TIME)
                + ", hoàn lại " + money(refund.getRefundAmount());
        return new ReceiptLine(number, "Vé xem phim - Ghế " + seat, detail, "Vé", 1, price, price, refund != null);
    }

    private List<TicketRefund> findRefunds(BookingOrder order) {
        if (order.getPaidAt() == null) return List.of();
        return refunds.findRefundHistory(order.getUser().getId()).stream()
                .filter(refund -> refund.getShowtimeId().equals(order.getShowtime().getId()))
                .filter(refund -> refund.getPaidAt() != null && refund.getPaidAt().truncatedTo(ChronoUnit.MICROS)
                        .equals(order.getPaidAt().truncatedTo(ChronoUnit.MICROS)))
                .filter(refund -> refund.getPaymentRef() == null || order.getPaymentRef() == null
                        || Objects.equals(refund.getPaymentRef(), order.getPaymentRef())).toList();
    }

    private String seatType(String type) {
        return "VIP".equals(type) ? "Ghế VIP" : "COUPLE".equals(type) ? "Ghế đôi" : "Ghế thường";
    }

    private String refundState(TicketRefund refund) {
        String method = refund.getPaymentMethod() == PaymentMethod.MOMO_DEMO ? "Hoàn tiền mô phỏng"
                : refund.getRefundRef() != null ? "Hoàn về MoMo" : "Nhận tại quầy";
        return "Đã hủy · hoàn " + money(refund.getRefundAmount()) + " · " + method;
    }

    private String money(BigDecimal amount) {
        return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")))
                .format(amount == null ? BigDecimal.ZERO : amount) + " đ";
    }
}
