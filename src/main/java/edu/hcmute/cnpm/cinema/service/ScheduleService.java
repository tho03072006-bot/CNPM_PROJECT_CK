package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.schedule.MovieSchedule;
import edu.hcmute.cnpm.cinema.dto.schedule.ScheduleDate;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dựng lịch chiếu của cả rạp theo từng ngày, phục vụ trang {@code /lich-chieu}.
 *
 * Khác với trang chi tiết phim (xem lịch của một phim), trang này trả lời câu
 * hỏi ngược lại: "ngày mai rạp có những phim gì, chiếu lúc mấy giờ".
 */
@Service
public class ScheduleService {

    /** Số ngày lịch chiếu cho khách chọn trên dải ngày. */
    public static final int SCHEDULE_DAYS = 7;

    public static final String ROOM_TYPE_STANDARD = "Phòng thường";
    public static final String ROOM_TYPE_PREMIUM = "Premium";
    public static final String ROOM_TYPE_GOLD = "Gold Class";

    /** Thứ tự hiển thị các nhóm phòng, từ phổ thông tới cao cấp. */
    private static final List<String> ROOM_TYPE_ORDER =
            List.of(ROOM_TYPE_STANDARD, ROOM_TYPE_PREMIUM, ROOM_TYPE_GOLD);

    private final ShowtimeRepository showtimeRepository;

    public ScheduleService(ShowtimeRepository showtimeRepository) {
        this.showtimeRepository = showtimeRepository;
    }

    /**
     * Các ngày sắp tới thật sự có suất chiếu, tính từ thời điểm hiện tại.
     *
     * Ngày nào không còn suất nào chưa bắt đầu thì không xuất hiện, để khách
     * không bấm vào một ngày rồi thấy trang trống.
     */
    @Transactional(readOnly = true)
    public List<ScheduleDate> findScheduleDates() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime until = today.plusDays(SCHEDULE_DAYS).atStartOfDay();

        List<ScheduleDate> dates = new ArrayList<>();
        LocalDate previous = null;
        // Một rạp chỉ có vài trăm suất trong một tuần nên lọc trong Java cho dễ
        // đọc, không cần viết thêm câu truy vấn gom nhóm riêng.
        for (Showtime showtime : showtimeRepository.findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(now, until)) {
            if (!isVisible(showtime)) {
                continue;
            }
            LocalDate date = showtime.getStartTime().toLocalDate();
            if (!date.equals(previous)) {
                dates.add(new ScheduleDate(date, today));
                previous = date;
            }
        }
        return dates;
    }

    /**
     * Lịch chiếu của một ngày, gom theo phim rồi theo loại phòng.
     *
     * Nếu ngày được hỏi là hôm nay thì chỉ lấy các suất chưa bắt đầu.
     */
    @Transactional(readOnly = true)
    public List<MovieSchedule> findScheduleFor(LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime from = startOfDay.isBefore(now) ? now : startOfDay;
        LocalDateTime until = date.plusDays(1).atStartOfDay();
        if (!from.isBefore(until)) {
            return List.of();
        }

        List<Showtime> showtimes = new ArrayList<>();
        for (Showtime showtime : showtimeRepository.findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(from, until)) {
            if (isVisible(showtime)) {
                showtimes.add(showtime);
            }
        }
        // Xếp theo tên phim để thứ tự trên trang luôn giống nhau giữa các lần mở,
        // trong mỗi phim thì theo giờ chiếu tăng dần.
        showtimes.sort(Comparator
                .comparing((Showtime showtime) -> showtime.getMovie().getTitle())
                .thenComparing(Showtime::getStartTime));

        Map<Long, Movie> moviesById = new LinkedHashMap<>();
        Map<Long, Map<String, List<Showtime>>> grouped = new LinkedHashMap<>();
        for (Showtime showtime : showtimes) {
            Long movieId = showtime.getMovie().getId();
            moviesById.putIfAbsent(movieId, showtime.getMovie());
            grouped.computeIfAbsent(movieId, key -> new LinkedHashMap<>())
                    .computeIfAbsent(resolveRoomType(showtime.getRoom().getName()), key -> new ArrayList<>())
                    .add(showtime);
        }

        List<MovieSchedule> schedule = new ArrayList<>();
        for (Map.Entry<Long, Map<String, List<Showtime>>> entry : grouped.entrySet()) {
            Map<String, List<Showtime>> ordered = new LinkedHashMap<>();
            for (String roomType : ROOM_TYPE_ORDER) {
                List<Showtime> ofType = entry.getValue().get(roomType);
                if (ofType != null) {
                    ordered.put(roomType, ofType);
                }
            }
            schedule.add(new MovieSchedule(moviesById.get(entry.getKey()), ordered));
        }
        return schedule;
    }

    /**
     * Suy ra loại phòng từ tên phòng.
     *
     * Rạp đặt tên phòng theo kiểu "Cinema 7 - PREMIUM", "Cinema 8 - GOLD CLASS",
     * còn phòng thường thì chỉ có số. Dữ liệu mẫu trong {@code seed-data.sql}
     * cũng đặt tên theo đúng quy ước này.
     */
    private String resolveRoomType(String roomName) {
        String name = roomName == null ? "" : roomName.toUpperCase(Locale.ROOT);
        if (name.contains("GOLD CLASS")) {
            return ROOM_TYPE_GOLD;
        }
        if (name.contains("PREMIUM")) {
            return ROOM_TYPE_PREMIUM;
        }
        return ROOM_TYPE_STANDARD;
    }

    /** Chỉ hiện suất chiếu của phim còn đang chiếu. */
    private boolean isVisible(Showtime showtime) {
        Movie movie = showtime.getMovie();
        return movie != null && Boolean.TRUE.equals(movie.getActive());
    }
}
