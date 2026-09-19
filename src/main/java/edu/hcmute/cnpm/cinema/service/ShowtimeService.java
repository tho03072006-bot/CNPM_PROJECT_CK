package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.MovieRepository;
import edu.hcmute.cnpm.cinema.repository.RoomRepository;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShowtimeService {
    private static final int CLEANING_MINUTES = 15;

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final TicketRepository ticketRepository;

    public ShowtimeService(ShowtimeRepository showtimeRepository, MovieRepository movieRepository,
                           RoomRepository roomRepository, TicketRepository ticketRepository) {
        this.showtimeRepository = showtimeRepository;
        this.movieRepository = movieRepository;
        this.roomRepository = roomRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public List<Showtime> findAllShowtimes() {
        return showtimeRepository.findAllByOrderByStartTimeAsc();
    }

    @Transactional(readOnly = true)
    public List<Showtime> findUpcomingByMovie(Long movieId) {
        return showtimeRepository.findByMovieIdAndStartTimeAfterOrderByStartTimeAsc(movieId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Showtime findById(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("suất chiếu", showtimeId));
    }

    @Transactional
    public Showtime createShowtime(Long movieId, Long roomId, LocalDateTime startTime, BigDecimal basePrice) {
        return saveShowtime(null, movieId, roomId, startTime, basePrice);
    }

    @Transactional
    public Showtime updateShowtime(Long showtimeId, Long movieId, Long roomId,
                                   LocalDateTime startTime, BigDecimal basePrice) {
        Showtime showtime = findById(showtimeId);
        if (ticketRepository.existsByShowtimeId(showtimeId)) {
            throw new BusinessException("Không thể sửa suất chiếu đã có vé đặt.");
        }
        return saveShowtime(showtime, movieId, roomId, startTime, basePrice);
    }

    @Transactional
    public void deleteShowtime(Long showtimeId) {
        Showtime showtime = findById(showtimeId);
        if (ticketRepository.existsByShowtimeId(showtimeId)) {
            throw new BusinessException("Không thể xoá suất chiếu đã có vé đặt.");
        }
        showtimeRepository.delete(showtime);
    }

    private Showtime saveShowtime(Showtime showtime, Long movieId, Long roomId,
                                  LocalDateTime startTime, BigDecimal basePrice) {
        if (startTime == null || !startTime.isAfter(LocalDateTime.now())) {
            throw new InvalidBookingException("Giờ bắt đầu phải ở trong tương lai.");
        }
        if (basePrice == null || basePrice.signum() <= 0) {
            throw new BusinessException("Giá vé cơ bản phải lớn hơn 0.");
        }
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("phim", movieId));
        if (!Boolean.TRUE.equals(movie.getActive())) {
            throw new BusinessException("Không thể xếp lịch cho phim đã ngừng chiếu.");
        }
        // Khoá phòng trong giao dịch để hai quản trị viên không cùng xếp lịch trùng giờ.
        Room room = roomRepository.findLockedById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("phòng chiếu", roomId));
        LocalDateTime endTime = startTime.plusMinutes(movie.getDurationMin() + CLEANING_MINUTES);
        for (Showtime existing : showtimeRepository
                .findByRoomIdAndStartTimeLessThanAndEndTimeGreaterThan(roomId, endTime, startTime)) {
            if (showtime == null || !existing.getId().equals(showtime.getId())) {
                throw new InvalidBookingException("Giờ chiếu trùng với suất #" + existing.getId()
                        + " (" + existing.getStartTime() + " đến " + existing.getEndTime() + ").");
            }
        }
        Showtime target = showtime == null ? new Showtime() : showtime;
        target.setMovie(movie);
        target.setRoom(room);
        target.setStartTime(startTime);
        target.setEndTime(endTime);
        target.setBasePrice(basePrice);
        return showtimeRepository.save(target);
    }
}
