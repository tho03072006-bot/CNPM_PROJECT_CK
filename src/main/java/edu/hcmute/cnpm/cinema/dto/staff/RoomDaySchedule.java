package edu.hcmute.cnpm.cinema.dto.staff;

import edu.hcmute.cnpm.cinema.entity.RoomType;

import java.util.List;

/** Lịch trong ngày của một phòng: các suất xếp theo giờ bắt đầu. */
public record RoomDaySchedule(Long roomId, String roomName, RoomType roomType, long seatCount,
                              List<DailyShowtimeEntry> entries) {
    public RoomDaySchedule {
        entries = List.copyOf(entries);
    }

    /** Số chỗ khoảng nghỉ giữa hai suất ngắn hơn quy định trong phòng này. */
    public long getShortBreakCount() {
        return entries.stream().filter(DailyShowtimeEntry::isBreakTooShort).count();
    }
}
