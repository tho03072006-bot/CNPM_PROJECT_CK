package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.booking.BookingDetailView;
import edu.hcmute.cnpm.cinema.dto.receipt.ReceiptLine;
import edu.hcmute.cnpm.cinema.dto.receipt.ReceiptView;
import edu.hcmute.cnpm.cinema.entity.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

/** Dùng chung dữ liệu vé, QR và ưu đãi của chứng từ thanh toán. */
@Service
public class BookingDetailService {
    private final ReceiptService receipts;
    private final BookingTicketDataService ticketData;

    public BookingDetailService(ReceiptService receipts, BookingTicketDataService ticketData) {
        this.receipts = receipts; this.ticketData = ticketData;
    }

    /** Chỉ được gọi sau khi BookingHistoryService xác nhận chủ đơn. */
    public BookingDetailView describe(BookingOrder order) {
        ReceiptView invoice = order.getStatus() == BookingOrderStatus.PAID
                ? receipts.findReceipt(order.getReceiptCode(), order.getUser()) : null;
        BookingTicketDataService.Data data = invoice == null ? ticketData.describe(order) : null;
        List<ReceiptLine> lines = invoice == null ? new ArrayList<>(data.lines()) : invoice.lines();
        if (invoice == null) {
            for (BookingOrderItem item : order.getItems()) {
                lines.add(new ReceiptLine(lines.size() + 1, item.getProductName(), "Nhận tại quầy bắp nước",
                        "Phần", item.getQuantity(), item.getUnitPrice(), item.getLineTotal(), false, null, null));
            }
        }
        BigDecimal refunded = invoice == null ? data.refundedAmount() : invoice.refundedAmount();
        return new BookingDetailView(order, List.copyOf(lines), invoice == null ? data.seats() : invoice.seats(),
                invoice == null ? (order.getPaymentMethod() == null ? "Chưa chọn phương thức thanh toán"
                        : order.getPaymentMethod().getDisplayName()) : invoice.paymentMethodLabel(),
                invoice == null ? VietnameseMoneyWords.of(order.getTotalAmount()) : invoice.amountInWords(),
                invoice == null ? data.tickets() : invoice.tickets(), refunded, order.getDiscountAmount());
    }
}
