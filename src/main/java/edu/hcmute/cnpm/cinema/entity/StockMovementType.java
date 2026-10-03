package edu.hcmute.cnpm.cinema.entity;

/** Lý do tồn kho bắp nước thay đổi. */
public enum StockMovementType {
    IMPORT("Nhập kho"),
    WRITE_OFF("Xuất hủy"),
    STOCKTAKE("Kiểm kê"),
    SALE("Bán cho khách");

    private final String label;

    StockMovementType(String label) { this.label = label; }
    public String getLabel() { return label; }

    /** Nhân viên được tự ghi các loại này; SALE chỉ do hệ thống ghi khi khách thanh toán. */
    public boolean isManual() { return this != SALE; }
}
