package edu.hcmute.cnpm.cinema.entity;

public enum TicketStatus {
    HELD,       // dang giu ghe, cho thanh toan (het han sau app.seat-hold-minutes)
    PAID,       // da thanh toan thanh cong
    CANCELLED,  // nguoi dung tu huy
    EXPIRED     // qua thoi gian giu ghe ma chua thanh toan
}
