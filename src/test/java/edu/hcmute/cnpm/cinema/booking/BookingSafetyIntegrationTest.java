package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.dto.booking.*;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.*;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import edu.hcmute.cnpm.cinema.service.*;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Only run on a dedicated SQL Server *_test database; inherits the cleanup guard. */
class BookingSafetyIntegrationTest extends IntegrationTestBase {
    @Autowired private SeatBookingService booking;
    @Autowired private SeatHoldService holds;
    @Autowired private PaymentService payments;
    @MockitoSpyBean private TicketRepository savingTickets;
    private Showtime showtime;
    private User first, second;
    private List<Seat> seats;
    @BeforeEach
    void prepareRoom() {
        Movie movie = testDataFactory.createMovie("Phim kiểm thử tranh chấp");
        Room room = testDataFactory.createRoom("Room test",1,6);
        showtime = testDataFactory.createShowtime(movie,room,LocalDateTime.now().plusDays(1));
        seats = new ArrayList<>();
        for(int i=1;i<=6;i++) seats.add(testDataFactory.createSeat(room,"A",i));
        first = testDataFactory.createCustomer("safety.first@test.local");
        second = testDataFactory.createCustomer("safety.second@test.local");
    }
    private HoldSeatsRequest request(List<Seat> selected) {
        HoldSeatsRequest request = new HoldSeatsRequest();
        request.setSeatIds(selected.stream().map(Seat::getId).toList());
        request.setAgeConfirmed(true);request.setTermsAccepted(true);return request;
    }
    private List<Boolean> race(Callable<Boolean> firstAction, Callable<Boolean> secondAction) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for(Callable<Boolean> action:List.of(firstAction,secondAction))
                futures.add(executor.submit(()->{ready.countDown();if(!start.await(30,TimeUnit.SECONDS))
                    throw new IllegalStateException("Timeout");return action.call();}));
            assertThat(ready.await(30,TimeUnit.SECONDS)).isTrue();start.countDown();
            return List.of(futures.get(0).get(30,TimeUnit.SECONDS),futures.get(1).get(30,TimeUnit.SECONDS));
        } finally {start.countDown();executor.shutdownNow();assertThat(executor.awaitTermination(30,TimeUnit.SECONDS)).isTrue();}
    }
    @Test @DisplayName("Hai nhóm khác ghế không được đồng thời tạo một chỗ lẻ")
    void shouldSerializeSelectionPolicy_whenDifferentCustomersBookDisjointGroups() throws Exception {
        List<Boolean> result = race(()->tryHold(first,seats.subList(0,2)),()->tryHold(second,seats.subList(3,6)));
        assertThat(result).containsExactlyInAnyOrder(true,false);
        assertThat(ticketRepository.count()).isIn(2L,3L);
    }
    private boolean tryHold(User user,List<Seat> selected) {
        try {booking.holdSeats(showtime.getId(),request(selected),user);return true;}
        catch(InvalidBookingException rejected){return false;}
    }
    @Test @DisplayName("Huỷ và thanh toán cùng lúc chỉ có một bên thắng")
    void shouldNeverDeletePaidTickets_whenPaymentAndCancellationRace() throws Exception {
        HoldSeatsResponse hold=booking.holdSeats(showtime.getId(),request(seats.subList(0,2)),first);
        List<Boolean> result=race(()->{
            try{payments.confirmCounterPayment(first.getId(),showtime.getId(),hold.getTicketIds());return true;}
            catch(InvalidBookingException rejected){return false;}
        },()->{
            try{holds.cancelHold(first.getId(),showtime.getId(),hold.getTicketIds());return true;}
            catch(InvalidBookingException rejected){return false;}
        });
        assertThat(result).containsExactlyInAnyOrder(true,false);
        List<Ticket> remaining=ticketRepository.findAll();
        assertThat(remaining).allMatch(ticket->ticket.getStatus()==TicketStatus.PAID);
        assertThat(remaining.size()).isIn(0,2);
    }
    @Test @DisplayName("Ghi ghế mới thất bại phải rollback và giữ nguyên vé cũ")
    void shouldRestoreOldTickets_whenReplacementInsertFails() {
        HoldSeatsResponse original=booking.holdSeats(showtime.getId(),request(seats.subList(0,2)),first);
        HoldSeatsRequest replacement=request(seats.subList(4,6));replacement.setExpectedTicketIds(original.getTicketIds());
        doThrow(new DataIntegrityViolationException("simulated unique conflict"))
                .when(savingTickets).saveAndFlush(any(Ticket.class));
        assertThatThrownBy(()->booking.holdSeats(showtime.getId(),replacement,first)).isInstanceOf(SeatAlreadyTakenException.class);
        assertThat(ticketRepository.findAll()).extracting(Ticket::getId).containsExactlyInAnyOrderElementsOf(original.getTicketIds());
    }
    @Test @DisplayName("Đổi thành công không gia hạn, tab cũ không huỷ được lượt mới")
    void shouldPreserveDeadlineAndRejectOldTab_whenReplacementSucceeds() {
        HoldSeatsResponse original=booking.holdSeats(showtime.getId(),request(seats.subList(0,2)),first);
        HoldSeatsRequest replacement=request(seats.subList(4,6));replacement.setExpectedTicketIds(original.getTicketIds());
        HoldSeatsResponse changed=booking.holdSeats(showtime.getId(),replacement,first);
        assertThat(changed.getExpiresAt()).isCloseTo(original.getExpiresAt(),within(1,ChronoUnit.MILLIS));
        assertThat(changed.getTicketIds()).doesNotContainAnyElementsOf(original.getTicketIds());
        assertThatThrownBy(()->holds.cancelHold(first.getId(),showtime.getId(),original.getTicketIds()))
                .isInstanceOf(InvalidBookingException.class);
        assertThat(ticketRepository.findAll()).extracting(Ticket::getId).containsExactlyInAnyOrderElementsOf(changed.getTicketIds());
    }
}
