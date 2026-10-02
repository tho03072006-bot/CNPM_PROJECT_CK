package edu.hcmute.cnpm.cinema.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

/** Một dòng bắp nước trên đơn hàng; tên và giá được chụp lại tại thời điểm mua. */
@Entity
@Table(name = "booking_order_items")
public class BookingOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_order_id", nullable = false)
    private BookingOrder bookingOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ConcessionProduct product;

    @Nationalized
    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    public BookingOrderItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BookingOrder getBookingOrder() { return bookingOrder; }
    public void setBookingOrder(BookingOrder bookingOrder) { this.bookingOrder = bookingOrder; }
    public ConcessionProduct getProduct() { return product; }
    public void setProduct(ConcessionProduct product) { this.product = product; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
