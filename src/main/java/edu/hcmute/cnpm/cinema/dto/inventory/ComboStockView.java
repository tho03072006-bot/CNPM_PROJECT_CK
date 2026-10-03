package edu.hcmute.cnpm.cinema.dto.inventory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Một combo và số combo còn ghép được từ kho món lẻ.
 *
 * @param available số combo tối đa còn bán được, tính theo món thành phần ít nhất
 * @param recipe    từng dòng công thức, ví dụ "2 × Nước ngọt cỡ vừa"
 */
public record ComboStockView(Long id, String name, String icon, BigDecimal price, boolean active,
                             int available, StockLevel level, List<String> recipe) {
}
