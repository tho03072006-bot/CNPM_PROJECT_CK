package edu.hcmute.cnpm.cinema.entity;

public enum SupportStatus {
    WAITING_STAFF("Chờ rạp trả lời"),
    WAITING_CUSTOMER("Chờ khách phản hồi"),
    RESOLVED("Đã giải quyết");

    private final String label;

    SupportStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
