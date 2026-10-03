package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.staff.DailyShowtimeBoard;
import edu.hcmute.cnpm.cinema.dto.staff.DailyShowtimeBoard.HourMark;
import edu.hcmute.cnpm.cinema.dto.staff.DailyShowtimeEntry;
import edu.hcmute.cnpm.cinema.dto.staff.RoomDaySchedule;
import edu.hcmute.cnpm.cinema.dto.staff.ShowtimeDayStatus;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.repository.SeatRepository;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bảng "Suất chiếu trong ngày" cho nhân viên: mỗi phòng có những suất nào, phim gì, chiếu từ mấy
 * giờ tới mấy giờ, lúc nào mở cửa đón khách, lúc nào phòng trống lại để dọn.
 *
 * Khác trang lịch chiếu của khách (gom theo phim) và trang quản trị suất chiếu (để sửa lịch):
 * bảng này gom theo PHÒNG, vì nhân viên điều phối khách vào ra theo từng phòng.
 *
 * Nhận "bây giờ" làm tham số thay vì tự đọc đồng hồ, để test được mọi thời điểm trong ngày.
 */
@Service
public class DailyShowtimeService {

    /** Phạm vi ngày của datetime2 trong SQL Server, cũng dùng cho bộ lọc trên giao diện. */
    public static final LocalDate MIN_SUPPORTED_DATE = LocalDate.of(1, 1, 1);
    public static final LocalDate MAX_SUPPORTED_DATE = LocalDate.of(9999, 12, 31);

    /** Mở cửa đón khách vào phòng trước giờ chiếu bao nhiêu phút. */
    public static final int DOORS_OPEN_MINUTES = 15;
    /** Thanh thời gian vẽ ít nhất từ 8 giờ sáng tới nửa đêm, có suất sớm hơn hay muộn hơn thì nới ra. */
    private static final int DEFAULT_TIMELINE_START = 8 * 60;
    private static final int DEFAULT_TIMELINE_END = 24 * 60;

    private final ShowtimeRepository showtimeRepository;
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeService showtimeService;

    public DailyShowtimeService(ShowtimeRepository showtimeRepository, TicketRepository ticketRepository,
                                SeatRepository seatRepository, ShowtimeService showtimeService) {
        this.showtimeRepository = showtimeRepository;
        this.ticketRepository = ticketRepository;
        this.seatRepository = seatRepository;
        this.showtimeService = showtimeService;
    }

    /**
     * Dựng bảng cho một ngày, gồm mọi suất BẮT ĐẦU trong ngày đó (kể cả phim đã ngừng bán: suất đã
     * bán vé vẫn phải chiếu).
     *
     * @param roomId lọc một phòng; null là mọi phòng
     * @param now    thời điểm hiện tại, để biết suất nào đang chiếu, đang đón khách, đang dọn phòng
     */
    @Transactional(readOnly = true)
    public DailyShowtimeBoard buildBoard(LocalDate date, Long roomId, LocalDateTime now) {
        if (date == null || date.isBefore(MIN_SUPPORTED_DATE) || date.isAfter(MAX_SUPPORTED_DATE)) {
            throw new BusinessException("Ngày xem lịch không hợp lệ. Vui lòng chọn ngày khác.");
        }
        int breakMinutes = showtimeService.getBreakMinutes();
        // Ngày cuối database không có mốc 0h hôm sau hợp lệ; không gửi năm 10000 vào JDBC.
        List<Showtime> dayShowtimes = date.equals(MAX_SUPPORTED_DATE)
                ? showtimeRepository.findByStartTimeGreaterThanEqualOrderByStartTimeAsc(date.atStartOfDay())
                : showtimeRepository.findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        List<Showtime> showtimes = dayShowtimes.stream()
                .filter(showtime -> roomId == null || showtime.getRoom().getId().equals(roomId))
                .toList();

        Map<Long, long[]> ticketCounts = countTickets(showtimes);
        int[] window = timelineWindow(showtimes, date, breakMinutes);

        // Gom theo phòng, phòng xếp theo mã (đúng thứ tự Cinema 1, 2, 3... lúc tạo), suất theo giờ.
        Map<Room, List<Showtime>> byRoom = new LinkedHashMap<>();
        showtimes.stream()
                .sorted(Comparator.comparing((Showtime showtime) -> showtime.getRoom().getId())
                        .thenComparing(Showtime::getStartTime))
                .forEach(showtime -> byRoom.computeIfAbsent(showtime.getRoom(), key -> new ArrayList<>()).add(showtime));

        List<RoomDaySchedule> rooms = new ArrayList<>();
        for (Map.Entry<Room, List<Showtime>> roomEntry : byRoom.entrySet()) {
            Room room = roomEntry.getKey();
            long seatCount = seatRepository.countByRoomId(room.getId());
            List<Showtime> roomShowtimes = roomEntry.getValue();
            List<DailyShowtimeEntry> entries = new ArrayList<>();
            for (int index = 0; index < roomShowtimes.size(); index++) {
                Showtime showtime = roomShowtimes.get(index);
                Showtime next = index + 1 < roomShowtimes.size() ? roomShowtimes.get(index + 1) : null;
                entries.add(toEntry(showtime, next, seatCount, ticketCounts, breakMinutes, now, date, window));
            }
            rooms.add(new RoomDaySchedule(room.getId(), room.getName(), room.getRoomType(), seatCount, entries));
        }

        boolean isToday = date.equals(now.toLocalDate());
        int nowMinute = isToday ? minuteOfDay(date, now) : 0;
        String nowLeft = isToday && nowMinute >= window[0] && nowMinute <= window[1]
                ? percent(nowMinute - window[0], window) : null;
        return new DailyShowtimeBoard(date, isToday, breakMinutes, DOORS_OPEN_MINUTES,
                rooms, hourMarks(window), nowLeft);
    }

