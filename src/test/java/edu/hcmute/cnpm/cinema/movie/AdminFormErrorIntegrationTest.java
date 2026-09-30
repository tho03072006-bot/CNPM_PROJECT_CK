package edu.hcmute.cnpm.cinema.movie;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Role;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Lỗi nghiệp vụ ở khu quản trị phải hiện ngay trên form hoặc danh sách, không đá quản trị
 * viên sang trang lỗi và làm mất dữ liệu vừa nhập.
 */
@AutoConfigureMockMvc
@DisplayName("Báo lỗi nghiệp vụ ngay trên trang quản trị")
class AdminFormErrorIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private User admin;
    private Movie movie;
    private Room room;
    private Showtime existing;

    @BeforeEach
    void setUp() {
        admin = testDataFactory.createUserWithRole("admin@example.com", Role.ADMIN);
        movie = testDataFactory.createMovie("Phim quản trị");
        room = testDataFactory.createRoom("Cinema 8", 2, 4);
        existing = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(3).withHour(19).withMinute(0).withSecond(0).withNano(0));
    }

    @Test
    @DisplayName("Xếp suất cho phim đã ngừng chiếu thì hiện lại form kèm lời nhắc, dữ liệu vừa nhập vẫn còn")
    void shouldRedisplayShowtimeForm_whenMovieIsNoLongerShowing() throws Exception {
        // Lỗi trùng giờ thì nằm dưới ô giờ bắt đầu (MoviePagesIntegrationTest của Module 1 kiểm tra);
        // ở đây là các lỗi nghiệp vụ còn lại, hiện ở đầu form.
        movie.setActive(false);
        movieRepository.save(movie);
        String nextDay = existing.getStartTime().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        mockMvc.perform(post("/admin/showtimes").sessionAttr(Constants.SESSION_USER, admin)
                        .param("movieId", movie.getId().toString())
                        .param("roomId", room.getId().toString())
                        .param("startTime", nextDay)
                        .param("basePrice", "115000"))
                .andExpect(status().isOk())
                .andExpect(view().name("movie/showtime-form"))
                .andExpect(model().attribute(Constants.MODEL_ERROR_MESSAGE, containsString("ngừng chiếu")))
                .andExpect(model().attributeExists("movies", "rooms"));

        assertThat(showtimeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Xoá phòng đang có suất chiếu thì quay về danh sách phòng kèm lời nhắc")
    void shouldReturnToRoomListWithMessage_whenRoomHasShowtimes() throws Exception {
        mockMvc.perform(post("/admin/rooms/{id}/delete", room.getId()).sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/rooms"))
                .andExpect(flash().attribute(Constants.MODEL_ERROR_MESSAGE, containsString("đang có suất chiếu")));

        assertThat(roomRepository.findById(room.getId())).isPresent();
    }

    @Test
    @DisplayName("Xoá suất chiếu đã bán vé thì quay về danh sách suất chiếu kèm lời nhắc")
    void shouldReturnToShowtimeListWithMessage_whenShowtimeHasTickets() throws Exception {
        User customer = testDataFactory.createCustomer("khach@example.com");
        ticketRepository.save(testDataFactory.newHeldTicket(existing, testDataFactory.createSeat(room, "A", 1), customer));

        mockMvc.perform(post("/admin/showtimes/{id}/delete", existing.getId()).sessionAttr(Constants.SESSION_USER, admin))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/showtimes"))
                .andExpect(flash().attribute(Constants.MODEL_ERROR_MESSAGE, containsString("đã có vé")));

        assertThat(showtimeRepository.findById(existing.getId())).isPresent();
    }
}
