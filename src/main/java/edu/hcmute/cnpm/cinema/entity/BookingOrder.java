package edu.hcmute.cnpm.cinema.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Đơn hàng gom vé và bắp nước để thanh toán và xuất hóa đơn một lần. */
@Entity
@Table(name = "booking_orders")
public class BookingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_code", nullable = false, unique = true, length = 40)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @Nationalized
    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 150)
    private String customerEmail;

    @Nationalized
    @Column(name = "movie_title", nullable = false, length = 200)
    private String movieTitle;

    @Nationalized
    @Column(name = "room_name", nullable = false, length = 50)
    private String roomName;

    @Column(name = "showtime_start", nullable = false)
    private LocalDateTime showtimeStart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingOrderStatus status = BookingOrderStatus.DRAFT;

    @Column(name = "ticket_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal ticketSubtotal = BigDecimal.ZERO;

    @Column(name = "concession_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal concessionSubtotal = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "voucher_code", length = 40)
    private String voucherCode;

    @org.hibernate.annotations.ColumnDefault("0")
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_ref", length = 100)
    private String paymentRef;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "bookingOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<BookingOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "bookingOrder")
    @OrderBy("id ASC")
    private List<Ticket> tickets = new ArrayList<>();

    public BookingOrder() {}

    public void replaceItems(List<BookingOrderItem> replacement) {
        items.clear();
        for (BookingOrderItem item : replacement) {
            item.setBookingOrder(this);
            items.add(item);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReceiptCode() { return receiptCode; }
    public void setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Showtime getShowtime() { return showtime; }
    public void setShowtime(Showtime showtime) { this.showtime = showtime; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public LocalDateTime getShowtimeStart() { return showtimeStart; }
    public void setShowtimeStart(LocalDateTime showtimeStart) { this.showtimeStart = showtimeStart; }
    public BookingOrderStatus getStatus() { return status; }
    public void setStatus(BookingOrderStatus status) { this.status = status; }
    public BigDecimal getTicketSubtotal() { return ticketSubtotal; }
    public void setTicketSubtotal(BigDecimal ticketSubtotal) { this.ticketSubtotal = ticketSubtotal; }
    public BigDecimal getConcessionSubtotal() { return concessionSubtotal; }
    public void setConcessionSubtotal(BigDecimal concessionSubtotal) { this.concessionSubtotal = concessionSubtotal; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public List<BookingOrderItem> getItems() { return items; }
    public void setItems(List<BookingOrderItem> items) { this.items = items; }
    public List<Ticket> getTickets() { return tickets; }
    public void setTickets(List<Ticket> tickets) { this.tickets = tickets; }
}
