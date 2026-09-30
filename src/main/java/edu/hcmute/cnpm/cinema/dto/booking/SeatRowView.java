package edu.hcmute.cnpm.cinema.dto.booking;

import java.util.List;

/**
 * Một hàng ghế trên sơ đồ.
 *
 * @param rowLabel chữ cái của hàng, hiện ở hai đầu hàng
 * @param gridRow  số thứ tự dòng trên lưới CSS, bắt đầu từ 1
 * @param seats    các ghế trong hàng, xếp theo cột tăng dần
 */
public record SeatRowView(String rowLabel, int gridRow, List<SeatView> seats) {
    public SeatRowView {
        seats = List.copyOf(seats);
    }
}
