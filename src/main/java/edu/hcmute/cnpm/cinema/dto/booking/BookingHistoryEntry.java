package edu.hcmute.cnpm.cinema.dto.booking;

import edu.hcmute.cnpm.cinema.entity.BookingOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Một lần đặt vé, dùng tên phim, suất và số tiền đã lưu trên đơn hàng. */
public record BookingHistoryEntry(String receiptCode, LocalDateTime createdAt,
                                  String movieTitle, String posterUrl, String genre,
                                  Integer durationMin, String ageRating, String roomName,
                                  LocalDateTime showtimeStart, BigDecimal totalAmount,
                                  BookingOrderStatus status, String statusLabel) {
}
