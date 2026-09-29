package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    // Dùng để vẽ seat-map: lấy các ghế đã bị giữ/đặt cho một suất chiếu.
    List<Ticket> findByShowtimeIdAndStatusIn(Long showtimeId, List<TicketStatus> statuses);

    boolean existsByShowtimeId(Long showtimeId);

    /** Đếm ghế đang bị chiếm cho nhiều suất trong một truy vấn, tránh N+1 ở trang lịch chiếu. */
    @Query("""
            select ticket.showtime.id, count(ticket.id)
            from Ticket ticket
            where ticket.showtime.id in :showtimeIds
            group by ticket.showtime.id
            """)
    List<Object[]> countReservedSeatsByShowtimeIds(@Param("showtimeIds") Collection<Long> showtimeIds);

    // ===== Module 3: thanh toán, lịch sử vé, thống kê =====

    /** Vé đang giữ của một khách cho một suất chiếu - dùng ở bước thanh toán. */
    List<Ticket> findByUserIdAndShowtimeIdAndStatus(Long userId, Long showtimeId, TicketStatus status);

    /** Toàn bộ vé của một khách, mới nhất lên đầu - dùng cho trang lịch sử đặt vé. */
    List<Ticket> findByUserIdOrderByHeldAtDesc(Long userId);

    /** Vé đã thanh toán trong một khoảng thời gian - dùng cho trang thống kê doanh thu. */
    List<Ticket> findByStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(
            TicketStatus status, LocalDateTime from, LocalDateTime until);

    /** Vé đang giữ đã đến hạn - dùng cho tác vụ dọn vé hết hạn của Module 2 (ADR-2). */
    List<Ticket> findByStatusAndHeldAtLessThanEqual(TicketStatus status, LocalDateTime heldBefore);
}
