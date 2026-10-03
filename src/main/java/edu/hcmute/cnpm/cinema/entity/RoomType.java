package edu.hcmute.cnpm.cinema.entity;

import java.util.Locale;

/**
 * Loại phòng chiếu, để khách phân biệt phòng thường với phòng cao cấp.
 *
 * KHÔNG lưu thành cột: loại phòng suy ra từ tên phòng theo quy ước của rạp, kiểu
 * "Cinema 7 - PREMIUM", "Cinema 8 - GOLD CLASS", còn phòng thường chỉ có số. Dữ liệu mẫu
 * trong {@code seed-data.sql} đặt tên đúng quy ước này, nên không phải đổi database.
 */
public enum RoomType {

    STANDARD("Phòng thường", "standard",
            "Phòng chiếu tiêu chuẩn. Giá vé đổi theo ngày trong tuần, Thứ Tư rẻ nhất."),
    PREMIUM("Premium", "premium",
            "Phòng cao cấp với ghế ngả. Một giá cho mọi ngày trong tuần."),
    GOLD("Gold Class", "gold",
            "Phòng hạng sang, ít ghế, ghế ngả rộng. Một giá cho mọi ngày trong tuần.");

    private final String label;
    private final String cssModifier;
    private final String description;

    RoomType(String label, String cssModifier, String description) {
        this.label = label;
        this.cssModifier = cssModifier;
        this.description = description;
    }

    /** Suy ra loại phòng từ tên phòng. Tên lạ hoặc để trống thì coi là phòng thường. */
    public static RoomType fromRoomName(String roomName) {
        String name = roomName == null ? "" : roomName.toUpperCase(Locale.ROOT);
        if (name.contains("GOLD CLASS")) {
            return GOLD;
        }
        if (name.contains("PREMIUM")) {
            return PREMIUM;
        }
        return STANDARD;
    }

    /** Tên hiện cho khách, ví dụ "Gold Class". */
    public String getLabel() { return label; }

    /** Hậu tố class CSS: {@code room-badge-standard}, {@code room-badge-premium}, {@code room-badge-gold}. */
    public String getCssModifier() { return cssModifier; }

    /** Một câu giới thiệu loại phòng, hiện ở trang chọn ghế. */
    public String getDescription() { return description; }
}
