package edu.hcmute.cnpm.cinema.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Mã ưu đãi và điều kiện sử dụng được lưu trong database. */
@Entity
@Table(name = "vouchers", uniqueConstraints = @UniqueConstraint(name = "uq_vouchers_code", columnNames = "code"))
@org.hibernate.annotations.Check(name = "ck_vouchers_rules", constraints = "discount_percent BETWEEN 0 AND 99 AND discount_amount >= 0 AND minimum_ticket_subtotal >= 0 AND maximum_discount > 0 AND ((discount_percent > 0 AND discount_amount = 0) OR (discount_percent = 0 AND discount_amount > 0)) AND (starts_on IS NULL OR ends_on IS NULL OR ends_on >= starts_on)")
public class Voucher {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Pattern(regexp = "[A-Z0-9_-]{1,40}")
    @Column(nullable = false, length = 40)
    private String code;

    @NotBlank @Nationalized
    @Column(nullable = false, length = 200)
    private String title;

    @Min(0) @Max(99)
    @Column(name = "discount_percent", nullable = false)
    private int percent;

    @NotNull @DecimalMin("0")
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @NotNull @DecimalMin("0")
    @Column(name = "minimum_ticket_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimum = BigDecimal.ZERO;

    @NotNull @DecimalMin(value = "0", inclusive = false)
    @Column(name = "maximum_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal maximum;

    @Column(name = "starts_on")
    private LocalDate startsOn;
    @Column(name = "ends_on")
    private LocalDate endsOn;
    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getPercent() { return percent; }
    public void setPercent(int percent) { this.percent = percent; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getMinimum() { return minimum; }
    public void setMinimum(BigDecimal minimum) { this.minimum = minimum; }
    public BigDecimal getMaximum() { return maximum; }
    public void setMaximum(BigDecimal maximum) { this.maximum = maximum; }
    public LocalDate getStartsOn() { return startsOn; }
    public void setStartsOn(LocalDate startsOn) { this.startsOn = startsOn; }
    public LocalDate getEndsOn() { return endsOn; }
    public void setEndsOn(LocalDate endsOn) { this.endsOn = endsOn; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
