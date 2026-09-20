package edu.hcmute.cnpm.cinema.controller.form;

import edu.hcmute.cnpm.cinema.entity.Showtime;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShowtimeForm {
    @NotNull(message = "Vui lòng chọn phim.")
    private Long movieId;
    @NotNull(message = "Vui lòng chọn phòng.")
    private Long roomId;
    @NotNull(message = "Vui lòng chọn giờ chiếu.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;
    @NotNull(message = "Vui lòng nhập giá vé.")
    @DecimalMin(value = "0.01", message = "Giá vé phải lớn hơn 0.")
    private BigDecimal basePrice;

    public static ShowtimeForm from(Showtime showtime) {
        ShowtimeForm form = new ShowtimeForm();
        form.movieId = showtime.getMovie().getId();
        form.roomId = showtime.getRoom().getId();
        form.startTime = showtime.getStartTime();
        form.basePrice = showtime.getBasePrice();
        return form;
    }

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
}
