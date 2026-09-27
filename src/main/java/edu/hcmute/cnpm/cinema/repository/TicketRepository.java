package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    // Dung de ve seat-map: lay cac ghe da bi giu/dat cho 1 suat chieu
    List<Ticket> findByShowtimeIdAndStatusIn(Long showtimeId, List<TicketStatus> statuses);

    boolean existsByShowtimeId(Long showtimeId);

    // ===== Module 3 them: thanh toan, lich su ve, thong ke =====

    /** Ve dang giu cua mot khach cho mot suat chieu - dung o buoc thanh toan. */
    List<Ticket> findByUserIdAndShowtimeIdAndStatus(Long userId, Long showtimeId, TicketStatus status);

    /** Toan bo ve cua mot khach, moi nhat len dau - dung cho trang lich su dat ve. */
    List<Ticket> findByUserIdOrderByHeldAtDesc(Long userId);

    /** Ve da thanh toan trong mot khoang thoi gian - dung cho trang thong ke doanh thu. */
    List<Ticket> findByStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(
            TicketStatus status, LocalDateTime from, LocalDateTime until);

    /** Ve dang giu qua han - dung cho tac vu don ve het han cua Module 2 (ADR-2). */
    List<Ticket> findByStatusAndHeldAtLessThan(TicketStatus status, LocalDateTime heldBefore);

    // ===== Bo sung 27/09: thanh toan MoMo, soat ve, huy ve =====

    /** Ve da tra bang mot giao dich MoMo - dung de khong xu ly trung khi khach tai lai trang ket qua. */
    List<Ticket> findByPaymentRef(String paymentRef);

    /**
     * Danh dau khach da vao phong. Dieu kien "checked_in_at IS NULL" nam ngay trong cau UPDATE
     * nen hai nhan vien soat cung mot ve cung luc thi chi mot nguoi thanh cong (cung tinh than
     * ADR-1: de database chan, khong doc len kiem tra roi moi ghi).
     *
     * @return 1 neu danh dau duoc, 0 neu ve da vao phong roi hoac khong con la ve da thanh toan
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Ticket t set t.checkedInAt = :checkedInAt "
            + "where t.id = :ticketId and t.status = :paid and t.checkedInAt is null")
    int markCheckedIn(@Param("ticketId") Long ticketId, @Param("checkedInAt") LocalDateTime checkedInAt,
                      @Param("paid") TicketStatus paid);

    /**
     * Xoa ve da thanh toan khi khach huy. Chi xoa neu ve chua vao phong - huy va soat ve
     * xay ra cung luc thi chi mot ben thang.
     *
     * @return 1 neu xoa duoc, 0 neu ve da vao phong, da bi huy truoc do, hoac khong ton tai
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Ticket t where t.id = :ticketId and t.status = :paid and t.checkedInAt is null")
    int deletePaidTicketNotCheckedIn(@Param("ticketId") Long ticketId, @Param("paid") TicketStatus paid);
}
