package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.dto.booking.HoldSeatsRequest;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.SeatAlreadyTakenException;
import edu.hcmute.cnpm.cinema.service.SeatBookingService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Kiểm tra giao dịch thật của Module 2 trên SQL Server riêng cho test. */
class SeatBookingServiceIntegrationTest extends IntegrationTestBase {
    private static final int MAX_WAIT_SECONDS = 30;

    @Autowired
    private SeatBookingService seatBookingService;
    private Showtime showtime;
    private Seat firstSeat;
    private Seat secondSeat;
    private User firstCustomer;
    private User secondCustomer;

    @BeforeEach
    void setUpBookingData() {
        Movie movie = testDataFactory.createMovie("Phim kiểm thử Module 2");
        Room room = testDataFactory.createRoom("Phòng kiểm thử Module 2", 1, 2);
        firstSeat = testDataFactory.createSeat(room, "A", 1);
        secondSeat = testDataFactory.createSeat(room, "A", 2);
        showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(1));
        firstCustomer = testDataFactory.createCustomer("module2.first@test.local");
        secondCustomer = testDataFactory.createCustomer("module2.second@test.local");
    }

    @Test
    @DisplayName("Hai khách đồng thời giữ cùng ghế: đúng một thành công, một lỗi tranh chấp")
    void shouldHoldSeatForOnlyOneCustomer_whenRequestsRunConcurrently() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readySignal = new CountDownLatch(2);
        CountDownLatch startSignal = new CountDownLatch(1);
        try {
            Future<Boolean> firstResult = executor.submit(() -> holdSeatAfterSignal(firstCustomer, readySignal, startSignal));
            Future<Boolean> secondResult = executor.submit(() -> holdSeatAfterSignal(secondCustomer, readySignal, startSignal));
            assertThat(readySignal.await(MAX_WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            startSignal.countDown();
            assertThat(List.of(firstResult.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS),
                    secondResult.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(ticketRepository.count()).isEqualTo(1);
        } finally {
            startSignal.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(MAX_WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("Ghế sau bị tranh chấp thì rollback cả ghế trước trong cùng yêu cầu")
    void shouldRollBackAllNewTickets_whenOneSeatIsAlreadyTaken() {
        Ticket existingTicket = ticketRepository.saveAndFlush(
                testDataFactory.newHeldTicket(showtime, secondSeat, secondCustomer));

        assertThatThrownBy(() -> seatBookingService.holdSeats(showtime.getId(),
                createRequest(List.of(firstSeat.getId(), secondSeat.getId())), firstCustomer))
                .isInstanceOf(SeatAlreadyTakenException.class);

        assertThat(ticketRepository.findAll()).extracting(Ticket::getId).containsExactly(existingTicket.getId());
    }

    @Test
    @DisplayName("Hai ghế hợp lệ được ghi đủ trong một giao dịch")
    void shouldSaveAllTickets_whenSeatsAreAvailable() {
        var response = seatBookingService.holdSeats(showtime.getId(),
                createRequest(List.of(firstSeat.getId(), secondSeat.getId())), firstCustomer);
        assertThat(response.getTicketIds()).hasSize(2);
        assertThat(response.getTotalPrice()).isEqualByComparingTo("150000.00");
        assertThat(ticketRepository.count()).isEqualTo(2);
    }

    private boolean holdSeatAfterSignal(User customer, CountDownLatch readySignal,
                                        CountDownLatch startSignal) throws InterruptedException {
        readySignal.countDown();
        if (!startSignal.await(MAX_WAIT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Hết thời gian chờ bắt đầu kiểm thử tranh chấp.");
        }
        try {
            seatBookingService.holdSeats(showtime.getId(), createRequest(List.of(firstSeat.getId())), customer);
            return true;
        } catch (SeatAlreadyTakenException exception) {
            return false;
        }
    }

    private HoldSeatsRequest createRequest(List<Long> seatIds) {
        HoldSeatsRequest request = new HoldSeatsRequest();
        request.setSeatIds(seatIds);
        return request;
    }
}
