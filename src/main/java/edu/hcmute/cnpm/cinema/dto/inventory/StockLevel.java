package edu.hcmute.cnpm.cinema.dto.inventory;

/** Mức tồn kho để tô màu nhãn trạng thái trên trang kho. */
public enum StockLevel {
    IN_STOCK("Còn hàng", "badge-ok"),
    LOW("Sắp hết", "badge-accent"),
    OUT("Hết hàng", "badge-danger");

    private final String label;
    private final String badgeClass;

    StockLevel(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() { return label; }
    public String getBadgeClass() { return badgeClass; }

    /** Bằng ngưỡng vẫn tính là sắp hết, để nhân viên kịp nhập trước khi hết hẳn. */
    public static StockLevel of(int available, int lowStockThreshold) {
        if (available <= 0) {
            return OUT;
        }
        return available <= lowStockThreshold ? LOW : IN_STOCK;
    }
}
