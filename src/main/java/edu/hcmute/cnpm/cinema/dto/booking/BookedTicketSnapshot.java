package edu.hcmute.cnpm.cinema.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Thông tin vé tại thời điểm đặt, tách khỏi ghế đang bị chiếm trong bảng tickets. */
public record BookedTicketSnapshot(Long ticketId, String seatLabel, String seatType,
                                   BigDecimal price, LocalDateTime heldAt) {}
