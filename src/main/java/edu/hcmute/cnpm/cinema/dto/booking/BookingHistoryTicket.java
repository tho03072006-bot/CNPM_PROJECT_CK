package edu.hcmute.cnpm.cinema.dto.booking;

import java.math.BigDecimal;

/** Vé lịch sử giữ ID nội bộ và dùng mã công khai 8 số cho QR vào phòng. */
public record BookingHistoryTicket(Long ticketId, String seatLabel, String seatTypeLabel,
                                   BigDecimal price, String statusLabel, boolean refunded, String qrSvg, String publicCode) {
}