    private DailyShowtimeEntry toEntry(Showtime showtime, Showtime next, long seatCount,
                                       Map<Long, long[]> ticketCounts, int breakMinutes,
                                       LocalDateTime now, LocalDate date, int[] window) {
        Movie movie = showtime.getMovie();
        int duration = durationOf(showtime);
        LocalDateTime start = showtime.getStartTime();
        LocalDateTime movieEnd = start.plusMinutes(duration);
        LocalDateTime roomReadyAt = movieEnd.plusMinutes(breakMinutes);

        Long minutesToNext = null;
        boolean isBreakTooShort = false;
        if (next != null) {
            minutesToNext = Duration.between(movieEnd, next.getStartTime()).toMinutes();
            isBreakTooShort = minutesToNext < breakMinutes;
        }

        long[] counts = ticketCounts.getOrDefault(showtime.getId(), new long[] {0, 0});
        int startMinute = minuteOfDay(date, start);
        return new DailyShowtimeEntry(showtime.getId(), movie == null ? "" : movie.getTitle(),
                movie == null ? null : movie.getAgeRating(), duration, start, movieEnd, roomReadyAt,
                statusAt(now, start, movieEnd, roomReadyAt), counts[0], counts[1], seatCount,
                minutesToNext, isBreakTooShort,
                percent(startMinute - window[0], window),
                percent(duration, window),
                percent(breakMinutes, window));
    }

    static ShowtimeDayStatus statusAt(LocalDateTime now, LocalDateTime start, LocalDateTime movieEnd,
                                      LocalDateTime roomReadyAt) {
        if (!now.isBefore(roomReadyAt)) {
            return ShowtimeDayStatus.FINISHED;
        }
        if (!now.isBefore(movieEnd)) {
            return ShowtimeDayStatus.CLEANING;
        }
        if (!now.isBefore(start)) {
            return ShowtimeDayStatus.SHOWING;
        }
        if (!now.isBefore(start.minusMinutes(DOORS_OPEN_MINUTES))) {
            return ShowtimeDayStatus.BOARDING;
        }
        return ShowtimeDayStatus.UPCOMING;
    }

    /** [mã suất] -> {số vé đã bán, số khách đã vào phòng}, một truy vấn cho cả ngày. */
    private Map<Long, long[]> countTickets(List<Showtime> showtimes) {
        Map<Long, long[]> counts = new HashMap<>();
        if (showtimes.isEmpty()) {
            return counts;
        }
        List<Long> ids = showtimes.stream().map(Showtime::getId).toList();
        for (Object[] row : ticketRepository.countSoldAndCheckedInByShowtimeIds(ids, TicketStatus.PAID)) {
            counts.put((Long) row[0], new long[] {((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
        }
        return counts;
    }

    /**
     * Khung giờ của thanh thời gian, tính bằng phút kể từ 0 giờ của ngày đang xem. Mặc định 8h-24h,
     * có suất sớm hơn hoặc kết thúc qua nửa đêm thì nới ra, làm tròn theo giờ chẵn.
     */
    private int[] timelineWindow(List<Showtime> showtimes, LocalDate date, int breakMinutes) {
        int start = DEFAULT_TIMELINE_START;
        int end = DEFAULT_TIMELINE_END;
        for (Showtime showtime : showtimes) {
            int startMinute = minuteOfDay(date, showtime.getStartTime());
            int readyMinute = startMinute + durationOf(showtime) + breakMinutes;
            start = Math.min(start, startMinute / 60 * 60);
            end = Math.max(end, (readyMinute + 59) / 60 * 60);
        }
        return new int[] {start, end};
    }

    private List<HourMark> hourMarks(int[] window) {
        List<HourMark> marks = new ArrayList<>();
        for (int minute = window[0]; minute <= window[1]; minute += 120) {
            marks.add(new HourMark((minute / 60) % 24 + "h", percent(minute - window[0], window)));
        }
        return marks;
    }

    private static int durationOf(Showtime showtime) {
        Movie movie = showtime.getMovie();
        if (movie != null && movie.getDurationMin() != null) {
            return movie.getDurationMin();
        }
        return (int) Duration.between(showtime.getStartTime(), showtime.getEndTime()).toMinutes();
    }

    private static int minuteOfDay(LocalDate date, LocalDateTime time) {
        return (int) Duration.between(date.atStartOfDay(), time).toMinutes();
    }

    /** Phần trăm trên thanh thời gian, chuỗi có dấu chấm thập phân để dùng thẳng trong CSS. */
    private static String percent(int minutes, int[] window) {
        double value = 100.0 * minutes / (window[1] - window[0]);
        return String.format(Locale.ROOT, "%.3f", Math.max(0, Math.min(100, value)));
    }
}
