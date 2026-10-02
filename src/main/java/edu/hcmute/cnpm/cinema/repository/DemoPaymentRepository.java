package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;

public interface DemoPaymentRepository extends JpaRepository<DemoPayment, Long> {
    Optional<DemoPayment> findByPublicId(String publicId);
    @Query("select p.showtimeId from DemoPayment p where p.publicId = :id")
    Optional<Long> findShowtimeId(@Param("id") String id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from DemoPayment p where p.publicId = :id")
    Optional<DemoPayment> findForUpdate(@Param("id") String id);
    List<DemoPayment> findByUserIdAndShowtimeIdAndStatus(Long userId, Long showtimeId, DemoPaymentStatus status);
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);
}
