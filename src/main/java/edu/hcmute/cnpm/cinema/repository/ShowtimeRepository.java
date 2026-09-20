package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovieId(Long movieId);

    List<Showtime> findByMovieIdAndStartTimeAfterOrderByStartTimeAsc(Long movieId, LocalDateTime now);

    List<Showtime> findAllByOrderByStartTimeAsc();

    // Lay suat chieu trong mot khoang thoi gian: tinh tu "from" (lay ca dung
    // moc do) den truoc "until" (khong lay moc do). Dung cho trang lich chieu
    // theo ngay, nen khoang thuong la [0h ngay X, 0h ngay X+1).
    List<Showtime> findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            LocalDateTime from, LocalDateTime until);

    List<Showtime> findByRoomIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long roomId, LocalDateTime endTime, LocalDateTime startTime);

    boolean existsByRoomId(Long roomId);

    boolean existsByMovieId(Long movieId);
}
