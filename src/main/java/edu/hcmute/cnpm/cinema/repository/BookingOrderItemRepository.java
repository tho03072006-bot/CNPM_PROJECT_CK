package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.BookingOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingOrderItemRepository extends JpaRepository<BookingOrderItem, Long> {
}
