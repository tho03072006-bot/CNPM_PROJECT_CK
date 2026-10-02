package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.BookingOrder;
import edu.hcmute.cnpm.cinema.entity.BookingOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingOrderRepository extends JpaRepository<BookingOrder, Long> {

    @EntityGraph(attributePaths = {"items", "showtime", "user"})
    Optional<BookingOrder> findFirstByUserIdAndShowtimeIdAndStatusOrderByCreatedAtDesc(
            Long userId, Long showtimeId, BookingOrderStatus status);

    @EntityGraph(attributePaths = {"items", "showtime", "user"})
    Optional<BookingOrder> findByReceiptCode(String receiptCode);

    @EntityGraph(attributePaths = {"items"})
    Optional<BookingOrder> findFirstByTicketsId(Long ticketId);
}
