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
    private final edu.hcmute.cnpm.cinema.repository.BookingOrderRepository orders;
    private final TicketRefundService refundService;
    private final TicketCodeService codes;
    private final QrCodeService qrCodes;
    private final String sellerName;
    private final String sellerAddress;
    private final String sellerEmail;

    public ReceiptService(BookingOrderService bookingOrderService, TicketRefundService refundService,
                          TicketCodeService codes, QrCodeService qrCodes,
                          edu.hcmute.cnpm.cinema.repository.BookingOrderRepository orders,
                          @Value("${app.cinema.name:Rạp chiếu phim UTE Cinema}") String sellerName,
                          @Value("${app.cinema.address:Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh}")
                          String sellerAddress,
                          @Value("${app.cinema.email:hotro@utecinema.local}") String sellerEmail) {
        this.orders = orders;
        this.bookingOrderService = bookingOrderService;
        this.refundService = refundService;
        this.codes = codes;
        this.qrCodes = qrCodes;
        this.sellerName = sellerName;
        this.sellerAddress = sellerAddress;
        this.sellerEmail = sellerEmail;
    }

    @Transactional
    public ReceiptView findReceipt(String receiptCode, User viewer) {
        BookingOrder order = bookingOrderService.findReceiptForUser(receiptCode, viewer);

        List<ReceiptLine> lines = new ArrayList<>();
        List<edu.hcmute.cnpm.cinema.dto.receipt.ReceiptTicket> admission = new ArrayList<>();
        var refunds = findRefundsOf(order);
        List<Long> ticketIds = new ArrayList<>(order.getTickets().stream().map(Ticket::getId).toList());
        ticketIds.addAll(refunds.stream().map(TicketRefund::getOriginalTicketId).toList());
        var publicCodes = codes.codesFor(ticketIds);
        StringJoiner seats = new StringJoiner(", ");
        for (Ticket ticket : order.getTickets().stream().sorted(java.util.Comparator
                .comparing((Ticket t) -> t.getSeat().getSeatRow())
                .thenComparing(t -> t.getSeat().getSeatColumn()).thenComparing(Ticket::getId)).toList()) {
            Seat seat = ticket.getSeat();
            String seatCode = seat.getSeatRow() + seat.getSeatColumn();
            seats.add(seatCode);
            String publicCode = publicCodes.get(ticket.getId());
            admission.add(new edu.hcmute.cnpm.cinema.dto.receipt.ReceiptTicket(seatCode, publicCode,
                    ticket.getCheckedInAt() == null ? "PAID" : "CHECKED_IN"));
            BigDecimal price = ticket.getPrice() == null ? BigDecimal.ZERO : ticket.getPrice();
            lines.add(new ReceiptLine(lines.size() + 1, "Vé xem phim - Ghế " + seatCode,
                    seatTypeLabel(seat.getSeatType()), "Vé", 1, price, price, false, publicCode, null));
        }
        // Vé hủy sau khi thanh toán bị xóa khỏi bảng tickets (ADR-3) nhưng đã nằm trên hóa đơn,
        // nên lấy lại từ biên nhận hoàn tiền để các dòng vẫn cộng khớp "Cộng tiền vé".
        for (TicketRefund refund : refunds) {
            seats.add(refund.getSeatLabel());
            admission.add(new edu.hcmute.cnpm.cinema.dto.receipt.ReceiptTicket(refund.getSeatLabel(),
                    publicCodes.get(refund.getOriginalTicketId()), "REFUNDED"));
            lines.add(new ReceiptLine(lines.size() + 1, "Vé xem phim - Ghế " + refund.getSeatLabel(),
                    "Đã hủy lúc " + refund.getRefundedAt().format(REFUND_TIME) + ", hoàn lại "
                            + formatMoney(refund.getRefundAmount()),
                    "Vé", 1, refund.getPaidPrice(), refund.getPaidPrice(), true, publicCodes.get(refund.getOriginalTicketId()), null));
        }
        for (BookingOrderItem item : order.getItems()) {
            lines.add(new ReceiptLine(lines.size() + 1, item.getProductName(), "Nhận tại quầy bắp nước", "Phần",
                    item.getQuantity(), item.getUnitPrice(), item.getLineTotal(), false, null, null));
        }

        String admissionPayload = null;
        if (!order.getTickets().isEmpty()) admissionPayload = admission.size() == 1
                ? codes.payload(admission.getFirst().code()) : codes.bookingPayload(order.getReceiptCode());
        String admissionQr = admissionPayload == null ? null : qrCodes.toSvg(admissionPayload,
                admission.size() == 1 ? "QR vé " + admission.getFirst().code() : "QR chung " + admission.size() + " ghế");
        return new ReceiptView(order, String.format("%07d", order.getId()), lines, seats.toString(),
                paymentMethodLabel(order.getPaymentMethod()),
                VietnameseMoneyWords.of(order.getTotalAmount()), sellerName, sellerAddress, sellerEmail, admission, admissionQr, admissionPayload);
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
                .filter(refund -> {
                    if (refund.getBookingOrderId() != null)
                        return refund.getBookingOrderId().equals(order.getId());
                    // Chứng từ cũ chỉ ghép khi đúng thời điểm/ref và có DUY NHẤT một đơn khớp.
                    return refund.getShowtimeId().equals(order.getShowtime().getId())
                            && refund.getPaidAt() != null && refund.getPaidAt().equals(order.getPaidAt())
                            && java.util.Objects.equals(refund.getPaymentRef(), order.getPaymentRef())
                            && orders.countLegacyRefundMatches(order.getUser().getId(), order.getShowtime().getId(),
                                    refund.getPaidAt(), refund.getPaymentRef()) == 1;
                }).toList();
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
