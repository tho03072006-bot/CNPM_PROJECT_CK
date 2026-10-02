package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real HTTP serialization and fresh JPA contexts, without first rendering the seat-map page. */
@AutoConfigureMockMvc
class BookingStateIntegrationTest extends IntegrationTestBase {
    @Autowired private MockMvc mvc;
    private record Fixture(Showtime showtime, List<Seat> seats, User customer) {}
    private Fixture fixture(String rating) {
        Movie movie = testDataFactory.createMovie("Phim mới " + rating);
        movie.setAgeRating(rating); movieRepository.saveAndFlush(movie);
        Room room = testDataFactory.createRoom("Cinema 1",1,4);
        List<Seat> seats = new ArrayList<>();
        for(int column=1;column<=4;column++) seats.add(testDataFactory.createSeat(room,"A",column));
        Showtime showtime = testDataFactory.createShowtime(movie,room,LocalDateTime.now().plusDays(1));
        return new Fixture(showtime,seats,testDataFactory.createCustomer("state@test.local"));
    }
    @ParameterizedTest
    @ValueSource(strings={"P","K","T13","T16","T18","C13","C16","C18"," t13 "})
    @DisplayName("Phim mới ở từng phân loại trả trạng thái ghế từ request đầu tiên")
    void shouldReturnStateWithoutOpeningPage_whenNewMovieHasSupportedRating(String rating) throws Exception {
        Fixture data = fixture(rating);
        mvc.perform(get("/booking/showtime/{id}/state",data.showtime().getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control",containsString("no-store")))
                .andExpect(jsonPath("$.seatMap.movieTitle").value("Phim mới " + rating))
                .andExpect(jsonPath("$.seatMap.seats",hasSize(4))).andExpect(jsonPath("$.bookingOpen").value(true))
                .andExpect(jsonPath("$.serverTimeMillis").isNumber()).andExpect(jsonPath("$.activeHold").isEmpty());
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @ValueSource(strings={"UNKNOWN","C"})
    @DisplayName("Thiếu hoặc sai phân loại trả lỗi nghiệp vụ rõ ràng, không trả 500")
    void shouldReturnBusinessError_whenMovieRatingIsNotBookable(String rating) throws Exception {
        Fixture data = fixture(rating);
        mvc.perform(get("/booking/showtime/{id}/state",data.showtime().getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
    @Test @DisplayName("Phim ngừng chiếu vẫn trả trạng thái nhưng đóng nút đặt mới")
    void shouldCloseBooking_whenMovieIsInactive() throws Exception {
        Fixture data = fixture("P");
        Movie movie=movieRepository.findById(data.showtime().getMovie().getId()).orElseThrow();
        movie.setActive(false);movieRepository.saveAndFlush(movie);
        mvc.perform(get("/booking/showtime/{id}/state",data.showtime().getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bookingOpen").value(false))
                .andExpect(jsonPath("$.paymentOpen").value(false));
    }
    @Test @DisplayName("Suất đã bắt đầu vẫn cập nhật sơ đồ mà không trả lỗi 500")
    void shouldReturnClosedState_whenShowtimeHasStarted() throws Exception {
        Fixture data = fixture("K");
        Showtime old=showtimeRepository.findById(data.showtime().getId()).orElseThrow();
        old.setStartTime(LocalDateTime.now().minusHours(1));showtimeRepository.saveAndFlush(old);
        mvc.perform(get("/booking/showtime/{id}/state",old.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bookingOpen").value(false))
                .andExpect(jsonPath("$.paymentOpen").value(false));
    }
    @ParameterizedTest
    @ValueSource(strings={"P","K","T13","T16","T18"," c13 "})
    @DisplayName("Thêm phim qua trang quản trị rồi đọc trạng thái suất mới thành công")
    void shouldReturnState_whenAdminAddsAnotherMovie(String rating) throws Exception {
        User admin=new User();admin.setRole(Role.ADMIN);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/movies")
                        .sessionAttr(Constants.SESSION_USER,admin).param("title","Phim vừa thêm")
                        .param("durationMin","100").param("ageRating",rating))
                .andExpect(status().is3xxRedirection());
        Movie movie=movieRepository.findAll().getFirst();
        Room room=testDataFactory.createRoom("Cinema 2",1,1);testDataFactory.createSeat(room,"A",1);
        Showtime showtime=testDataFactory.createShowtime(movie,room,LocalDateTime.now().plusDays(1));
        mvc.perform(get("/booking/showtime/{id}/state",showtime.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bookingOpen").value(true))
                .andExpect(jsonPath("$.seatMap.movieTitle").value("Phim vừa thêm"));
    }
    @Test @DisplayName("Khách đăng nhập đọc trạng thái ghế trong request riêng")
    void shouldRestoreHoldFromFreshRequest_whenCustomerIsLoggedIn() throws Exception {
        Fixture data = fixture("K");
        Ticket held = ticketRepository.saveAndFlush(testDataFactory.newHeldTicket(data.showtime(),data.seats().getFirst(),data.customer()));
        mvc.perform(get("/booking/showtime/{id}/state",data.showtime().getId()).accept(MediaType.APPLICATION_JSON)
                        .sessionAttr(Constants.SESSION_USER,data.customer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activeHold.ticketIds[0]").value(held.getId()))
                .andExpect(jsonPath("$.activeHold.expiresAtMillis").isNumber())
                .andExpect(jsonPath("$.seatMap.seats[0].status").value("HELD"));
    }
    @Test @DisplayName("Dọn vé hết hạn vẫn đọc được thông tin phim và phòng trong cùng request")
    void shouldReturnAvailableState_whenExpiredTicketWasDeleted() throws Exception {
        Fixture data = fixture("T18");
        Ticket expired = testDataFactory.newHeldTicket(data.showtime(),data.seats().getFirst(),data.customer());
        expired.setHeldAt(LocalDateTime.now().minusMinutes(6));ticketRepository.saveAndFlush(expired);
        mvc.perform(get("/booking/showtime/{id}/state",data.showtime().getId()).accept(MediaType.APPLICATION_JSON)
                        .sessionAttr(Constants.SESSION_USER,data.customer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activeHold").isEmpty())
                .andExpect(jsonPath("$.seatMap.seats[0].status").value("AVAILABLE"));
        assertThat(ticketRepository.findById(expired.getId())).isEmpty();
    }
    @Test @DisplayName("Gợi ý ghế cho phim mới từ request độc lập")
    void shouldSuggestSeatsWithoutOpeningPage_whenMovieWasJustAdded() throws Exception {
        Fixture data = fixture("P");
        mvc.perform(get("/booking/showtime/{id}/suggestions",data.showtime().getId()).accept(MediaType.APPLICATION_JSON)
                        .param("admissions","2").param("seatType","ANY").param("maxBudget","150000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.seatIds",hasSize(2)))
                .andExpect(jsonPath("$.admissions").value(2));
    }
}
