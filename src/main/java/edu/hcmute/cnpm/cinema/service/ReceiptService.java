package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.receipt.ReceiptLine;
import edu.hcmute.cnpm.cinema.dto.receipt.ReceiptView;
import edu.hcmute.cnpm.cinema.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Dựng hóa đơn điện tử theo bố cục hóa đơn bán hàng thật: thông tin đơn vị bán, số hóa đơn,
 * bảng hàng hóa có STT và đơn vị tính, số tiền viết bằng chữ.
 *
 * Thông tin rạp đọc từ cấu hình app.cinema.* (có giá trị mặc định), muốn đổi thì thêm
 * vào application.properties, không phải sửa code.
 */
@Service
public class ReceiptService {

    private static final DateTimeFormatter REFUND_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final BookingOrderService bookingOrderService;
    private final TicketRefundService refundService;
    private final String sellerName;
    private final String sellerAddress;
    private final String sellerEmail;

    public ReceiptService(BookingOrderService bookingOrderService, TicketRefundService refundService,
                          @Value("${app.cinema.name:Rạp chiếu phim UTE Cinema}") String sellerName,
                          @Value("${app.cinema.address:Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh}")
                          String sellerAddress,
                          @Value("${app.cinema.email:hotro@utecinema.local}") String sellerEmail) {
        this.bookingOrderService = bookingOrderService;
        this.refundService = refundService;
        this.sellerName = sellerName;
        this.sellerAddress = sellerAddress;
        this.sellerEmail = sellerEmail;
    }

    @Transactional(readOnly = true)
    public ReceiptView findReceipt(String receiptCode, User viewer) {
        BookingOrder order = bookingOrderService.findReceiptForUser(receiptCode, viewer);

        List<ReceiptLine> lines = new ArrayList<>();
        StringJoiner seats = new StringJoiner(", ");
        for (Ticket ticket : order.getTickets()) {
            Seat seat = ticket.getSeat();
            String seatCode = seat.getSeatRow() + seat.getSeatColumn();
            seats.add(seatCode);
            BigDecimal price = ticket.getPrice() == null ? BigDecimal.ZERO : ticket.getPrice();
            lines.add(new ReceiptLine(lines.size() + 1, "Vé xem phim - Ghế " + seatCode,
                    seatTypeLabel(seat.getSeatType()), "Vé", 1, price, price, false));
        }
        // Vé hủy sau khi thanh toán bị xóa khỏi bảng tickets (ADR-3) nhưng đã nằm trên hóa đơn,
        // nên lấy lại từ biên nhận hoàn tiền để các dòng vẫn cộng khớp "Cộng tiền vé".
        for (TicketRefund refund : findRefundsOf(order)) {
            seats.add(refund.getSeatLabel());
            lines.add(new ReceiptLine(lines.size() + 1, "Vé xem phim - Ghế " + refund.getSeatLabel(),
                    "Đã hủy lúc " + refund.getRefundedAt().format(REFUND_TIME) + ", hoàn lại "
                            + formatMoney(refund.getRefundAmount()),
                    "Vé", 1, refund.getPaidPrice(), refund.getPaidPrice(), true));
        }
        for (BookingOrderItem item : order.getItems()) {
            lines.add(new ReceiptLine(lines.size() + 1, item.getProductName(), "Nhận tại quầy bắp nước", "Phần",
                    item.getQuantity(), item.getUnitPrice(), item.getLineTotal(), false));
        }

        return new ReceiptView(order, String.format("%07d", order.getId()), lines, seats.toString(),
                paymentMethodLabel(order.getPaymentMethod()),
                VietnameseMoneyWords.of(order.getTotalAmount()), sellerName, sellerAddress, sellerEmail);
    }

    private static String paymentMethodLabel(PaymentMethod method) {
        if (method == null) {
            return "Tiền mặt tại quầy";
        }
        return switch (method) {
            case COUNTER -> "Tiền mặt tại quầy";
            case MOMO -> "Ví điện tử MoMo";
            case MOMO_DEMO -> method.getDisplayName();
        };
    }

    /** Vé cùng khách, cùng suất và trả tiền cùng lúc với đơn này (vé và đơn ghi chung một thời điểm). */
    private List<TicketRefund> findRefundsOf(BookingOrder order) {
        return refundService.findRefundHistory(order.getUser().getId()).stream()
                .filter(refund -> refund.getShowtimeId().equals(order.getShowtime().getId()))
                .filter(refund -> refund.getPaidAt() != null && order.getPaidAt() != null
                        && Duration.between(refund.getPaidAt(), order.getPaidAt()).abs().getSeconds() < 1)
                .toList();
    }

    private static String seatTypeLabel(String seatType) {
        if ("VIP".equals(seatType)) {
            return "Ghế VIP";
        }
        return "COUPLE".equals(seatType) ? "Ghế đôi" : "Ghế thường";
    }

    private static String formatMoney(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")));
        return format.format(amount == null ? BigDecimal.ZERO : amount) + " đ";
    }
}
