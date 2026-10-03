package edu.hcmute.cnpm.cinema.dto.staff;

import java.time.LocalDateTime;

/**
 * Một suất trên bảng suất chiếu trong ngày.
 *
 * @param movieEnd        giờ hết phim dự kiến (giờ bắt đầu + thời lượng phim)
 * @param roomReadyAt     giờ phòng sẵn sàng cho suất sau (hết phim + khoảng nghỉ)
 * @param minutesToNext   số phút từ lúc hết phim tới giờ bắt đầu suất kế tiếp cùng phòng; null nếu là suất cuối
 * @param isBreakTooShort khoảng nghỉ tới suất sau ngắn hơn quy định (dữ liệu cũ, xếp trước khi có quy tắc)
 * @param timelineLeft    vị trí bắt đầu trên thanh thời gian, tính bằng % (chuỗi dạng 12.500 cho CSS)
 * @param timelineMovie   độ dài phần chiếu phim trên thanh thời gian, %
 * @param timelineBreak   độ dài khoảng nghỉ trên thanh thời gian, %
 */
public record DailyShowtimeEntry(Long showtimeId, String movieTitle, String ageRating, int durationMin,
                                 LocalDateTime start, LocalDateTime movieEnd, LocalDateTime roomReadyAt,
                                 ShowtimeDayStatus status, long soldSeats, long checkedIn, long seatCount,
                                 Long minutesToNext, boolean isBreakTooShort,
                                 String timelineLeft, String timelineMovie, String timelineBreak) {

    /** Khách vào phòng rồi mà còn ít hơn số vé đã bán: nhân viên cần chờ hoặc gọi thêm khách. */
    public long getWaitingGuests() {
        return Math.max(0, soldSeats - checkedIn);
    }

    /** Có nên hiện nút sang trang soát vé không: chỉ lúc đón khách hoặc đang chiếu. */
    public boolean isCheckInOpen() {
        return status == ShowtimeDayStatus.BOARDING || status == ShowtimeDayStatus.SHOWING;
    }
}
