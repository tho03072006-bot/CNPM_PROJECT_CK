package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.ConcessionProduct;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConcessionProductRepository extends JpaRepository<ConcessionProduct, Long> {
    List<ConcessionProduct> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    /** Mọi sản phẩm kèm công thức combo, cho trang kho và để tính số phần còn bán được. */
    @EntityGraph(attributePaths = {"components", "components.component"})
    List<ConcessionProduct> findAllByOrderByDisplayOrderAscNameAsc();

    /** Khóa dòng sản phẩm khi nhân viên sửa kho, tránh hai người cùng sửa đè số của nhau. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ConcessionProduct p where p.id = :id")
    Optional<ConcessionProduct> findByIdForStockUpdate(@Param("id") Long id);

    /**
     * Trừ kho trong một câu lệnh: chỉ trừ khi còn đủ, trả về 0 nếu không đủ.
     * Hai khách thanh toán cùng lúc không thể cùng lấy phần cuối cùng.
     */
    @Modifying(flushAutomatically = true)
    @Query("update ConcessionProduct p set p.stockQuantity = p.stockQuantity - :quantity "
            + "where p.id = :id and p.stockQuantity >= :quantity")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Query("select p.stockQuantity from ConcessionProduct p where p.id = :id")
    int findStockQuantity(@Param("id") Long id);

    /** Số món lẻ đang bán (hoặc nằm trong combo) đã chạm ngưỡng sắp hết. */
    @Query("select count(p) from ConcessionProduct p where p.components is empty "
            + "and p.stockQuantity <= p.lowStockThreshold "
            + "and (p.active = true or exists (select c.id from ConcessionComboItem c where c.component = p))")
    long countLowStockItems();
}
