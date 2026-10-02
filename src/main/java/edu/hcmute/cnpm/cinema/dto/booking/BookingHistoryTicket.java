package edu.hcmute.cnpm.cinema.dto.booking;

import java.math.BigDecimal;

/** Dữ liệu vé chỉ dùng ở lịch sử; mã QR cùng chứa ID số mà bộ soát vé hiện tại nhận. */
public record BookingHistoryTicket(Long ticketId, String seatLabel, String seatTypeLabel,
                                   BigDecimal price, String statusLabel, boolean refunded, String qrSvg) {
}
