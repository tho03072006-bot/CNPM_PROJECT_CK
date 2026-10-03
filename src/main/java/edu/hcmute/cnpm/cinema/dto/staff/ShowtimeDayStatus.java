package edu.hcmute.cnpm.cinema.dto.staff;

/**
 * Suất chiếu đang ở bước nào, để nhân viên biết lúc nào mở cửa đón khách và lúc nào dọn phòng.
 */
public enum ShowtimeDayStatus {
    UPCOMING("Sắp chiếu", "upcoming"),
    /** Trong khoảng mở cửa trước giờ chiếu: soát vé, đưa khách vào phòng. */
    BOARDING("Mở cửa đón khách", "boarding"),
    SHOWING("Đang chiếu", "showing"),
    /** Hết phim, chưa hết khoảng nghỉ: khách ra về, nhân viên dọn phòng cho suất sau. */
    CLEANING("Khách ra, dọn phòng", "cleaning"),
    FINISHED("Đã xong", "finished");

    private final String label;
    private final String cssModifier;

    ShowtimeDayStatus(String label, String cssModifier) {
        this.label = label;
        this.cssModifier = cssModifier;
    }

    public String getLabel() { return label; }

    /** Hậu tố class CSS: {@code day-status-showing}... */
    public String getCssModifier() { return cssModifier; }
}
