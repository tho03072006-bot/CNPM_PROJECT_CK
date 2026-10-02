package edu.hcmute.cnpm.cinema.dto.receipt;

import edu.hcmute.cnpm.cinema.entity.BookingOrder;

import java.util.List;

/**
 * Dữ liệu trang hóa đơn điện tử.
 *
 * @param invoiceNumber số hóa đơn 7 chữ số, đánh theo thứ tự đơn hàng
 * @param seats         danh sách ghế, ví dụ "C5, C6"
 * @param amountInWords tổng tiền viết bằng chữ
 * @param lookupUrl     đường dẫn tra cứu hóa đơn, cũng là nội dung mã QR
 * @param qrSvg         mã QR dạng SVG, nhúng thẳng vào trang
 */
public record ReceiptView(BookingOrder order, String invoiceNumber, List<ReceiptLine> lines, String seats,
                          String paymentMethodLabel, String amountInWords, String lookupUrl, String qrSvg,
                          String sellerName, String sellerAddress, String sellerEmail) {
}
