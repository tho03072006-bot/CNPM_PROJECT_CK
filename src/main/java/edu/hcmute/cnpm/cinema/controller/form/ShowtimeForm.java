package edu.hcmute.cnpm.cinema.controller.form;

import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.service.SeatPricingService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShowtimeForm {
    @NotNull(message = "Vui lòng chọn phim.")
    @Positive(message = "Mã phim phải là số nguyên dương.")
    private Long movieId;
    @NotNull(message = "Vui lòng chọn phòng.")
    @Positive(message = "Mã phòng phải là số nguyên dương.")
    private Long roomId;
    @NotNull(message = "Vui lòng chọn giờ chiếu.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;
    @NotNull(message = "Vui lòng nhập giá vé.")
    @DecimalMin(value = "1", message = "Giá vé phải từ 1 đồng.")
    @DecimalMax(value = SeatPricingService.MAX_BASE_PRICE, message = "Giá vé cơ bản tối đa 49.999.999 đồng.")
    @Digits(integer = 8, fraction = 0, message = "Giá vé phải là số đồng nguyên.")
    private BigDecimal basePrice;

    public static ShowtimeForm from(Showtime showtime) {
        ShowtimeForm form = new ShowtimeForm();
        form.movieId = showtime.getMovie().getId();
        form.roomId = showtime.getRoom().getId();
        form.startTime = showtime.getStartTime();
        form.setBasePrice(showtime.getBasePrice());
        return form;
    }

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) {
        // Accept database values such as 75000.00 without accepting fractional dong.
        this.basePrice = basePrice == null ? null : basePrice.stripTrailingZeros();
    }
}
