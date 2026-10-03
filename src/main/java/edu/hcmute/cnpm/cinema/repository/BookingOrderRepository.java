package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.BookingOrder;
import edu.hcmute.cnpm.cinema.entity.BookingOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingOrderRepository extends JpaRepository<BookingOrder, Long> {
    @org.springframework.data.jpa.repository.Query("select o.showtime.id from BookingOrder o where o.receiptCode = :code")
    Optional<Long> findShowtimeIdByReceiptCode(@org.springframework.data.repository.query.Param("code") String code);

    @EntityGraph(attributePaths = {"items", "showtime", "user"})
    Optional<BookingOrder> findFirstByUserIdAndShowtimeIdAndStatusOrderByCreatedAtDesc(
            Long userId, Long showtimeId, BookingOrderStatus status);

    @EntityGraph(attributePaths = {"items", "showtime", "user"})
    Optional<BookingOrder> findByReceiptCode(String receiptCode);

    @org.springframework.data.jpa.repository.Query("""
            select count(o) from BookingOrder o where o.user.id = :userId and o.showtime.id = :showtimeId
            and o.status = edu.hcmute.cnpm.cinema.entity.BookingOrderStatus.PAID and o.paidAt = :paidAt
            and (o.paymentRef = :paymentRef or (o.paymentRef is null and :paymentRef is null))
            """)
    long countLegacyRefundMatches(@org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("showtimeId") Long showtimeId,
            @org.springframework.data.repository.query.Param("paidAt") java.time.LocalDateTime paidAt,
            @org.springframework.data.repository.query.Param("paymentRef") String paymentRef);

    @EntityGraph(attributePaths = {"items"})
    Optional<BookingOrder> findFirstByTicketsId(Long ticketId);
}
