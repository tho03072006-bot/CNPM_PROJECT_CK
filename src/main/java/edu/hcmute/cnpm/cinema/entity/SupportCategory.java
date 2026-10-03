package edu.hcmute.cnpm.cinema.entity;

public enum SupportCategory {
    BOOKING("Đặt vé và chọn ghế"),
    PAYMENT("Thanh toán và hóa đơn"),
    REFUND("Hủy vé và hoàn tiền"),
    SERVICE("Dịch vụ tại rạp"),
    OTHER("Nội dung khác");

    private final String label;

    SupportCategory(String label) { this.label = label; }
    public String getLabel() { return label; }
}
