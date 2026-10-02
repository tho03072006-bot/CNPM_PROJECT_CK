package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Tính giá ghế của Module 2 bằng số thập phân, không nhận giá từ trình duyệt. */
@Service
public class SeatPricingService {
    // DECIMAL(10,2) must also fit the largest supported multiplier (COUPLE x2).
    public static final String MAX_BASE_PRICE = "49999999";

    public static boolean isValidBasePrice(BigDecimal price) {
        return price != null && price.signum() > 0 && price.stripTrailingZeros().scale() <= 0
                && price.compareTo(new BigDecimal(MAX_BASE_PRICE)) <= 0;
    }

    public BigDecimal calculateSeatPrice(BigDecimal basePrice, String seatType) {
        if (!isValidBasePrice(basePrice)) {
            throw new InvalidBookingException("Giá vé của suất chiếu chưa hợp lệ.");
        }
        if (seatType == null) {
            throw new InvalidBookingException("Loại ghế chưa hợp lệ.");
        }
        BigDecimal multiplier = switch (seatType) {
            case "NORMAL" -> new BigDecimal(Constants.SEAT_PRICE_MULTIPLIER_NORMAL);
            case "VIP" -> new BigDecimal(Constants.SEAT_PRICE_MULTIPLIER_VIP);
            case "COUPLE" -> new BigDecimal(Constants.SEAT_PRICE_MULTIPLIER_COUPLE);
            default -> throw new InvalidBookingException("Loại ghế chưa được hỗ trợ.");
        };
        // VND is charged in whole dong; round each seat before adding tickets/concessions.
        return basePrice.multiply(multiplier).setScale(0, RoundingMode.HALF_UP).setScale(2);
    }
}
