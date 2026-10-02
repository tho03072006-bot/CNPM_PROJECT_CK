package edu.hcmute.cnpm.cinema.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Sản phẩm bán kèm tại quầy bắp nước.
 *
 * Món lẻ (bắp, nước) có tồn kho riêng. Combo không có kho riêng: combo được ghép từ các món
 * lẻ trong {@link #components}, bán một combo là trừ kho từng món thành phần.
 */
@Entity
@Table(name = "concession_products")
public class ConcessionProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Nationalized
    @Column(nullable = false, length = 150)
    private String name;

    @Nationalized
    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Nationalized
    @Column(nullable = false, length = 12)
    private String icon = "🍿";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    // @ColumnDefault để ddl-auto=update thêm được cột NOT NULL vào bảng đã có dữ liệu.
    @ColumnDefault("0")
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    /** Tồn kho từ mức này trở xuống thì báo "sắp hết" cho nhân viên. */
    @ColumnDefault("10")
    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 10;

    @OneToMany(mappedBy = "combo")
    @OrderBy("id ASC")
    private List<ConcessionComboItem> components = new ArrayList<>();

    public ConcessionProduct() {}

    /** Combo là sản phẩm ghép từ món lẻ, không tự có tồn kho. */
    public boolean isCombo() { return !components.isEmpty(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public List<ConcessionComboItem> getComponents() { return components; }
    public void setComponents(List<ConcessionComboItem> components) { this.components = components; }
}
