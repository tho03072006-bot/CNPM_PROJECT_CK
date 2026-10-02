package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    // Giữ điều kiện ổn định trong transaction thanh toán khi quản trị viên đang đổi mã.
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<Voucher> findByCode(String code);
    List<Voucher> findAllByOrderByCodeAsc();
}
