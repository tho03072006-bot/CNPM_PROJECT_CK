package edu.hcmute.cnpm.cinema.dto.booking;

import edu.hcmute.cnpm.cinema.entity.RoomType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SeatMapView {
    /** Thứ tự loại ghế trên chú thích: từ rẻ tới đắt. */
    private static final List<String> SEAT_TYPE_ORDER = List.of("NORMAL", "VIP", "COUPLE");

    private final Long showtimeId;
    private final String movieTitle;
    private final String roomName;
    private final LocalDateTime startTime;
    private final Integer totalColumns;
    private final List<SeatView> seats;

    public SeatMapView(Long showtimeId, String movieTitle, String roomName,
                       LocalDateTime startTime, Integer totalColumns, List<SeatView> seats) {
        this.showtimeId = showtimeId;
        this.movieTitle = movieTitle;
        this.roomName = roomName;
        this.startTime = startTime;
        this.totalColumns = totalColumns;
        this.seats = List.copyOf(seats);
    }

    public Long getShowtimeId() { return showtimeId; }
    public String getMovieTitle() { return movieTitle; }
    public String getRoomName() { return roomName; }
    public LocalDateTime getStartTime() { return startTime; }
    public Integer getTotalColumns() { return totalColumns; }
    public List<SeatView> getSeats() { return seats; }

    public RoomType getRoomType() { return RoomType.fromRoomName(roomName); }

    /**
     * Số cột ghế thật sự cần vẽ. Thường bằng số cột của phòng, nhưng nếu dữ liệu có ghế
     * nằm ngoài số cột khai báo thì nới ra để ghế đó không đè lên nhãn hàng.
     */
    public int getGridColumns() {
        int widest = totalColumns == null ? 0 : totalColumns;
        for (SeatView seat : seats) {
            if (seat.getSeatColumn() != null && seat.getSeatColumn() > widest) {
                widest = seat.getSeatColumn();
            }
        }
        return widest;
    }

    /**
     * Ghế gom theo hàng, giữ thứ tự hàng như danh sách ghế (đã xếp A, B, C...).
     * Mỗi hàng biết số thứ tự của mình để đặt đúng dòng trên lưới.
     */
    public List<SeatRowView> getRows() {
        Map<String, List<SeatView>> seatsByRow = new LinkedHashMap<>();
        for (SeatView seat : seats) {
            seatsByRow.computeIfAbsent(seat.getSeatRow(), ignored -> new ArrayList<>()).add(seat);
        }
        List<SeatRowView> rows = new ArrayList<>();
        int gridRow = 1;
        for (Map.Entry<String, List<SeatView>> entry : seatsByRow.entrySet()) {
            rows.add(new SeatRowView(entry.getKey(), gridRow++, entry.getValue()));
        }
        return rows;
    }

    /** Các loại ghế có trong phòng này kèm giá của suất chiếu này, để làm chú thích. */
    public List<SeatTypeOption> getSeatTypeOptions() {
        List<SeatTypeOption> options = new ArrayList<>();
        for (String seatType : SEAT_TYPE_ORDER) {
            List<SeatView> ofType = seats.stream()
                    .filter(seat -> seatType.equals(seat.getSeatType()))
                    .toList();
            if (!ofType.isEmpty()) {
                SeatView sample = ofType.getFirst();
                options.add(new SeatTypeOption(seatType, sample.getSeatTypeLabel(), sample.getCssModifier(),
                        sample.getPrice(), sample.getCapacity(), ofType.size()));
            }
        }
        return options;
    }

    public long getAvailableSeatCount() {
        return seats.stream().filter(SeatView::isAvailable).count();
    }
}
