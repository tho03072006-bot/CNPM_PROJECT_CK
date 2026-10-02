package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.booking.HoldSeatsRequest;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import edu.hcmute.cnpm.cinema.service.*;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ShowtimeValidationIntegrationTest extends IntegrationTestBase {
    @Autowired private ShowtimeService showtimes;
    @Autowired private SeatBookingService booking;
    @Autowired private MockMvc mvc;
    @MockitoSpyBean private BookingClock clock;
    @MockitoSpyBean private TicketRepository ticketChecks;
    private Movie movie;
    private Room room;
    private LocalDateTime start;

    @BeforeEach
    void prepare() {
        movie = testDataFactory.createMovie("Phim validation");
        room = testDataFactory.createRoom("Cinema validation", 1, 4);
        start = clock.now().plusDays(1).withSecond(0).withNano(0);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-1", "0.01", "75000.5", "50000000", "1E100"})
    void shouldRejectInvalidPriceInService(String price) {
        assertThatThrownBy(() -> showtimes.createShowtime(movie.getId(), room.getId(), start,
                price == null ? null : new BigDecimal(price))).isInstanceOf(BusinessException.class);
        assertThat(showtimeRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.01", "75000.5", "50000000", "1E100"})
    void shouldKeepInvalidPriceOnAdminForm(String price) throws Exception {
        User admin = testDataFactory.createUserWithRole("price.admin@test.local", Role.ADMIN);
        mvc.perform(post("/admin/showtimes").sessionAttr(Constants.SESSION_USER, admin)
                        .param("movieId", movie.getId().toString()).param("roomId", room.getId().toString())
                        .param("startTime", start.toString()).param("basePrice", price))
                .andExpect(status().isOk()).andExpect(view().name("movie/showtime-form"))
                .andExpect(model().attributeHasFieldErrors("showtimeForm", "basePrice"));
        assertThat(showtimeRepository.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"0,1", "-1,1", "1,0", "1,-1"})
    void shouldRejectNonPositiveMovieOrRoomId(long movieId, long roomId) {
        assertThatThrownBy(() -> showtimes.createShowtime(movieId, roomId, start, new BigDecimal("75000")))
                .isInstanceOf(BusinessException.class);
        assertThat(showtimeRepository.count()).isZero();
    }

    @Test
    void shouldAcceptIntegralDatabasePriceOnCreateAndEditForm() throws Exception {
        User admin = testDataFactory.createUserWithRole("edit.admin@test.local", Role.ADMIN);
        mvc.perform(post("/admin/showtimes").sessionAttr(Constants.SESSION_USER, admin)
                        .param("movieId", movie.getId().toString()).param("roomId", room.getId().toString())
                        .param("startTime", start.toString()).param("basePrice", "75000.00"))
                .andExpect(redirectedUrl("/admin/showtimes"));
        Showtime showtime = showtimeRepository.findAll().getFirst();
        mvc.perform(post("/admin/showtimes/{id}", showtime.getId()).sessionAttr(Constants.SESSION_USER, admin)
                        .param("movieId", movie.getId().toString()).param("roomId", room.getId().toString())
                        .param("startTime", start.toString()).param("basePrice", "75001.00"))
                .andExpect(redirectedUrl("/admin/showtimes"));
        assertThat(showtimeRepository.findById(showtime.getId()).orElseThrow().getBasePrice())
                .isEqualByComparingTo("75001");
    }

    @Test
    void shouldUseCinemaClockWhenValidatingNewShowtime() {
        doReturn(start.plusMinutes(1)).when(clock).now();
        assertThatThrownBy(() -> showtimes.createShowtime(movie.getId(), room.getId(), start, new BigDecimal("75000")))
                .isInstanceOf(InvalidBookingException.class);
        assertThat(showtimeRepository.count()).isZero();
    }

    @Test
    void shouldRejectEndTimeOutsideSqlServerRange() {
        assertThatThrownBy(() -> showtimes.createShowtime(movie.getId(), room.getId(),
                LocalDateTime.of(9999, 12, 31, 23, 59), new BigDecimal("75000")))
                .isInstanceOf(InvalidBookingException.class);
        assertThat(showtimeRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldSerializeAdminChangeAndSeatHold(boolean deleting) throws Exception {
        Seat first = testDataFactory.createSeat(room, "A", 1);
        Seat second = testDataFactory.createSeat(room, "A", 2);
        testDataFactory.createSeat(room, "A", 3);
        testDataFactory.createSeat(room, "A", 4);
        Showtime showtime = showtimes.createShowtime(movie.getId(), room.getId(), start, new BigDecimal("75000"));
        Room other = testDataFactory.createRoom("Cinema khác", 1, 4);
        User user = testDataFactory.createCustomer("race.admin.hold@test.local");
        CountDownLatch checked = new CountDownLatch(1), allowChange = new CountDownLatch(1);
        doAnswer(call -> {
            boolean exists = jdbcTemplate.queryForObject("select count(*) from tickets where showtime_id = ?",
                    Long.class, showtime.getId()) > 0;
            checked.countDown();
            if (!allowChange.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Admin test timed out");
            return exists;
        }).when(ticketChecks).existsByShowtimeId(showtime.getId());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> changed = pool.submit(() -> {
                try {
                    if (deleting) showtimes.deleteShowtime(showtime.getId());
                    else showtimes.updateShowtime(showtime.getId(), movie.getId(), other.getId(), start, new BigDecimal("75001"));
                    return true;
                } catch (BusinessException rejected) { return false; }
            });
            boolean signaled = checked.await(10, TimeUnit.SECONDS);
            if (!signaled) changed.get(1, TimeUnit.SECONDS); // Surface worker exceptions instead of hiding them as latch timeouts.
            assertThat(signaled).isTrue();
            CountDownLatch holding = new CountDownLatch(1);
            Future<Boolean> held = pool.submit(() -> {
                HoldSeatsRequest request = new HoldSeatsRequest();
                request.setSeatIds(List.of(first.getId(), second.getId()));
                request.setAgeConfirmed(true); request.setTermsAccepted(true);
                holding.countDown();
                try { booking.holdSeats(showtime.getId(), request, user); return true; }
                catch (BusinessException rejected) { return false; }
            });
            assertThat(holding.await(5, TimeUnit.SECONDS)).isTrue();
            try { held.get(2, TimeUnit.SECONDS); }
            catch (TimeoutException waitingForAdminLock) { /* Holding waits until the admin transaction finishes. */ }
            allowChange.countDown();
            assertThat(changed.get(15, TimeUnit.SECONDS)).isTrue();
            assertThat(held.get(15, TimeUnit.SECONDS)).isFalse();
            assertThat(ticketRepository.count()).isZero();
            if (deleting) assertThat(showtimeRepository.findById(showtime.getId())).isEmpty();
            else assertThat(showtimeRepository.findById(showtime.getId()).orElseThrow().getRoom().getId()).isEqualTo(other.getId());
        } finally {
            allowChange.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"HELD", "PAID"})
    void shouldKeepShowtimeAndPriceWhenTicketsAlreadyExist(String status) {
        Showtime showtime = showtimes.createShowtime(movie.getId(), room.getId(), start, new BigDecimal("75000"));
        Seat seat = testDataFactory.createSeat(room, "A", 1);
        User user = testDataFactory.createCustomer("existing.ticket@test.local");
        Ticket ticket = testDataFactory.newHeldTicket(showtime, seat, user);
        ticket.setStatus(TicketStatus.valueOf(status));
        ticketRepository.saveAndFlush(ticket);
        assertThatThrownBy(() -> showtimes.updateShowtime(showtime.getId(), movie.getId(), room.getId(),
                start.plusHours(1), new BigDecimal("75001"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> showtimes.deleteShowtime(showtime.getId())).isInstanceOf(BusinessException.class);
        Showtime unchanged = showtimeRepository.findById(showtime.getId()).orElseThrow();
        assertThat(unchanged.getStartTime()).isEqualTo(start);
        assertThat(unchanged.getBasePrice()).isEqualByComparingTo("75000");
        assertThat(ticketRepository.findById(ticket.getId()).orElseThrow().getStatus()).isEqualTo(TicketStatus.valueOf(status));
    }
}
