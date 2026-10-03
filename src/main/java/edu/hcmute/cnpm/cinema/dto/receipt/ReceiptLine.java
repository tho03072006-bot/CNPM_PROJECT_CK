package edu.hcmute.cnpm.cinema.dto.receipt;

import java.math.BigDecimal;

/**
 * Một dòng hàng hóa, dịch vụ trên hóa đơn.
 *
 * @param number   số thứ tự (STT)
 * @param unit     đơn vị tính: "Vé" hoặc "Phần"
 * @param refunded vé đã hủy và hoàn tiền sau khi thanh toán; hóa đơn vẫn giữ dòng như lúc phát hành
 */
public record ReceiptLine(int number, String description, String detail, String unit, int quantity,
                          BigDecimal unitPrice, BigDecimal amount, boolean refunded, String ticketCode, String qrSvg) {
}
