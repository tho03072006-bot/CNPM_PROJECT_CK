package edu.hcmute.cnpm.cinema.dto.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Một ngày trên dải chọn ngày của trang lịch chiếu.
 *
 * Nhãn thứ được tính sẵn ở tầng Java thay vì để Thymeleaf tự định dạng, vì
 * {@code #temporals.format(..., 'EEEE')} phụ thuộc vào ngôn ngữ mà trình duyệt
 * gửi lên, nên máy đặt tiếng Anh sẽ hiện "Monday" thay vì "Thứ 2".
 */
public class ScheduleDate {

    private final LocalDate date;
    private final String dayLabel;
    private final boolean today;

    public ScheduleDate(LocalDate date, LocalDate todayDate) {
        this.date = date;
        this.today = date.equals(todayDate);
        this.dayLabel = this.today ? "Hôm nay" : buildDayLabel(date.getDayOfWeek());
    }

    /** Đổi thứ trong tuần sang cách gọi của tiếng Việt. */
    private static String buildDayLabel(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:    return "Thứ 2";
            case TUESDAY:   return "Thứ 3";
            case WEDNESDAY: return "Thứ 4";
            case THURSDAY:  return "Thứ 5";
            case FRIDAY:    return "Thứ 6";
            case SATURDAY:  return "Thứ 7";
            default:        return "Chủ nhật";
        }
    }

    public LocalDate getDate() { return date; }
    public String getDayLabel() { return dayLabel; }
    public boolean isToday() { return today; }
}
