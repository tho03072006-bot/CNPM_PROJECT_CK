package edu.hcmute.cnpm.cinema.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Biên nhận hoàn tiền của một vé đã huỷ.
 *
 * Vì sao cần bảng riêng: huỷ vé đã thanh toán vẫn phải XOÁ dòng trong bảng tickets
 * (giống ADR-2), nếu không ràng buộc UNIQUE (showtime_id, seat_id) chặn người khác mua
 * lại ghế đó. Nhưng tiền thì đã thu rồi, không được biến mất khỏi sổ sách. Nên trước
 * khi xoá, chép lại mọi thứ cần cho kế toán vào đây.
 *
 * Chép cả tên phim, phòng, ghế, giờ chiếu (thay vì chỉ giữ khoá ngoại) vì đây là chứng
 * từ tiền bạc: sau này phim đổi tên hay suất bị xoá thì biên nhận vẫn phải đọc được.
 */
@Entity
@Table(name = "ticket_refunds")
public class TicketRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_ticket_id", nullable = false)
    private Long originalTicketId;

    /** Hóa đơn gốc, giữ dưới dạng snapshot kể cả khi đơn bị xóa về sau. */
    @Column(name = "booking_order_id")
    private Long bookingOrderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Nationalized
    @Column(name = "movie_title", nullable = false, length = 200)
    private String movieTitle;

    @Nationalized
    @Column(name = "room_name", nullable = false, length = 50)
    private String roomName;

    @Nationalized
    @Column(name = "seat_label", nullable = false, length = 10)
    private String seatLabel;

    @Column(name = "showtime_start", nullable = false)
    private LocalDateTime showtimeStart;

    @Column(name = "paid_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal paidPrice;

    @Column(name = "refund_percent", nullable = false)
    private Integer refundPercent;

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Nationalized
    @Column(name = "payment_ref", length = 100)
    private String paymentRef;

    /** Mã giao dịch hoàn tiền bên cổng thanh toán. Trống nếu hoàn tiền mặt tại quầy. */
    @Nationalized
    @Column(name = "refund_ref", length = 100)
    private String refundRef;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at", nullable = false)
    private LocalDateTime refundedAt;

    public TicketRefund() {}

    /** Số tiền rạp giữ lại sau khi hoàn: giá vé trừ tiền hoàn. */
    public BigDecimal getRetainedAmount() {
        return paidPrice.subtract(refundAmount);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOriginalTicketId() { return originalTicketId; }
    public void setOriginalTicketId(Long originalTicketId) { this.originalTicketId = originalTicketId; }
    public Long getBookingOrderId() { return bookingOrderId; }
    public void setBookingOrderId(Long id) { bookingOrderId = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getShowtimeId() { return showtimeId; }
    public void setShowtimeId(Long showtimeId) { this.showtimeId = showtimeId; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getSeatLabel() { return seatLabel; }
    public void setSeatLabel(String seatLabel) { this.seatLabel = seatLabel; }
    public LocalDateTime getShowtimeStart() { return showtimeStart; }
    public void setShowtimeStart(LocalDateTime showtimeStart) { this.showtimeStart = showtimeStart; }
    public BigDecimal getPaidPrice() { return paidPrice; }
    public void setPaidPrice(BigDecimal paidPrice) { this.paidPrice = paidPrice; }
    public Integer getRefundPercent() { return refundPercent; }
    public void setRefundPercent(Integer refundPercent) { this.refundPercent = refundPercent; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }
    public String getRefundRef() { return refundRef; }
    public void setRefundRef(String refundRef) { this.refundRef = refundRef; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
}
