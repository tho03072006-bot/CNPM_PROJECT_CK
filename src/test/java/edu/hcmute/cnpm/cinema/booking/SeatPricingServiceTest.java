package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.service.SeatPricingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeatPricingServiceTest {
    private final SeatPricingService seatPricingService = new SeatPricingService();

    @ParameterizedTest
    @CsvSource({"NORMAL,75000.00", "VIP,112500.00", "COUPLE,150000.00"})
    @DisplayName("Giá ghế thường, VIP và ghế đôi đúng quy định")
    void shouldCalculateSeatPrice_whenSeatTypeIsSupported(String seatType, String expectedPrice) {
        assertThat(seatPricingService.calculateSeatPrice(new BigDecimal("75000.00"), seatType))
                .isEqualByComparingTo(expectedPrice);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    @DisplayName("Không chấp nhận giá gốc bằng không hoặc âm")
    void shouldRejectPrice_whenBasePriceIsNotPositive(String basePrice) {
        assertThatThrownBy(() -> seatPricingService.calculateSeatPrice(new BigDecimal(basePrice), "NORMAL"))
                .isInstanceOf(InvalidBookingException.class);
    }

    @Test
    @DisplayName("Không chấp nhận suất chiếu chưa có giá")
    void shouldRejectPrice_whenBasePriceIsMissing() {
        assertThatThrownBy(() -> seatPricingService.calculateSeatPrice(null, "NORMAL"))
                .isInstanceOf(InvalidBookingException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"OTHER", "vip"})
    @DisplayName("Không chấp nhận loại ghế thiếu hoặc ngoài danh sách quy định")
    void shouldRejectSeatType_whenSeatTypeIsInvalid(String seatType) {
        assertThatThrownBy(() -> seatPricingService.calculateSeatPrice(BigDecimal.TEN, seatType))
                .isInstanceOf(InvalidBookingException.class);
    }

    @Test
    @DisplayName("Giá sau nhân được làm tròn đến hai chữ số thập phân")
    void shouldRoundPrice_whenMultiplicationCreatesFractionalCents() {
        assertThat(seatPricingService.calculateSeatPrice(new BigDecimal("10.01"), "VIP"))
                .isEqualByComparingTo("15.02");
    }
}
