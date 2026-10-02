package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.booking.HoldSeatsRequest;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Set;

@Service
public class BookingConsentPolicy {
    public String normalize(String rating) {
        String value = rating == null ? "" : rating.trim().toUpperCase(Locale.ROOT);
        if (value.matches("C(13|16|18)")) value = "T" + value.substring(1);
        if (!Set.of("P", "K", "T13", "T16", "T18", "C").contains(value))
            throw new InvalidBookingException("Phim chưa có phân loại độ tuổi hợp lệ. Vui lòng liên hệ rạp.");
        if ("C".equals(value)) throw new InvalidBookingException("Phim này không được phép phổ biến.");
        return value;
    }
    public String message(String rating) {
        return switch (normalize(rating)) {
            case "P" -> "Phim dành cho mọi độ tuổi.";
            case "K" -> "Người xem dưới 13 tuổi phải đi cùng cha, mẹ hoặc người giám hộ.";
            default -> "Tất cả người xem phải từ " + normalize(rating).substring(1)
                    + " tuổi. Vui lòng mang giấy tờ xác minh độ tuổi khi vào rạp.";
        };
    }
    public void validate(String rating, HoldSeatsRequest request) {
        String normalized = normalize(rating);
        if (!request.isTermsAccepted())
            throw new InvalidBookingException("Vui lòng đồng ý quy định đặt vé trước khi giữ ghế.");
        if (!"P".equals(normalized) && !request.isAgeConfirmed())
            throw new InvalidBookingException("Vui lòng xác nhận điều kiện độ tuổi của tất cả người xem.");
    }
}
