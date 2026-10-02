package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.DemoWalletSessions;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.service.*;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Creates movie/showtime through admin HTTP; no pre-existing tickets or cached page. */
@SpringBootTest(properties = {"demo-wallet.enabled=true", "demo-wallet.public-base-url=https://demo.group8.test"})
@AutoConfigureMockMvc
class NewShowtimeBookingIntegrationTest extends IntegrationTestBase {
    @Autowired private MockMvc mvc;
    @Autowired private RoomService rooms;
    @Autowired private BookingClock clock;
    @Autowired private DemoWalletSessions sessions;

    @ParameterizedTest
    @ValueSource(strings = {"P", "K", "T13", "T16", "T18"})
    void shouldPayNewMovieAndShowtimeWithOddVipPrice(String rating) throws Exception {
        User admin = testDataFactory.createUserWithRole("new.admin@test.local", Role.ADMIN);
        mvc.perform(post("/admin/movies").sessionAttr(Constants.SESSION_USER, admin)
                        .param("title", "Phim mới " + rating).param("durationMin", "100").param("ageRating", rating))
                .andExpect(redirectedUrl("/admin/movies"));
        Movie movie = movieRepository.findAll().getFirst();
        Room room = rooms.createRoom("Cinema mới", 3, 4);
        mvc.perform(post("/admin/showtimes").sessionAttr(Constants.SESSION_USER, admin)
                        .param("movieId", movie.getId().toString()).param("roomId", room.getId().toString())
                        .param("startTime", clock.now().plusDays(1).withSecond(0).withNano(0).toString())
                        .param("basePrice", "75001"))
                .andExpect(redirectedUrl("/admin/showtimes"));
        Showtime showtime = showtimeRepository.findAll().getFirst();
        mvc.perform(get("/booking/showtime/{id}/state", showtime.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bookingOpen").value(true))
                .andExpect(jsonPath("$.seatMap.seats", hasSize(12)))
                .andExpect(jsonPath("$.seatMap.seats[4].price").value(112502));
        List<Seat> selected = seatRepository.findByRoomIdOrderBySeatRowAscSeatColumnAsc(room.getId())
                .stream().filter(seat -> "B".equals(seat.getSeatRow()) && seat.getSeatColumn() <= 2).toList();
        User user = testDataFactory.createCustomer("new.customer@test.local");
        MockHttpSession laptop = new MockHttpSession();
        laptop.setAttribute(Constants.SESSION_USER, user);
        mvc.perform(post("/booking/showtime/{id}/hold", showtime.getId()).session(laptop)
                        .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\":[" + selected.get(0).getId() + "," + selected.get(1).getId()
                                + "],\"ageConfirmed\":true,\"termsAccepted\":true}"))
                .andExpect(status().isOk());
        List<Ticket> held = ticketRepository.findAll();
        assertThat(held).hasSize(2).allSatisfy(ticket -> assertThat(ticket.getPrice()).isEqualByComparingTo("112502"));
        String qr = mvc.perform(post("/thanh-toan/{id}/demo-wallet", showtime.getId()).session(laptop)
                        .param("ticketIds", held.stream().map(ticket -> ticket.getId().toString()).toArray(String[]::new))
                        .param("walletCsrf", sessions.csrf(laptop)))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        String publicId = qr.substring(qr.lastIndexOf('/') + 1);
        var issued = sessions.find(laptop, publicId);
        MockHttpSession phone = new MockHttpSession();
        mvc.perform(post("/demo-wallet/api/{id}/confirm", publicId).session(phone)
                        .header("X-Demo-Wallet-CSRF", sessions.csrf(phone)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + issued.token() + "\",\"expectedAmount\":225004,\"confirmed\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        mvc.perform(get(qr + "/status").session(laptop).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        assertThat(ticketRepository.findAll()).allMatch(ticket -> ticket.getStatus() == TicketStatus.PAID
                && ticket.getPaymentMethod() == PaymentMethod.MOMO_DEMO);
        assertThat(bookingOrderRepository.findAll()).singleElement()
                .satisfies(order -> assertThat(order.getTotalAmount()).isEqualByComparingTo("225004"));
    }
}
