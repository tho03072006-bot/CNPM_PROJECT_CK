package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    // Dùng để vẽ seat-map: lấy các ghế đã bị giữ/đặt cho một suất chiếu.
    List<Ticket> findByShowtimeIdAndStatusIn(Long showtimeId, List<TicketStatus> statuses);

    @Query("select t.showtime.id from Ticket t where t.id = :id")
    java.util.Optional<Long> findShowtimeIdByTicketId(@Param("id") Long id);

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

    @Query("select distinct t.showtime.id from Ticket t where t.status = :status "
            + "and (t.heldAt is null or t.heldAt <= :cutoff) order by t.showtime.id")
    List<Long> findExpiredShowtimeIds(@Param("status") TicketStatus status, @Param("cutoff") LocalDateTime cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Ticket t where t.showtime.id = :showtimeId and t.status = :status "
            + "and (t.heldAt is null or t.heldAt <= :cutoff)")
    int deleteExpiredHolds(@Param("showtimeId") Long showtimeId, @Param("status") TicketStatus status,
                           @Param("cutoff") LocalDateTime cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Ticket t where t.user.id = :userId and t.showtime.id = :showtimeId "
            + "and t.status = :status and t.id in :ids")
    int deleteHeldTickets(@Param("userId") Long userId, @Param("showtimeId") Long showtimeId,
                          @Param("status") TicketStatus status, @Param("ids") Collection<Long> ids);

    // ===== Bổ sung 27/09: thanh toán MoMo, soát vé, huỷ vé =====

    /** Vé đã trả bằng một giao dịch MoMo - dùng để không xử lý trùng khi khách tải lại trang kết quả. */
    List<Ticket> findByPaymentRef(String paymentRef);

    /**
     * Đánh dấu khách đã vào phòng. Điều kiện "checked_in_at IS NULL" nằm ngay trong câu UPDATE
     * nên hai nhân viên soát cùng một vé cùng lúc thì chỉ một người thành công (cùng tinh thần
     * ADR-1: để database chặn, không đọc lên kiểm tra rồi mới ghi).
     *
     * @return 1 nếu đánh dấu được, 0 nếu vé đã vào phòng rồi hoặc không còn là vé đã thanh toán
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Ticket t set t.checkedInAt = :checkedInAt "
            + "where t.id = :ticketId and t.status = :paid and t.checkedInAt is null "
            + "and (:bookingOrderId is null or t.bookingOrder.id = :bookingOrderId) "
            + "and (t.bookingOrder is null or exists (select b.id from BookingOrder b where b.id = t.bookingOrder.id "
            + "and b.user.id = t.user.id and b.showtime.id = t.showtime.id and b.status = edu.hcmute.cnpm.cinema.entity.BookingOrderStatus.PAID)) "
            + "and exists (select s.id from Showtime s where s.id = t.showtime.id "
            + "and s.movie.active = true and s.endTime > :checkedInAt and s.startTime < :dayEnd "
            + "and s.endTime > s.startTime and exists (select seat.id from Seat seat "
            + "where seat.id = t.seat.id and seat.room.id = s.room.id))")
    int markCheckedIn(@Param("ticketId") Long ticketId, @Param("checkedInAt") LocalDateTime checkedInAt,
                      @Param("paid") TicketStatus paid, @Param("dayEnd") LocalDateTime dayEnd, @Param("bookingOrderId") Long bookingOrderId);

    /**
     * Xoá vé đã thanh toán khi khách huỷ. Chỉ xoá nếu vé chưa vào phòng - huỷ và soát vé
     * xảy ra cùng lúc thì chỉ một bên thắng.
     *
     * @return 1 nếu xoá được, 0 nếu vé đã vào phòng, đã bị huỷ trước đó, hoặc không tồn tại
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Ticket t where t.id = :ticketId and t.status = :paid and t.checkedInAt is null")
    int deletePaidTicketNotCheckedIn(@Param("ticketId") Long ticketId, @Param("paid") TicketStatus paid);
}
