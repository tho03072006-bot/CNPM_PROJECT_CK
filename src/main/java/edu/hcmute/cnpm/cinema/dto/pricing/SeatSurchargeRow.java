package edu.hcmute.cnpm.cinema.dto.pricing;

import java.math.BigDecimal;

/**
 * Một dòng của bảng phụ thu theo loại ghế.
 *
 * {@code examplePrice} là giá của chính loại ghế đó khi áp lên một mức giá gốc
 * cụ thể, để khách không phải tự nhân nhẩm.
 */
public class SeatSurchargeRow {

    private final String seatTypeLabel;
    private final String description;
    private final BigDecimal multiplier;
    private final BigDecimal examplePrice;

    public SeatSurchargeRow(String seatTypeLabel, String description,
                            BigDecimal multiplier, BigDecimal examplePrice) {
        this.seatTypeLabel = seatTypeLabel;
        this.description = description;
        this.multiplier = multiplier;
        this.examplePrice = examplePrice;
    }

    public String getSeatTypeLabel() { return seatTypeLabel; }
    public String getDescription() { return description; }
    public BigDecimal getMultiplier() { return multiplier; }
    public BigDecimal getExamplePrice() { return examplePrice; }
}
