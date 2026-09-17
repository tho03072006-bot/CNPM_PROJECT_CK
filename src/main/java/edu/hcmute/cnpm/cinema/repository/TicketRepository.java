package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    // Dung de ve seat-map: lay cac ghe da bi giu/dat cho 1 suat chieu
    List<Ticket> findByShowtimeIdAndStatusIn(Long showtimeId, List<TicketStatus> statuses);
}
