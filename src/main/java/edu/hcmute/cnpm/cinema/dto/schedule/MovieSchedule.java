package edu.hcmute.cnpm.cinema.dto.schedule;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Showtime;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Lịch chiếu của MỘT phim trong MỘT ngày, đã gom sẵn theo loại phòng.
 *
 * Khoá của {@code showtimesByRoomType} là nhãn loại phòng ("Phòng thường",
 * "Premium", "Gold Class") và thứ tự các khoá đã được sắp cố định từ
 * {@code ScheduleService}, nên giao diện cứ duyệt tuần tự là ra đúng thứ tự
 * muốn hiển thị.
 */
public class MovieSchedule {

    private final Movie movie;
    private final Map<String, List<Showtime>> showtimesByRoomType;

    public MovieSchedule(Movie movie, Map<String, List<Showtime>> showtimesByRoomType) {
        this.movie = movie;
        this.showtimesByRoomType = showtimesByRoomType;
    }

    public Movie getMovie() { return movie; }

    public Map<String, List<Showtime>> getShowtimesByRoomType() { return showtimesByRoomType; }

    /** Tổng số suất chiếu của phim này trong ngày, dùng cho dòng tóm tắt. */
    public int getTotalShowtimes() {
        int total = 0;
        for (List<Showtime> showtimes : showtimesByRoomType.values()) {
            total += showtimes.size();
        }
        return total;
    }

    /** Giá vé thấp nhất trong ngày, để hiện "Giá từ ...". */
    public BigDecimal getLowestPrice() {
        BigDecimal lowest = null;
        for (List<Showtime> showtimes : showtimesByRoomType.values()) {
            for (Showtime showtime : showtimes) {
                if (lowest == null || showtime.getBasePrice().compareTo(lowest) < 0) {
                    lowest = showtime.getBasePrice();
                }
            }
        }
        return lowest;
    }
}
