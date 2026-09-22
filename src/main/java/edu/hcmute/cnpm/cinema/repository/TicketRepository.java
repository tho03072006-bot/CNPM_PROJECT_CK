package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
