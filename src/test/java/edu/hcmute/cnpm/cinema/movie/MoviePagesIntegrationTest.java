package edu.hcmute.cnpm.cinema.movie;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@AutoConfigureMockMvc
class MoviePagesIntegrationTest extends IntegrationTestBase {
    @Autowired private MockMvc mockMvc;

    @Test
    void shouldShowOnlyActiveMoviesWhenOpeningPublicList() throws Exception {
        testDataFactory.createMovie("Phim đang chiếu");
        Movie inactive = testDataFactory.createMovie("Phim đã ngừng");
        inactive.setActive(false);
        movieRepository.save(inactive);

        mockMvc.perform(MockMvcRequestBuilders.get("/movies"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Phim đang chiếu")))
                .andExpect(content().string(not(containsString("Phim đã ngừng"))));
    }

    @Test
    void shouldReturn404WhenMovieDoesNotExist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/movies/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDenyCustomerWhenOpeningAdminPages() throws Exception {
        User customer = new User();
        customer.setRole(Role.CUSTOMER);
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/movies")
                        .sessionAttr(Constants.SESSION_USER, customer))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRenderAdminPagesWhenSessionIsAdmin() throws Exception {
        User admin = new User();
        admin.setRole(Role.ADMIN);
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/movies")
                        .sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/rooms/new")
                        .sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/showtimes/new")
                        .sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().isOk());
    }

    @Test
    void shouldShowFieldErrorWhenMovieTitleIsBlank() throws Exception {
        User admin = new User();
        admin.setRole(Role.ADMIN);
        mockMvc.perform(MockMvcRequestBuilders.post("/admin/movies")
                        .sessionAttr(Constants.SESSION_USER, admin)
                        .param("title", " ").param("durationMin", "0"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("movieForm", "title", "durationMin"));
    }

    @Test
    void shouldRenderUpcomingShowtimeOnMovieAndAdminPages() throws Exception {
        Movie movie = testDataFactory.createMovie("Phim cuối tuần");
        Room room = testDataFactory.createRoom("Phòng số một", 5, 8);
        testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(2));
        User admin = new User();
        admin.setRole(Role.ADMIN);

        mockMvc.perform(MockMvcRequestBuilders.get("/movies/{id}", movie.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/booking/showtime/")));
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/showtimes")
                        .sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Phim cuối tuần")));
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/movies")
                        .sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Phim cuối tuần")));
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/rooms")
                        .sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Phòng số một")));
    }

    @Test
    void shouldCreateMovieRoomAndShowtimeThroughAdminForms() throws Exception {
        User admin = new User();
        admin.setRole(Role.ADMIN);
        mockMvc.perform(MockMvcRequestBuilders.post("/admin/movies")
                        .sessionAttr(Constants.SESSION_USER, admin)
                        .param("title", "Bão Giữa Trời Quang")
                        .param("genre", "Tâm lý")
                        .param("durationMin", "120"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/movies"));
        Movie movie = movieRepository.findAll().getFirst();
        assertThat(movie.getTitle()).isEqualTo("Bão Giữa Trời Quang");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/rooms")
                        .sessionAttr(Constants.SESSION_USER, admin)
                        .param("name", "Phòng A")
                        .param("totalRows", "8")
                        .param("totalColumns", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/rooms"));
        Room room = roomRepository.findAll().getFirst();
        assertThat(seatRepository.findByRoomId(room.getId())).hasSize(80);

        String start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        mockMvc.perform(MockMvcRequestBuilders.post("/admin/showtimes")
                        .sessionAttr(Constants.SESSION_USER, admin)
                        .param("movieId", movie.getId().toString())
                        .param("roomId", room.getId().toString())
                        .param("startTime", start)
                        .param("basePrice", "75000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/showtimes"));
        assertThat(showtimeRepository.findAll()).singleElement()
                .satisfies(showtime -> assertThat(showtime.getEndTime())
                        .isEqualTo(showtime.getStartTime().plusMinutes(135)));
    }

    @Test
    void shouldUpdateAndDeactivateMovieThroughAdminForms() throws Exception {
        User admin = new User();
        admin.setRole(Role.ADMIN);
        Movie movie = testDataFactory.createMovie("Tên cũ");
        mockMvc.perform(MockMvcRequestBuilders.post("/admin/movies/{id}", movie.getId())
                        .sessionAttr(Constants.SESSION_USER, admin)
                        .param("title", "Tên mới")
                        .param("durationMin", "100"))
                .andExpect(status().is3xxRedirection());
        assertThat(movieRepository.findById(movie.getId()).orElseThrow().getTitle()).isEqualTo("Tên mới");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/movies/{id}/deactivate", movie.getId())
                        .sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().is3xxRedirection());
        assertThat(movieRepository.findById(movie.getId()).orElseThrow().getActive()).isFalse();
    }
}
