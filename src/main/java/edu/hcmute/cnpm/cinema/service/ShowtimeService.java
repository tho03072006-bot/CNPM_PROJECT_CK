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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ShowtimeService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_BREAK_MINUTES = 180;

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final TicketRepository ticketRepository;
    private final BookingLockService locks;
    private final BookingClock clock;
    /** Phút nghỉ tối thiểu giữa hai suất liên tiếp trong cùng phòng (app.showtime.break-minutes). */
    private final int breakMinutes;

    public ShowtimeService(ShowtimeRepository showtimeRepository, MovieRepository movieRepository,
                           RoomRepository roomRepository, TicketRepository ticketRepository,
                           BookingLockService locks, BookingClock clock,
                           @Value("${app.showtime.break-minutes:15}") int breakMinutes) {
        if (breakMinutes < 0 || breakMinutes > MAX_BREAK_MINUTES) {
            throw new IllegalStateException("app.showtime.break-minutes phải từ 0 tới " + MAX_BREAK_MINUTES + " phút.");
        }
        this.showtimeRepository = showtimeRepository;
        this.movieRepository = movieRepository;
        this.roomRepository = roomRepository;
        this.ticketRepository = ticketRepository;
        this.locks = locks;
        this.clock = clock;
        this.breakMinutes = breakMinutes;
    }

    /** Khoảng nghỉ tối thiểu (phút) giữa hai suất liên tiếp trong cùng một phòng. */
    public int getBreakMinutes() {
        return breakMinutes;
    }

    @Transactional(readOnly = true)
    public List<Showtime> findAllShowtimes() {
        return showtimeRepository.findAllByOrderByStartTimeAsc();
    }

    @Transactional(readOnly = true)
    public List<Showtime> findShowtimes(LocalDate date, Long movieId, Long roomId) {
        return findAllShowtimes().stream()
                .filter(showtime -> date == null || showtime.getStartTime().toLocalDate().equals(date))
                .filter(showtime -> movieId == null || showtime.getMovie().getId().equals(movieId))
                .filter(showtime -> roomId == null || showtime.getRoom().getId().equals(roomId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Showtime> findUpcomingByMovie(Long movieId) {
        return showtimeRepository.findByMovieIdAndStartTimeAfterOrderByStartTimeAsc(movieId, clock.now());
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
        // Serialize the ticket check and edit with hold/cancel/payment transactions.
        Showtime showtime = locks.lock(showtimeId);
        if (ticketRepository.existsByShowtimeId(showtimeId)) {
            throw new BusinessException("Không thể sửa suất chiếu đã có vé đặt.");
        }
        return saveShowtime(showtime, movieId, roomId, startTime, basePrice);
    }

    @Transactional
    public void deleteShowtime(Long showtimeId) {
        Showtime showtime = locks.lock(showtimeId);
        if (ticketRepository.existsByShowtimeId(showtimeId)) {
            throw new BusinessException("Không thể xoá suất chiếu đã có vé đặt.");
        }
        showtimeRepository.delete(showtime);
    }

    private Showtime saveShowtime(Showtime showtime, Long movieId, Long roomId,
                                  LocalDateTime startTime, BigDecimal basePrice) {
        if (startTime == null || !startTime.isAfter(clock.now()) || startTime.getYear() > 9999) {
            throw new InvalidBookingException("Giờ bắt đầu phải ở trong tương lai.");
        }
        if (!SeatPricingService.isValidBasePrice(basePrice)) {
            throw new BusinessException("Giá vé cơ bản phải là số đồng nguyên từ 1 đến 49.999.999 đồng.");
        }
        if (movieId == null || movieId <= 0 || roomId == null || roomId <= 0) {
            throw new BusinessException("Mã phim và mã phòng phải là số nguyên dương.");
        }
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("phim", movieId));
        if (!Boolean.TRUE.equals(movie.getActive())) {
            throw new BusinessException("Không thể xếp lịch cho phim đã ngừng chiếu.");
        }
        if (movie.getDurationMin() == null || movie.getDurationMin() <= 0) {
            throw new BusinessException("Phim chưa có thời lượng hợp lệ.");
        }
        // end_time lưu giờ phòng sẵn sàng cho suất sau: hết phim cộng khoảng nghỉ để khách ra vào
        // và nhân viên dọn phòng.
        LocalDateTime endTime;
        try {
            endTime = startTime.plusMinutes(movie.getDurationMin().longValue() + breakMinutes);
            if (endTime.getYear() > 9999) throw new DateTimeException("SQL Server date range exceeded");
        } catch (DateTimeException exception) {
            throw new InvalidBookingException("Giờ kết thúc vượt phạm vi ngày giờ cho phép. Vui lòng chọn giờ chiếu khác.");
        }
        // Khoá phòng trong giao dịch để hai quản trị viên không cùng xếp lịch trùng giờ.
        Room room = roomRepository.findLockedById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("phòng chiếu", roomId));
        // So theo khoảng nghỉ HIỆN HÀNH chứ không theo end_time đã lưu: suất cũ có thể được xếp lúc
        // khoảng nghỉ còn ngắn hơn. Vì vậy lấy rộng ra thêm một khoảng nghỉ rồi lọc lại cho chính xác.
        for (Showtime existing : showtimeRepository.findByRoomIdAndStartTimeLessThanAndEndTimeGreaterThan(
                roomId, endTime, startTime.minusMinutes(breakMinutes))) {
            if (showtime != null && existing.getId().equals(showtime.getId())) {
                continue;
            }
            LocalDateTime existingMovieEnd = movieEndOf(existing);
            boolean isTooClose = startTime.isBefore(existingMovieEnd.plusMinutes(breakMinutes))
                    && existing.getStartTime().isBefore(endTime);
            if (isTooClose) {
                throw new InvalidBookingException(conflictMessage(existing, existingMovieEnd, room, startTime));
            }
        }
        if (!startTime.isAfter(clock.now())) {
            throw new InvalidBookingException("Giờ bắt đầu đã qua trong lúc xử lý. Vui lòng chọn giờ chiếu khác.");
        }
        Showtime target = showtime == null ? new Showtime() : showtime;
        target.setMovie(movie);
        target.setRoom(room);
        target.setStartTime(startTime);
        target.setEndTime(endTime);
        target.setBasePrice(basePrice.setScale(2));
        return showtimeRepository.save(target);
    }

    /** Giờ hết phim (chưa tính khoảng nghỉ). Dữ liệu thiếu thời lượng thì lấy end_time đã lưu. */
    private LocalDateTime movieEndOf(Showtime existing) {
        Movie movie = existing.getMovie();
        if (movie == null || movie.getDurationMin() == null) {
            return existing.getEndTime();
        }
        return existing.getStartTime().plusMinutes(movie.getDurationMin().longValue());
    }

    /**
     * Lời nhắc khi trùng lịch: nói rõ suất nào chiếm phòng, khoảng nghỉ bắt buộc và giờ sớm nhất
     * được bắt đầu sau suất đó, để quản trị viên sửa một lần là đúng.
     */
    private String conflictMessage(Showtime existing, LocalDateTime existingMovieEnd, Room room,
                                   LocalDateTime requestedStart) {
        String movieTitle = existing.getMovie() == null ? "" : existing.getMovie().getTitle() + ", ";
        StringBuilder message = new StringBuilder("Giờ chiếu trùng với suất chiếu số ").append(existing.getId())
                .append(" (").append(movieTitle).append(existing.getStartTime().format(TIME_FORMAT))
                .append(" đến ").append(existingMovieEnd.format(HOUR_FORMAT)).append(") ở ").append(room.getName())
                .append(". Giữa hai suất trong cùng phòng phải nghỉ ít nhất ").append(breakMinutes)
                .append(" phút để khách ra vào và dọn phòng, nên sau suất đó sớm nhất bắt đầu lúc ")
                .append(existingMovieEnd.plusMinutes(breakMinutes).format(TIME_FORMAT));
        if (requestedStart.isBefore(existing.getStartTime())) {
            message.append(", hoặc chọn giờ để phim kết thúc trước ")
                    .append(existing.getStartTime().minusMinutes(breakMinutes).format(HOUR_FORMAT));
        }
        return message.append('.').toString();
    }
}
