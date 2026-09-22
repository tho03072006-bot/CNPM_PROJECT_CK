package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.repository.MovieRepository;
import edu.hcmute.cnpm.cinema.repository.RoomRepository;
import edu.hcmute.cnpm.cinema.repository.SeatRepository;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import edu.hcmute.cnpm.cinema.service.SeatBookingService;
import edu.hcmute.cnpm.cinema.service.SeatPricingService;
import edu.hcmute.cnpm.cinema.service.SeatSelectionPolicy;
import edu.hcmute.cnpm.cinema.service.SeatService;
import edu.hcmute.cnpm.cinema.support.TestDataFactory;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Dùng factory chung với repository giả cho unit test, hoàn toàn không kết nối database. */
class BookingTestFixture {
    final SeatRepository seatRepository = mock(SeatRepository.class);
    final ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
    final TicketRepository ticketRepository = mock(TicketRepository.class);
    final UserRepository userRepository = mock(UserRepository.class);
    final MovieRepository movieRepository = mock(MovieRepository.class);
    final RoomRepository roomRepository = mock(RoomRepository.class);
    final TestDataFactory testDataFactory = new TestDataFactory(userRepository, movieRepository,
            roomRepository, seatRepository, showtimeRepository);
    final SeatPricingService seatPricingService = new SeatPricingService();
    final SeatSelectionPolicy seatSelectionPolicy = new SeatSelectionPolicy();
    final SeatService seatService = new SeatService(showtimeRepository, seatRepository,
            ticketRepository, seatPricingService);
    final SeatBookingService seatBookingService = new SeatBookingService(seatService,
            seatPricingService, seatSelectionPolicy, seatRepository, ticketRepository, userRepository);
    final Movie movie;
    final Room room;
    final Seat seat;
    final Showtime showtime;
    final User customer;

    BookingTestFixture() {
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(showtimeRepository.save(any(Showtime.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        movie = testDataFactory.createMovie("Phim kiểm thử");
        movie.setId(1L);
        room = testDataFactory.createRoom("Phòng kiểm thử", 2, 8);
        room.setId(1L);
        seat = testDataFactory.createSeat(room, "A", 1);
        seat.setId(1L);
        showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(1));
        showtime.setId(1L);
        customer = testDataFactory.createCustomer("module2@test.local");
        customer.setId(1L);
    }
}
