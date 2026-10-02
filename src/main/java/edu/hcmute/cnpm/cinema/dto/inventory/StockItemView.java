package edu.hcmute.cnpm.cinema.dto.inventory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Một món lẻ có tồn kho riêng (bắp, nước...).
 *
 * @param usedInCombos tên các combo có dùng món này, để nhân viên biết hết món này thì combo nào ngừng bán theo
 */
public record StockItemView(Long id, String code, String name, String icon, BigDecimal price, boolean active,
                            int stock, int lowStockThreshold, StockLevel level, List<String> usedInCombos) {
}
