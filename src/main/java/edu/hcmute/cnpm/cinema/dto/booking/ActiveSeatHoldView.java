package edu.hcmute.cnpm.cinema.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Thông tin lượt giữ ghế còn hiệu lực của khách tại một suất chiếu. */
public class ActiveSeatHoldView {
    private final List<Long> ticketIds;
    private final List<Long> seatIds;
    private final List<String> seatLabels;
    private final BigDecimal totalPrice;
    private final LocalDateTime expiresAt;

    public ActiveSeatHoldView(List<Long> ticketIds, List<Long> seatIds, List<String> seatLabels,
                              BigDecimal totalPrice, LocalDateTime expiresAt) {
        this.ticketIds = List.copyOf(ticketIds);
        this.seatIds = List.copyOf(seatIds);
        this.seatLabels = List.copyOf(seatLabels);
        this.totalPrice = totalPrice;
        this.expiresAt = expiresAt;
    }

    public List<Long> getTicketIds() { return ticketIds; }
    public List<Long> getSeatIds() { return seatIds; }
    public List<String> getSeatLabels() { return seatLabels; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public LocalDateTime getExpiresAt() { return expiresAt; }

    public boolean containsSeat(Long seatId) {
        return seatIds.contains(seatId);
    }
}
