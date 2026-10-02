package edu.hcmute.cnpm.cinema.entity;

import jakarta.persistence.*;

/** Một món lẻ nằm trong combo, ví dụ "Combo đôi" gồm 2 phần "Nước ngọt cỡ vừa". */
@Entity
@Table(name = "concession_combo_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_combo_component",
                columnNames = {"combo_product_id", "component_product_id"}))
public class ConcessionComboItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_product_id", nullable = false)
    private ConcessionProduct combo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_product_id", nullable = false)
    private ConcessionProduct component;

    @Column(nullable = false)
    private int quantity = 1;

    public ConcessionComboItem() {}

    public ConcessionComboItem(ConcessionProduct combo, ConcessionProduct component, int quantity) {
        this.combo = combo;
        this.component = component;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ConcessionProduct getCombo() { return combo; }
    public void setCombo(ConcessionProduct combo) { this.combo = combo; }
    public ConcessionProduct getComponent() { return component; }
    public void setComponent(ConcessionProduct component) { this.component = component; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
