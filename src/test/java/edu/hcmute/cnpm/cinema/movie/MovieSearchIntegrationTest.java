package edu.hcmute.cnpm.cinema.movie;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.service.MovieService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tìm phim theo tên, lọc theo thể loại, và mở bán lại phim đã ngừng chiếu. */
@AutoConfigureMockMvc
@DisplayName("Tìm kiếm, lọc và mở bán lại phim")
class MovieSearchIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MovieService movieService;
    @Autowired
    private MockMvc mockMvc;

    private Movie ghostMovie;
    private Movie cartoonMovie;
    private Movie stoppedMovie;

    @BeforeEach
    void createMovies() {
        ghostMovie = createMovieWithGenre("Bóng Ma Nhà Hát", "Hài, Kinh dị", true);
        cartoonMovie = createMovieWithGenre("Đảo Quên Lãng", "Hoạt hình, Hài, Gia đình", true);
        stoppedMovie = createMovieWithGenre("Minions & Quái Vật", "Hoạt hình, Hài", false);
    }

    @Test
    @DisplayName("Gõ không dấu vẫn tìm ra phim có dấu, kể cả chữ đ")
    void shouldFindMovie_whenKeywordTypedWithoutAccents() {
        assertThat(movieService.searchActiveMovies("bong ma", null))
                .extracting(Movie::getTitle).containsExactly("Bóng Ma Nhà Hát");
        assertThat(movieService.searchActiveMovies("DAO QUEN", null))
                .extracting(Movie::getTitle).containsExactly("Đảo Quên Lãng");
    }

    @Test
    @DisplayName("Lọc theo thể loại khớp đúng từng thể loại trong danh sách, không khớp nửa chữ")
    void shouldFilterByGenreToken_whenGenreSelected() {
        assertThat(movieService.searchActiveMovies(null, "Hài"))
                .extracting(Movie::getTitle).containsExactlyInAnyOrder("Bóng Ma Nhà Hát", "Đảo Quên Lãng");
        assertThat(movieService.searchActiveMovies(null, "Kinh dị"))
                .extracting(Movie::getTitle).containsExactly("Bóng Ma Nhà Hát");
        assertThat(movieService.searchActiveMovies(null, "Kinh"))
                .as("Chọn thể loại phải khớp nguyên thể loại, không phải một phần chữ")
                .isEmpty();
    }

    @Test
    @DisplayName("Phim đã ngừng chiếu không hiện trong kết quả tìm kiếm lẫn danh sách thể loại")
    void shouldExcludeInactiveMovies_whenSearchingAndListingGenres() {
        assertThat(movieService.searchActiveMovies("minions", null)).isEmpty();
        assertThat(movieService.findActiveGenres())
                .containsExactlyInAnyOrder("Hài", "Kinh dị", "Hoạt hình", "Gia đình");
    }

    @Test
    @DisplayName("Mở bán lại phim đã ngừng chiếu thì phim hiện lại cho khán giả")
    void shouldShowMovieAgain_whenReactivated() {
        movieService.activateMovie(stoppedMovie.getId());

        assertThat(movieService.searchActiveMovies("minions", null))
                .extracting(Movie::getId).containsExactly(stoppedMovie.getId());
    }

    @Test
    @DisplayName("Trang Phim nhận tham số tìm kiếm và chỉ hiện phim khớp")
    void shouldRenderOnlyMatchingMovies_whenSearchingOnMoviesPage() throws Exception {
        mockMvc.perform(get("/movies").param("q", "bong ma"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bóng Ma Nhà Hát")))
                .andExpect(content().string(not(containsString("Đảo Quên Lãng"))))
                .andExpect(content().string(containsString("Tìm thấy 1 phim phù hợp")));
    }

    @Test
    @DisplayName("Quản trị viên bấm Mở bán lại trên trang quản trị thì phim chuyển về đang chiếu")
    void shouldActivateMovie_whenAdminPostsActivate() throws Exception {
        User admin = testDataFactory.createUserWithRole("admin@example.com", Role.ADMIN);

        mockMvc.perform(post("/admin/movies/{id}/activate", stoppedMovie.getId())
                        .sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().is3xxRedirection());

        assertThat(movieRepository.findById(stoppedMovie.getId()).orElseThrow().getActive()).isTrue();
    }

    private Movie createMovieWithGenre(String title, String genre, boolean isActive) {
        Movie movie = testDataFactory.createMovie(title);
        movie.setGenre(genre);
        movie.setActive(isActive);
        return movieRepository.save(movie);
    }
}
