package edu.hcmute.cnpm.cinema.dto.receipt;
/** Danh sách ghế và mã dùng chung một QR của hóa đơn. */
public record ReceiptTicket(String seatLabel, String code, String state) { }
