package edu.hcmute.cnpm.cinema.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/**
 * Một lần tồn kho của món lẻ thay đổi: nhập, hủy, kiểm kê hoặc bán.
 *
 * Bảng này chỉ thêm, không sửa, nên tra lại được ai đã đổi kho lúc nào và vì sao.
 * Tên người thực hiện được chụp lại để lịch sử không đổi khi tài khoản đổi tên.
 */
@Entity
@Table(name = "concession_stock_movements")
public class ConcessionStockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ConcessionProduct product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockMovementType type;

    /** Số lượng tăng (dương) hoặc giảm (âm). */
    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;

    @Nationalized
    @Column(length = 255)
    private String note;

    /** Mã hóa đơn, chỉ có ở lần bán hàng. */
    @Column(length = 40)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Nationalized
    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ConcessionStockMovement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ConcessionProduct getProduct() { return product; }
    public void setProduct(ConcessionProduct product) { this.product = product; }
    public StockMovementType getType() { return type; }
    public void setType(StockMovementType type) { this.type = type; }
    public int getQuantityChange() { return quantityChange; }
    public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange; }
    public int getQuantityAfter() { return quantityAfter; }
    public void setQuantityAfter(int quantityAfter) { this.quantityAfter = quantityAfter; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
