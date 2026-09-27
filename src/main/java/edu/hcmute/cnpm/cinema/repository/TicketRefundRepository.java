package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.TicketRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRefundRepository extends JpaRepository<TicketRefund, Long> {

    /** Vé đã huỷ của một khách, mới nhất lên đầu - cho trang vé của tôi. */
    List<TicketRefund> findByUserIdOrderByRefundedAtDesc(Long userId);

    /** Vé đã huỷ theo ngày BÁN - phần phí giữ lại vẫn tính vào doanh thu ngày bán. */
    List<TicketRefund> findByPaidAtGreaterThanEqualAndPaidAtLessThan(LocalDateTime from, LocalDateTime until);

    /** Vé đã huỷ theo ngày HOÀN TIỀN - cho ô "đã hoàn tiền" trên trang thống kê. */
    List<TicketRefund> findByRefundedAtGreaterThanEqualAndRefundedAtLessThan(LocalDateTime from, LocalDateTime until);
}
