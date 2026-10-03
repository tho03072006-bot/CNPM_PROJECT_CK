package edu.hcmute.cnpm.cinema.dto.payment;

/**
 * Giao dịch MoMo vừa tạo.
 *
 * @param payUrl    trang thanh toán của MoMo (trên đó khách cũng quét QR hoặc nhập thẻ được)
 * @param qrCodeUrl dữ liệu để vẽ mã QR cho app MoMo quét; chỉ có khi tạo kiểu captureWallet
 */
public record MomoCheckout(String payUrl, String qrCodeUrl) {
}
