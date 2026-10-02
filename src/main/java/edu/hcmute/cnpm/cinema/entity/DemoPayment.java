package edu.hcmute.cnpm.cinema.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;

/** © Nhóm 8. Giao dịch mô phỏng, không đại diện cho tiền thật hoặc hệ thống MoMo. */
@Entity
@Table(name = "demo_payments", indexes = @Index(name = "ix_demo_payments_owner", columnList = "user_id,showtime_id,status"))
public class DemoPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "ticket_ids", nullable = false, length = 200)
    private String ticketIds;

    @Nationalized @Column(name = "seat_labels", nullable = false, length = 100)
    private String seatLabels;

    @Nationalized @Column(name = "movie_title", nullable = false, length = 200)
    private String movieTitle;

    @Nationalized @Column(name = "room_name", nullable = false, length = 50)
    private String roomName;

    @Column(name = "showtime_start", nullable = false)
    private LocalDateTime showtimeStart;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private DemoPaymentStatus status = DemoPaymentStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public DemoPayment() {}
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String value) { publicId = value; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String value) { tokenHash = value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public Long getShowtimeId() { return showtimeId; }
    public void setShowtimeId(Long value) { showtimeId = value; }
    public String getTicketIds() { return ticketIds; }
    public void setTicketIds(String value) { ticketIds = value; }
    public String getSeatLabels() { return seatLabels; }
    public void setSeatLabels(String value) { seatLabels = value; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String value) { movieTitle = value; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String value) { roomName = value; }
    public LocalDateTime getShowtimeStart() { return showtimeStart; }
    public void setShowtimeStart(LocalDateTime value) { showtimeStart = value; }
    public Long getAmount() { return amount; }
    public void setAmount(Long value) { amount = value; }
    public DemoPaymentStatus getStatus() { return status; }
    public void setStatus(DemoPaymentStatus value) { status = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime value) { expiresAt = value; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime value) { paidAt = value; }
}
