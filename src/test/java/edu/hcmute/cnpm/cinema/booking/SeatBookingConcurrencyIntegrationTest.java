package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test tich hop cho ADR-1: chong dat trung ghe bang rang buoc UNIQUE (showtime_id, seat_id).
 *
 * Day la diem quan trong nhat cua he thong: du 2 (hay 10) nguoi cung bam giu 1 ghe
 * trong cung mot suat chieu, database chi duoc phep nhan DUNG 1 ve.
 *
 * Test chay tren SQL Server that (database cinema_booking_test), KHONG dung database
 * gia lap trong bo nho, vi rang buoc UNIQUE va co che khoa dong la do database lo -
 * dung H2 thi test se khong con chung minh duoc dieu can chung minh.
 *
 * Phu trach: Tho (Module 4 - Testing).
 */
@DisplayName("ADR-1 - Chong dat trung ghe bang rang buoc UNIQUE (showtime_id, seat_id)")
class SeatBookingConcurrencyIntegrationTest extends IntegrationTestBase {

    /** Ket qua cua 1 request: giu duoc ghe. */
    private static final String OUTCOME_HELD = "GIU_DUOC_GHE";
    /** Ket qua cua 1 request: bi database tu choi vi trung ghe. */
    private static final String OUTCOME_REJECTED = "BI_TU_CHOI_TRUNG_GHE";

    private static final int MAX_WAIT_SECONDS = 30;

    private Showtime showtime;
    private Seat seat;

    @BeforeEach
    void prepareOneShowtimeWithOneSeat() {
        Movie movie = testDataFactory.createMovie("Phim kiem thu ADR-1");
        Room room = testDataFactory.createRoom("Phong kiem thu", 1, 1);
        seat = testDataFactory.createSeat(room, "A", 1);
        showtime = testDataFactory.createShowtime(movie, room, LocalDateTime.now().plusDays(1));
    }

    @Test
    @DisplayName("Database phai co rang buoc UNIQUE tren (showtime_id, seat_id)")
    void shouldHaveUniqueConstraintOnShowtimeAndSeat() {
        String countMatchingUniqueIndexes =
                "SELECT COUNT(*) "
                + "FROM sys.indexes i "
                + "WHERE i.object_id = OBJECT_ID('dbo.tickets') "
                + "  AND i.is_unique = 1 "
                + "  AND (SELECT COUNT(*) FROM sys.index_columns ic "
                + "       WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id) = 2 "
                + "  AND EXISTS (SELECT 1 FROM sys.index_columns ic "
                + "              JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id "
                + "              WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id "
                + "                AND c.name = 'showtime_id') "
                + "  AND EXISTS (SELECT 1 FROM sys.index_columns ic "
                + "              JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id "
                + "              WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id "
                + "                AND c.name = 'seat_id')";

        Integer matchingUniqueIndexes = jdbcTemplate.queryForObject(countMatchingUniqueIndexes, Integer.class);

        assertThat(matchingUniqueIndexes)
                .as("Bang tickets phai co UNIQUE (showtime_id, seat_id) - xem ADR-1. "
                        + "Neu test nay do, kiem tra lai @UniqueConstraint trong Ticket.java va database/schema.sql")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dat lai ghe da co nguoi giu (tuan tu) -> bi database tu choi")
    void shouldRejectSecondTicket_whenSeatIsAlreadyHeld() {
        User firstCustomer = testDataFactory.createCustomer("khach.thu.nhat@test.local");
        User secondCustomer = testDataFactory.createCustomer("khach.thu.hai@test.local");

        ticketRepository.saveAndFlush(testDataFactory.newHeldTicket(showtime, seat, firstCustomer));

        assertThatThrownBy(() ->
                ticketRepository.saveAndFlush(testDataFactory.newHeldTicket(showtime, seat, secondCustomer)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Race-condition: 2 request giu cung 1 ghe cung luc -> chi 1 request thanh cong")
    void shouldHoldSeatForOnlyOneRequest_whenTwoRequestsRunAtTheSameTime() throws InterruptedException {
        List<String> outcomes = holdSameSeatConcurrently(2);

        assertThat(outcomes).containsExactlyInAnyOrder(OUTCOME_HELD, OUTCOME_REJECTED);
        assertThat(ticketRepository.count())
                .as("Chi duoc phep ton tai dung 1 ve cho cap (suat chieu, ghe) nay")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Race-condition: 10 request giu cung 1 ghe cung luc -> van chi 1 request thanh cong")
    void shouldHoldSeatForOnlyOneRequest_whenManyRequestsRunAtTheSameTime() throws InterruptedException {
        int concurrentRequests = 10;

        List<String> outcomes = holdSameSeatConcurrently(concurrentRequests);

        assertThat(outcomes)
                .as("Moi request phai ket thuc bang 1 trong 2 ket qua mong doi, khong duoc loi khac")
                .containsOnly(OUTCOME_HELD, OUTCOME_REJECTED);
        assertThat(outcomes).filteredOn(OUTCOME_HELD::equals).hasSize(1);
        assertThat(outcomes).filteredOn(OUTCOME_REJECTED::equals).hasSize(concurrentRequests - 1);
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    /**
     * Cho {@code concurrentRequests} nguoi dung khac nhau cung bam giu DUNG 1 ghe tai cung
     * mot thoi diem, roi tra ve ket qua cua tung request.
     *
     * Cach dong bo: tat ca thread deu dung cho o {@code startSignal}; khi mo chot thi chung
     * cung lao vao INSERT - nho vay moi tai duoc dung tinh huong tranh chap that.
     */
    private List<String> holdSameSeatConcurrently(int concurrentRequests) throws InterruptedException {
        List<User> customers = new ArrayList<>();
        for (int index = 0; index < concurrentRequests; index++) {
            customers.add(testDataFactory.createCustomer("khach.dong.thoi." + index + "@test.local"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<String>> pendingRequests = new ArrayList<>();

        for (User customer : customers) {
            pendingRequests.add(executor.submit(() -> {
                startSignal.await();
                try {
                    ticketRepository.saveAndFlush(testDataFactory.newHeldTicket(showtime, seat, customer));
                    return OUTCOME_HELD;
                } catch (DataIntegrityViolationException ex) {
                    return OUTCOME_REJECTED;
                }
            }));
        }

        startSignal.countDown();
        executor.shutdown();
        boolean allRequestsFinished = executor.awaitTermination(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
        assertThat(allRequestsFinished)
                .as("Cac request giu ghe khong ket thuc trong %d giay (co the bi deadlock)", MAX_WAIT_SECONDS)
                .isTrue();

        List<String> outcomes = new ArrayList<>();
        for (Future<String> request : pendingRequests) {
            try {
                outcomes.add(request.get());
            } catch (ExecutionException ex) {
                outcomes.add("LOI_KHAC: " + ex.getCause());
            }
        }
        return outcomes;
    }
}
