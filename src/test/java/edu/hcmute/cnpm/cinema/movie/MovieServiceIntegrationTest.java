package edu.hcmute.cnpm.cinema.movie;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.service.MovieService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovieServiceIntegrationTest extends IntegrationTestBase {
    @Autowired private MovieService movieService;

    @Test
    void shouldReturnOnlyActiveMoviesWhenListingPublicMovies() {
        Movie active = testDataFactory.createMovie("Phim đang chiếu");
        Movie inactive = testDataFactory.createMovie("Phim đã ngừng");
        inactive.setActive(false);
        movieRepository.save(inactive);

        assertThat(movieService.findActiveMovies()).extracting(Movie::getId).containsExactly(active.getId());
    }

    @Test
    void shouldThrowNotFoundWhenMovieIdDoesNotExist() {
        assertThatThrownBy(() -> movieService.findById(99999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldKeepMovieRowWhenDeactivating() {
        Movie movie = testDataFactory.createMovie("Phim ngừng chiếu");
        movieService.deactivateMovie(movie.getId());

        assertThat(movieRepository.findById(movie.getId())).get().extracting(Movie::getActive).isEqualTo(false);
    }

    @Test
    void shouldRejectEmptyTitleAndZeroDurationWhenCreating() {
        Movie movie = new Movie();
        movie.setTitle("  ");
        movie.setDurationMin(0);
        assertThatThrownBy(() -> movieService.createMovie(movie)).isInstanceOf(BusinessException.class);
        movie.setTitle("Phim mới");
        assertThatThrownBy(() -> movieService.createMovie(movie)).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldPreserveVietnameseTitleWhenCreating() {
        Movie movie = new Movie();
        movie.setTitle("Bão Giữa Trời Quang");
        movie.setDurationMin(95);
        Movie saved = movieService.createMovie(movie);
        assertThat(movieRepository.findById(saved.getId())).get()
                .extracting(Movie::getTitle).isEqualTo("Bão Giữa Trời Quang");
    }

    @Test
    void shouldRejectDurationChangeWhenMovieAlreadyHasShowtimes() {
        Movie movie = testDataFactory.createMovie("Phim đã lên lịch");
        Room room = testDataFactory.createRoom("Phòng A", 5, 8);
        testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(2));
        Movie changed = new Movie();
        changed.setTitle(movie.getTitle());
        changed.setDurationMin(90);

        assertThatThrownBy(() -> movieService.updateMovie(movie.getId(), changed))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("thời lượng");
    }
}
