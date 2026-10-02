package edu.hcmute.cnpm.cinema.dto.inventory;

import java.util.List;

/**
 * Số liệu của trang kho bắp nước.
 *
 * @param lowCount   số món lẻ sắp hết (chưa hết hẳn)
 * @param outCount   số món lẻ đã hết
 * @param soldToday  tổng số phần món lẻ bán ra từ đầu ngày, combo đã quy về món lẻ
 */
public record InventoryOverview(List<StockItemView> items, List<ComboStockView> combos,
                                long lowCount, long outCount, long soldToday) {
}
