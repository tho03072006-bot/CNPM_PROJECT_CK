package edu.hcmute.cnpm.cinema.repository;
import edu.hcmute.cnpm.cinema.entity.TicketPublicCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TicketPublicCodeRepository extends JpaRepository<TicketPublicCode, Long> {
    Optional<TicketPublicCode> findByPublicCode(String publicCode);
    boolean existsByPublicCode(String publicCode);
}
