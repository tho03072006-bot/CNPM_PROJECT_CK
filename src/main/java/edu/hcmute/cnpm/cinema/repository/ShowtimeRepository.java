package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovieId(Long movieId);

    List<Showtime> findByMovieIdAndStartTimeAfterOrderByStartTimeAsc(Long movieId, LocalDateTime now);

    List<Showtime> findAllByOrderByStartTimeAsc();

    List<Showtime> findByRoomIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long roomId, LocalDateTime endTime, LocalDateTime startTime);

    boolean existsByRoomId(Long roomId);

    boolean existsByMovieId(Long movieId);
}
