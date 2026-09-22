package edu.hcmute.cnpm.cinema.dto.booking;

import java.time.LocalDateTime;
import java.util.List;

public class SeatMapView {
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
}
