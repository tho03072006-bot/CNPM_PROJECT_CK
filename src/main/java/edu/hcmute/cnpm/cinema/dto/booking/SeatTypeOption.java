package edu.hcmute.cnpm.cinema.dto.booking;

import java.math.BigDecimal;

/**
 * Một loại ghế trong chú thích của sơ đồ ghế.
 *
 * @param code        NORMAL, VIP hoặc COUPLE
 * @param label       tên hiện cho khách
 * @param cssModifier hậu tố class CSS của loại ghế
 * @param price       giá một ghế loại này ở suất chiếu đang xem
 * @param capacity    số người ngồi được (ghế đôi là 2)
 * @param seatCount   số ghế loại này trong phòng
 */
public record SeatTypeOption(String code, String label, String cssModifier,
                             BigDecimal price, int capacity, int seatCount) {
}
