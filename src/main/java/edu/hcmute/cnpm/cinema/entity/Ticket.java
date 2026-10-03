package edu.hcmute.cnpm.cinema.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// LUU Y QUAN TRONG (ADR-1): unique constraint (showtime_id, seat_id) o database
// la co che chinh de chong 2 nguoi dat trung 1 ghe cung suat chieu.
// Khi INSERT ma vi pham constraint nay -> bat DataIntegrityViolationException
// va bao "ghe da co nguoi giu/dat".
@Entity
@Table(name = "tickets", uniqueConstraints = {
        @UniqueConstraint(name = "uq_showtime_seat", columnNames = {"showtime_id", "seat_id"})
})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Đơn hàng gom các vé trong cùng lượt với bắp nước và hóa đơn. Cho phép null để
    // những vé cũ tạo trước khi có chức năng hóa đơn vẫn đọc được bình thường.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_order_id")
    private BookingOrder bookingOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.HELD;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "held_at", nullable = false)
    private LocalDateTime heldAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Trả bằng cách nào. Vé thanh toán trước ngày 27/09/2026 để trống, coi như trả tại quầy.
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    // Mã giao dịch bên cổng thanh toán (transId của MoMo). Cần để hoàn tiền về đúng giao dịch.
    @Nationalized
    @Column(name = "payment_ref", length = 100)
    private String paymentRef;

    // Lúc nhân viên soát vé cho khách vào phòng. Khác null nghĩa là vé đã dùng rồi.
    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Transient
    private String admissionCode;
    public String getAdmissionCode() { return admissionCode; }
    public void setAdmissionCode(String admissionCode) { this.admissionCode = admissionCode; }

    public Ticket() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Showtime getShowtime() { return showtime; }
    public void setShowtime(Showtime showtime) { this.showtime = showtime; }
    public Seat getSeat() { return seat; }
    public void setSeat(Seat seat) { this.seat = seat; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BookingOrder getBookingOrder() { return bookingOrder; }
    public void setBookingOrder(BookingOrder bookingOrder) { this.bookingOrder = bookingOrder; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice == null ? price : originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public LocalDateTime getHeldAt() { return heldAt; }
    public void setHeldAt(LocalDateTime heldAt) { this.heldAt = heldAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
}
