package edu.hcmute.cnpm.cinema.dto.stats;

import java.math.BigDecimal;

/**
 * Một dòng của bảng thống kê: doanh thu và số vé của một nhóm.
 *
 * Nhóm ở đây có thể là một ngày ("21/09/2026") hoặc một bộ phim, tuỳ bảng đang
 * hiển thị - dùng chung một kiểu nên giao diện chỉ cần một đoạn lặp.
 */
public class RevenueRow {

    private final String label;
    private final long ticketCount;
    private final BigDecimal revenue;

    public RevenueRow(String label, long ticketCount, BigDecimal revenue) {
        this.label = label;
        this.ticketCount = ticketCount;
        this.revenue = revenue;
    }

    public String getLabel() { return label; }
    public long getTicketCount() { return ticketCount; }
    public BigDecimal getRevenue() { return revenue; }
}
