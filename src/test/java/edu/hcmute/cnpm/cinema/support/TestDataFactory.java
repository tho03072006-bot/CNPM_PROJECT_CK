package edu.hcmute.cnpm.cinema.support;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.repository.MovieRepository;
import edu.hcmute.cnpm.cinema.repository.RoomRepository;
import edu.hcmute.cnpm.cinema.repository.SeatRepository;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Xuong tao du lieu mau dung chung cho tat ca test tich hop cua 4 module.
 *
 * Muc dich: moi nguoi khong phai tu viet lai doan "tao phim -> tao phong -> tao ghe ->
 * tao suat chieu" trong tung file test. Can them kieu du lieu mau nao thi them method moi
 * vao day, KHONG sua cac method dang co nguoi khac dung.
 *
 * Phu trach: Tho (Module 4).
 */
@Component
public class TestDataFactory {

    private static final BigDecimal DEFAULT_BASE_PRICE = new BigDecimal("75000.00");

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;

    public TestDataFactory(UserRepository userRepository,
                           MovieRepository movieRepository,
                           RoomRepository roomRepository,
                           SeatRepository seatRepository,
                           ShowtimeRepository showtimeRepository) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.roomRepository = roomRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
    }

    public User createCustomer(String email) {
        User customer = new User();
        customer.setFullName("Khach hang test");
        customer.setEmail(email);
        customer.setPhone("0900000000");
        customer.setPasswordHash("$2a$10$test.hash.khong.dung.that");
        customer.setRole(Role.CUSTOMER);
        return userRepository.save(customer);
    }

    public Movie createMovie(String title) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setGenre("Hanh dong");
        movie.setDurationMin(120);
        movie.setAgeRating("C13");
        movie.setActive(true);
        return movieRepository.save(movie);
    }

    public Room createRoom(String name, int totalRows, int totalColumns) {
        Room room = new Room();
        room.setName(name);
        room.setTotalRows(totalRows);
        room.setTotalColumns(totalColumns);
        return roomRepository.save(room);
    }

    public Seat createSeat(Room room, String seatRow, int seatColumn) {
        Seat seat = new Seat();
        seat.setRoom(room);
        seat.setSeatRow(seatRow);
        seat.setSeatColumn(seatColumn);
        seat.setSeatType("NORMAL");
        return seatRepository.save(seat);
    }

    public Showtime createShowtime(Movie movie, Room room, LocalDateTime startTime) {
        return createShowtime(movie, room, startTime, DEFAULT_BASE_PRICE);
    }

    /**
     * Tao suat chieu voi gia ve tu chon.
     *
     * Dung cho test bang gia ve: can dung gia cu the de kiem tra gia cong bo co
     * khop voi gia gan vao suat chieu hay khong.
     */
    public Showtime createShowtime(Movie movie, Room room, LocalDateTime startTime, BigDecimal basePrice) {
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(startTime);
        showtime.setEndTime(startTime.plusMinutes(movie.getDurationMin()));
        showtime.setBasePrice(basePrice);
        return showtimeRepository.save(showtime);
    }

    /**
     * Tao doi tuong Ticket o trang thai HELD nhung CHUA luu xuong database.
     * Dung cho cac test tu quyet dinh thoi diem insert (vi du test race-condition).
     */
    public Ticket newHeldTicket(Showtime showtime, Seat seat, User user) {
        Ticket ticket = new Ticket();
        ticket.setShowtime(showtime);
        ticket.setSeat(seat);
        ticket.setUser(user);
        ticket.setStatus(TicketStatus.HELD);
        ticket.setPrice(showtime.getBasePrice());
        ticket.setHeldAt(LocalDateTime.now());
        return ticket;
    }
}
