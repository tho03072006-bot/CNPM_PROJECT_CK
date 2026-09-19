package edu.hcmute.cnpm.cinema.dto.booking;

import java.util.List;

/** Chỉ nhận mã ghế; danh tính và giá vé được xác định ở phía máy chủ. */
public class HoldSeatsRequest {
    private List<Long> seatIds;

    public HoldSeatsRequest() { }

    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds; }
}
