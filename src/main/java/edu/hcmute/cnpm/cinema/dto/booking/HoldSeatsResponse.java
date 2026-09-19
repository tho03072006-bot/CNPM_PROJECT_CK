package edu.hcmute.cnpm.cinema.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class HoldSeatsResponse {
    private final List<Long> ticketIds;
    private final BigDecimal totalPrice;
    private final LocalDateTime expiresAt;

    public HoldSeatsResponse(List<Long> ticketIds, BigDecimal totalPrice, LocalDateTime expiresAt) {
        this.ticketIds = List.copyOf(ticketIds);
        this.totalPrice = totalPrice;
        this.expiresAt = expiresAt;
    }

    public boolean isSuccess() { return true; }
    public String getMessage() { return "Đã giữ ghế thành công."; }
    public List<Long> getTicketIds() { return ticketIds; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
