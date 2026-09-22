package edu.hcmute.cnpm.cinema.dto.booking;

import java.math.BigDecimal;

/** Dữ liệu sơ đồ ghế; không đưa thông tin người giữ ghế ra giao diện. */
public class SeatView {
    private final Long id;
    private final String seatRow;
    private final Integer seatColumn;
    private final String seatType;
    private final BigDecimal price;
    private final String status;

    public SeatView(Long id, String seatRow, Integer seatColumn, String seatType,
                    BigDecimal price, String status) {
        this.id = id;
        this.seatRow = seatRow;
        this.seatColumn = seatColumn;
        this.seatType = seatType;
        this.price = price;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getSeatRow() { return seatRow; }
    public Integer getSeatColumn() { return seatColumn; }
    public String getSeatType() { return seatType; }
    public BigDecimal getPrice() { return price; }
    public String getStatus() { return status; }
    public boolean isAvailable() { return "AVAILABLE".equals(status); }
    public int getCapacity() { return "COUPLE".equals(seatType) ? 2 : 1; }

    public String getSeatTypeLabel() {
        return switch (seatType) {
            case "NORMAL" -> "Ghế thường";
            case "VIP" -> "Ghế VIP";
            case "COUPLE" -> "Ghế đôi";
            default -> "Loại ghế chưa xác định";
        };
    }

    public String getStatusLabel() {
        return switch (status) {
            case "AVAILABLE" -> "Còn trống";
            case "HELD" -> "Đang giữ";
            case "PAID" -> "Đã bán";
            default -> "Chưa mở lại";
        };
    }
}
