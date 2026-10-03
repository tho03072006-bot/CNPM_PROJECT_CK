package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.receipt.*;
import edu.hcmute.cnpm.cinema.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

/** Hóa đơn và lịch sử dùng chung dữ liệu vé, giá gốc, hoàn tiền và mã công khai. */
@Service
public class ReceiptService {
    private final BookingOrderService bookingOrderService;
    private final BookingTicketDataService ticketData;
    private final TicketCodeService codes;
    private final QrCodeService qrCodes;
    private final String sellerName, sellerAddress, sellerEmail;

    public ReceiptService(BookingOrderService bookingOrderService, BookingTicketDataService ticketData,
                          TicketCodeService codes, QrCodeService qrCodes,
                          @Value("${app.cinema.name:Rạp chiếu phim UTE Cinema}") String sellerName,
                          @Value("${app.cinema.address:Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh}") String sellerAddress,
                          @Value("${app.cinema.email:hotro@utecinema.local}") String sellerEmail) {
        this.bookingOrderService = bookingOrderService;
        this.ticketData = ticketData; this.codes = codes; this.qrCodes = qrCodes;
        this.sellerName = sellerName; this.sellerAddress = sellerAddress; this.sellerEmail = sellerEmail;
    }

    @Transactional
    public ReceiptView findReceipt(String receiptCode, User viewer) {
        BookingOrder order = bookingOrderService.findReceiptForUser(receiptCode, viewer);
        var data = ticketData.describe(order);
        var lines = new ArrayList<>(data.lines());
        for (BookingOrderItem item : order.getItems()) {
            lines.add(new ReceiptLine(lines.size() + 1, item.getProductName(), "Nhận tại quầy bắp nước", "Phần",
                    item.getQuantity(), item.getUnitPrice(), item.getLineTotal(), false, null, null));
        }
        var admission = data.tickets().stream().filter(ticket -> ticket.publicCode() != null)
                .map(ticket -> new ReceiptTicket(ticket.seatLabel(), ticket.publicCode(),
                        ticket.refunded() ? "REFUNDED" : "Đã sử dụng".equals(ticket.statusLabel()) ? "CHECKED_IN"
                                : ticket.qrSvg() != null ? "PAID" : "INVALID")).toList();
        String payload = null;
        if (data.tickets().stream().anyMatch(ticket -> ticket.qrSvg() != null)) {
            payload = admission.size() == 1 ? codes.payload(admission.getFirst().code())
                    : codes.bookingPayload(order.getReceiptCode());
        }
        String svg = payload == null ? null : qrCodes.toSvg(payload, admission.size() == 1
                ? "QR vé " + admission.getFirst().code() : "QR chung " + admission.size() + " ghế");
        return new ReceiptView(order, String.format("%07d", order.getId()), lines, data.seats(),
                paymentMethodLabel(order.getPaymentMethod()), VietnameseMoneyWords.of(order.getTotalAmount()),
                sellerName, sellerAddress, sellerEmail, admission, svg, payload, data.tickets(), data.refundedAmount());
    }

    private static String paymentMethodLabel(PaymentMethod method) {
        if (method == null) return "Tiền mặt tại quầy";
        return switch (method) {
            case COUNTER -> "Tiền mặt tại quầy";
            case MOMO -> "Ví điện tử MoMo";
            case MOMO_DEMO -> method.getDisplayName();
        };
    }
}
