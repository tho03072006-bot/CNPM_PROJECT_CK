package edu.hcmute.cnpm.cinema.util;

import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Định dạng tiền Việt Nam cho giao diện, thông báo và email; không đổi giá trị lưu trữ. */
@Component("money")
public final class MoneyFormatter {
    /** Dùng trong Thymeleaf khi đơn vị đ/đồng đã có sẵn trong câu. */
    public String number(Number amount) {
        return digits(amount);
    }

    public static String format(Number amount) {
        return digits(amount) + " đ";
    }

    public static String digits(Number amount) {
        // DecimalFormat không an toàn khi dùng chung giữa các luồng: tạo mới mỗi lần.
        DecimalFormat formatter = new DecimalFormat("#,##0",
                DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")));
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter.format(amount == null ? 0 : amount);
    }
}
