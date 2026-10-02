package edu.hcmute.cnpm.cinema.dto.booking;

import edu.hcmute.cnpm.cinema.dto.receipt.ReceiptLine;
import edu.hcmute.cnpm.cinema.entity.BookingOrder;
import java.math.BigDecimal;
import java.util.List;

/** Lớp hiển thị riêng cho lịch sử, tái sử dụng dòng hóa đơn mà không đổi DTO của module vé. */
public record BookingDetailView(BookingOrder order, List<ReceiptLine> lines, String seats,
                                String paymentMethodLabel, String amountInWords,
                                List<BookingHistoryTicket> tickets, BigDecimal refundedAmount,
                                BigDecimal discountAmount) {
}
