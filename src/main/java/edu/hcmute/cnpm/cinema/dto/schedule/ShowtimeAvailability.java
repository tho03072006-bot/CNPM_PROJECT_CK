package edu.hcmute.cnpm.cinema.dto.schedule;

/** Tóm tắt sức chứa của một suất chiếu để hiển thị trước khi khách chọn ghế. */
public class ShowtimeAvailability {
    private final int totalSeats;
    private final int reservedSeats;
    private final int remainingSeats;
    private final int occupancyPercent;

    public ShowtimeAvailability(int totalSeats, int reservedSeats) {
        this.totalSeats = Math.max(totalSeats, 0);
        this.reservedSeats = Math.min(Math.max(reservedSeats, 0), this.totalSeats);
        this.remainingSeats = Math.max(this.totalSeats - this.reservedSeats, 0);
        this.occupancyPercent = this.totalSeats == 0
                ? 100 : (int) Math.round(this.reservedSeats * 100.0 / this.totalSeats);
    }

    public int getTotalSeats() { return totalSeats; }
    public int getReservedSeats() { return reservedSeats; }
    public int getRemainingSeats() { return remainingSeats; }
    public int getOccupancyPercent() { return occupancyPercent; }

    public boolean isSoldOut() {
        return remainingSeats == 0;
    }

    public boolean isAlmostFull() {
        return !isSoldOut() && (remainingSeats <= 10 || occupancyPercent >= 80);
    }

    public String getLabel() {
        if (isSoldOut()) {
            return "Hết vé";
        }
        if (isAlmostFull()) {
            return "Chỉ còn " + remainingSeats + " ghế";
        }
        return "Còn " + remainingSeats + " ghế";
    }
}
