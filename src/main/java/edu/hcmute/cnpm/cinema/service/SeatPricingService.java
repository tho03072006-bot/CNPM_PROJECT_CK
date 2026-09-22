package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Tính giá ghế của Module 2 bằng số thập phân, không nhận giá từ trình duyệt. */
@Service
public class SeatPricingService {

    public BigDecimal calculateSeatPrice(BigDecimal basePrice, String seatType) {
        if (basePrice == null || basePrice.signum() <= 0) {
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
        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
