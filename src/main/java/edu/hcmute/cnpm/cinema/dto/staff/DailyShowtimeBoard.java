package edu.hcmute.cnpm.cinema.dto.staff;

import java.time.LocalDate;
import java.util.List;

/**
 * Bảng suất chiếu trong ngày cho nhân viên: lịch từng phòng và thanh thời gian chung.
 *
 * @param breakMinutes      khoảng nghỉ tối thiểu giữa hai suất cùng phòng (phút)
 * @param doorsOpenMinutes  mở cửa đón khách trước giờ chiếu bao nhiêu phút
 * @param hourMarks         các mốc giờ vẽ trên thanh thời gian
 * @param nowLeft           vị trí vạch "bây giờ" trên thanh thời gian (%); null nếu không phải hôm nay
 */
public record DailyShowtimeBoard(LocalDate date, boolean isToday, int breakMinutes, int doorsOpenMinutes,
                                 List<RoomDaySchedule> rooms, List<HourMark> hourMarks, String nowLeft) {

    public DailyShowtimeBoard {
        rooms = List.copyOf(rooms);
        hourMarks = List.copyOf(hourMarks);
    }

    public long getShowtimeCount() {
        return rooms.stream().mapToLong(room -> room.entries().size()).sum();
    }

    public long getSoldSeats() {
        return entries().mapToLong(DailyShowtimeEntry::soldSeats).sum();
    }

    public long getCheckedIn() {
        return entries().mapToLong(DailyShowtimeEntry::checkedIn).sum();
    }

    /** Số suất đang chiếu hoặc đang đón khách lúc này. */
    public long getActiveCount() {
        return entries().filter(DailyShowtimeEntry::isCheckInOpen).count();
    }

    public long getShortBreakCount() {
        return rooms.stream().mapToLong(RoomDaySchedule::getShortBreakCount).sum();
    }

    private java.util.stream.Stream<DailyShowtimeEntry> entries() {
        return rooms.stream().flatMap(room -> room.entries().stream());
    }

    /** Một mốc giờ trên thanh thời gian, ví dụ "14h" ở vị trí 37.5%. */
    public record HourMark(String label, String left) {}
}
