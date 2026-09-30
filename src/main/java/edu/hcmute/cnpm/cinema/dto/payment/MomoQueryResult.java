package edu.hcmute.cnpm.cinema.dto.payment;

/**
 * Trạng thái một giao dịch, lấy bằng cách hỏi thẳng MoMo (API query).
 *
 * @param resultCode 0 là đã trả tiền, 1000 là đang chờ khách xác nhận; mã khác xem tài liệu MoMo
 * @param message    lời MoMo giải thích, tiếng Việt
 * @param transId    mã giao dịch bên MoMo, chỉ có khi đã trả tiền
 * @param amount     số tiền của giao dịch
 */
public record MomoQueryResult(int resultCode, String message, String transId, long amount) {
}
