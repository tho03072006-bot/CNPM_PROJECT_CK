package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.ConcessionStockMovement;
import edu.hcmute.cnpm.cinema.entity.StockMovementType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConcessionStockMovementRepository extends JpaRepository<ConcessionStockMovement, Long> {

    @EntityGraph(attributePaths = {"product"})
    List<ConcessionStockMovement> findTop20ByOrderByCreatedAtDescIdDesc();

    List<ConcessionStockMovement> findTop50ByProductIdOrderByCreatedAtDescIdDesc(Long productId);

    /** Tổng số phần đã giảm vì một lý do kể từ một thời điểm, ví dụ số phần bán từ đầu ngày. */
    @Query("select coalesce(sum(-m.quantityChange), 0) from ConcessionStockMovement m "
            + "where m.type = :type and m.createdAt >= :from")
    long sumOutgoingSince(@Param("type") StockMovementType type, @Param("from") LocalDateTime from);
}
