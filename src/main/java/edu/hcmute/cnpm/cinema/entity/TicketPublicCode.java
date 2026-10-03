package edu.hcmute.cnpm.cinema.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import java.time.LocalDateTime;

/** Giữ mã vĩnh viễn ngay cả khi vé đã hủy; không có FK xóa theo vé. */
@Entity
@Table(name = "ticket_public_codes", uniqueConstraints =
        @UniqueConstraint(name = "uq_ticket_public_code", columnNames = "public_code"))
@Check(name = "ck_ticket_public_code_format", constraints =
        "len(public_code) = 8 and public_code collate Latin1_General_100_BIN2 like '[1-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]'")
public class TicketPublicCode {
    @Id @Column(name = "ticket_id")
    private Long ticketId;
    @Column(name = "public_code", nullable = false, length = 8)
    private String publicCode;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    public TicketPublicCode() { }
    public TicketPublicCode(Long ticketId, String publicCode, LocalDateTime createdAt) {
        this.ticketId = ticketId; this.publicCode = publicCode; this.createdAt = createdAt;
    }
    public Long getTicketId() { return ticketId; }
    public String getPublicCode() { return publicCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
