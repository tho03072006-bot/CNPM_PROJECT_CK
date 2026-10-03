package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.BookingOrder;
import edu.hcmute.cnpm.cinema.entity.BookingOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;
import java.util.Optional;

/** Truy vấn riêng của lịch sử; không thay repository thanh toán mà thành viên khác đang sửa. */
public interface BookingHistoryRepository extends Repository<BookingOrder, Long> {
    // Không fetch collection khi phân trang để SQL giới hạn đúng 12 giao dịch.
    @EntityGraph(attributePaths = {"showtime.movie"})
    Page<BookingOrder> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"showtime.movie"})
    Page<BookingOrder> findByUserIdAndStatus(Long userId, BookingOrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"showtime.movie", "user"})
    Optional<BookingOrder> findByReceiptCodeAndUserId(String receiptCode, Long userId);
}
