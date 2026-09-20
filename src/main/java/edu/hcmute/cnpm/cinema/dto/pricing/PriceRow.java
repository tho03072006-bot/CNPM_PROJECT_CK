package edu.hcmute.cnpm.cinema.dto.pricing;

import java.math.BigDecimal;
import java.util.List;

/**
 * Một dòng của bảng giá vé: giá ghế thường của một loại phòng, trong một nhóm ngày.
 *
 * {@code pricesInSchedule} là các mức giá ghế thường ĐANG thật sự nằm trong lịch
 * chiếu sắp tới của loại phòng này, đọc thẳng từ database. Nhờ vậy nếu giá công
 * bố và giá thực tế lệch nhau thì nhìn vào trang là thấy, không phải đi dò.
 */
public class PriceRow {

    private final String roomType;
    private final String dayGroup;
    private final BigDecimal price;
    private final boolean highlighted;
    private final List<BigDecimal> pricesInSchedule;

    public PriceRow(String roomType, String dayGroup, BigDecimal price,
                    boolean highlighted, List<BigDecimal> pricesInSchedule) {
        this.roomType = roomType;
        this.dayGroup = dayGroup;
        this.price = price;
        this.highlighted = highlighted;
        this.pricesInSchedule = pricesInSchedule;
    }

    public String getRoomType() { return roomType; }
    public String getDayGroup() { return dayGroup; }
    public BigDecimal getPrice() { return price; }

    /** Dòng cần làm nổi bật trên giao diện, ví dụ ngày có giá ưu đãi. */
    public boolean isHighlighted() { return highlighted; }

    public List<BigDecimal> getPricesInSchedule() { return pricesInSchedule; }

    /** Giá công bố có khớp với giá đang dùng trong lịch chiếu hay không. */
    public boolean isMatchingSchedule() {
        if (pricesInSchedule.isEmpty()) {
            return true;
        }
        for (BigDecimal inSchedule : pricesInSchedule) {
            if (inSchedule.compareTo(price) == 0) {
                return true;
            }
        }
        return false;
    }

    /** Nhóm ngày này có suất chiếu nào trong lịch sắp tới không. */
    public boolean isPresentInSchedule() {
        return !pricesInSchedule.isEmpty();
    }

    /**
     * Các mức giá đang có trong lịch, viết sẵn kiểu "115.000, 79.000".
     *
     * Định dạng ở đây thay vì để Thymeleaf ghép chuỗi, vì ghép thẳng danh sách
     * {@code BigDecimal} sẽ ra "115000" không có dấu chấm ngăn nghìn.
     */
    public String getPricesInScheduleText() {
        StringBuilder text = new StringBuilder();
        for (BigDecimal price : pricesInSchedule) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(formatThousands(price));
        }
        return text.toString();
    }

    private static String formatThousands(BigDecimal price) {
        String digits = price.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
        StringBuilder grouped = new StringBuilder();
        int countFromRight = 0;
        for (int index = digits.length() - 1; index >= 0; index--) {
            grouped.append(digits.charAt(index));
            countFromRight++;
            if (countFromRight % 3 == 0 && index > 0) {
                grouped.append('.');
            }
        }
        return grouped.reverse().toString();
    }
}
