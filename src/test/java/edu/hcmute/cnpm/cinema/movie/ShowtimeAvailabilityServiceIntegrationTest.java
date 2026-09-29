package edu.hcmute.cnpm.cinema.movie;

import edu.hcmute.cnpm.cinema.dto.schedule.ShowtimeAvailability;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.service.ShowtimeAvailabilityService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShowtimeAvailabilityServiceIntegrationTest extends IntegrationTestBase {
    @Autowired private ShowtimeAvailabilityService availabilityService;

    @Test
    @DisplayName("Tính số ghế còn lại và cảnh báo sắp hết vé")
    void shouldCalculateRemainingSeats_whenTicketsOccupySeats() {
        Movie movie = testDataFactory.createMovie("Phim gần hết vé");
        Room room = testDataFactory.createRoom("Phòng nhỏ", 1, 3);
        Seat seat1 = testDataFactory.createSeat(room, "A", 1);
        Seat seat2 = testDataFactory.createSeat(room, "A", 2);
        Showtime showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(1));
        User customer = testDataFactory.createCustomer("availability@example.com");
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat1, customer));
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat2, customer));

        ShowtimeAvailability availability = availabilityService.findForShowtimes(List.of(showtime))
                .get(showtime.getId());

        assertThat(availability.getTotalSeats()).isEqualTo(3);
        assertThat(availability.getRemainingSeats()).isEqualTo(1);
        assertThat(availability.getOccupancyPercent()).isEqualTo(67);
        assertThat(availability.isAlmostFull()).isTrue();
        assertThat(availability.getLabel()).isEqualTo("Chỉ còn 1 ghế");
    }

    @Test
    @DisplayName("Suất chiếu đủ vé được đánh dấu hết vé")
    void shouldMarkSoldOut_whenEverySeatIsReserved() {
        Movie movie = testDataFactory.createMovie("Phim hết vé");
        Room room = testDataFactory.createRoom("Phòng một ghế", 1, 1);
        Seat seat = testDataFactory.createSeat(room, "A", 1);
        Showtime showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(1));
        User customer = testDataFactory.createCustomer("soldout@example.com");
        ticketRepository.save(testDataFactory.newHeldTicket(showtime, seat, customer));

        ShowtimeAvailability availability = availabilityService.findForShowtimes(List.of(showtime))
                .get(showtime.getId());

        assertThat(availability.isSoldOut()).isTrue();
        assertThat(availability.getLabel()).isEqualTo("Hết vé");
    }
}
