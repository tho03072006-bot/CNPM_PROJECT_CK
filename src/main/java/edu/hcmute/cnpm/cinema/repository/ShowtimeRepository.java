package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovieId(Long movieId);
}
