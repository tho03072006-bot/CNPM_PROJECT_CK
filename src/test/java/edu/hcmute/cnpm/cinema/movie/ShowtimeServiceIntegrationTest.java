package edu.hcmute.cnpm.cinema.movie;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.service.ShowtimeService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShowtimeServiceIntegrationTest extends IntegrationTestBase {
    private static final BigDecimal PRICE = new BigDecimal("75000");
    @Autowired private ShowtimeService showtimeService;
    private Movie movie;
    private Room room;
    private LocalDateTime start;

    @BeforeEach
    void prepareShowtime() {
        movie = testDataFactory.createMovie("Phim 120 phút");
        room = testDataFactory.createRoom("Phòng A", 5, 8);
        start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        showtimeService.createShowtime(movie.getId(), room.getId(), start, PRICE);
    }

    @Test
    void shouldRejectWhenNewShowtimeStartsInsideExisting() {
        assertOverlap(start.plusMinutes(30), movie);
    }

    @Test
    void shouldRejectWhenNewShowtimeEndsInsideExisting() {
        assertOverlap(start.minusMinutes(105), movie);
    }

    @Test
    void shouldRejectWhenNewShowtimeContainsExisting() {
        Movie longMovie = testDataFactory.createMovie("Phim dài");
        longMovie.setDurationMin(180);
        movieRepository.save(longMovie);
        assertOverlap(start.minusMinutes(30), longMovie);
    }

    @Test
    void shouldRejectWhenNewShowtimeIsContainedByExisting() {
        Movie shortMovie = testDataFactory.createMovie("Phim ngắn");
        shortMovie.setDurationMin(60);
        movieRepository.save(shortMovie);
        assertOverlap(start.plusMinutes(30), shortMovie);
    }

    @Test
    void shouldAllowWhenNewShowtimeStartsExactlyAfterCleaning() {
        Showtime next = showtimeService.createShowtime(movie.getId(), room.getId(),
                start.plusMinutes(135), PRICE);
        assertThat(next.getStartTime()).isEqualTo(start.plusMinutes(135));
        assertThat(next.getEndTime()).isEqualTo(start.plusMinutes(270));
    }

    @Test
    void shouldAllowOverlappingTimesWhenRoomsDiffer() {
        Room otherRoom = testDataFactory.createRoom("Phòng B", 5, 8);
        assertThat(showtimeService.createShowtime(movie.getId(), otherRoom.getId(), start, PRICE).getId())
                .isNotNull();
    }

    @Test
    void shouldRejectWhenNewShowtimeStartsInPast() {
        assertThatThrownBy(() -> showtimeService.createShowtime(movie.getId(), room.getId(),
                LocalDateTime.now().minusHours(1), PRICE)).isInstanceOf(InvalidBookingException.class);
    }

    private void assertOverlap(LocalDateTime newStart, Movie newMovie) {
        assertThatThrownBy(() -> showtimeService.createShowtime(newMovie.getId(), room.getId(), newStart, PRICE))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("suất #");
    }
}
