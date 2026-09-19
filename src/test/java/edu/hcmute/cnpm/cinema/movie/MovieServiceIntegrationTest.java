package edu.hcmute.cnpm.cinema.movie;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.service.MovieService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovieServiceIntegrationTest extends IntegrationTestBase {
    @Autowired private MovieService movieService;

    @Test
    @DisplayName("Danh sách công khai chỉ trả về phim đang chiếu")
    void shouldReturnOnlyActiveMovies_whenListingPublicMovies() {
        Movie active = testDataFactory.createMovie("Phim đang chiếu");
        Movie inactive = testDataFactory.createMovie("Phim đã ngừng");
        inactive.setActive(false);
        movieRepository.save(inactive);

        assertThat(movieService.findActiveMovies()).extracting(Movie::getId).containsExactly(active.getId());
    }

    @Test
    @DisplayName("Không tìm thấy phim thì báo lỗi 404")
    void shouldThrowNotFound_whenMovieIdDoesNotExist() {
        assertThatThrownBy(() -> movieService.findById(99999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Ngừng chiếu giữ lại dữ liệu phim")
    void shouldKeepMovieRow_whenDeactivating() {
        Movie movie = testDataFactory.createMovie("Phim ngừng chiếu");
        movieService.deactivateMovie(movie.getId());

        assertThat(movieRepository.findById(movie.getId())).get().extracting(Movie::getActive).isEqualTo(false);
    }

    @Test
    @DisplayName("Từ chối phim thiếu tên hoặc có thời lượng bằng không")
    void shouldRejectEmptyTitleAndZeroDuration_whenCreating() {
        Movie movie = new Movie();
        movie.setTitle("  ");
        movie.setDurationMin(0);
        assertThatThrownBy(() -> movieService.createMovie(movie)).isInstanceOf(BusinessException.class);
        movie.setTitle("Phim mới");
        assertThatThrownBy(() -> movieService.createMovie(movie)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Lưu tên phim tiếng Việt có dấu")
    void shouldPreserveVietnameseTitle_whenCreating() {
        Movie movie = new Movie();
        movie.setTitle("Bão Giữa Trời Quang");
        movie.setDurationMin(95);
        Movie saved = movieService.createMovie(movie);
        assertThat(movieRepository.findById(saved.getId())).get()
                .extracting(Movie::getTitle).isEqualTo("Bão Giữa Trời Quang");
    }

    @Test
    @DisplayName("Không đổi thời lượng khi phim đã có suất chiếu")
    void shouldRejectDurationChange_whenMovieAlreadyHasShowtimes() {
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
