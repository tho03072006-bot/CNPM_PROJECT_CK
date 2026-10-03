package edu.hcmute.cnpm.cinema.staff;

import edu.hcmute.cnpm.cinema.dto.staff.DailyShowtimeBoard;
import edu.hcmute.cnpm.cinema.dto.staff.DailyShowtimeEntry;
import edu.hcmute.cnpm.cinema.dto.staff.RoomDaySchedule;
import edu.hcmute.cnpm.cinema.dto.staff.ShowtimeDayStatus;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.service.DailyShowtimeService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bảng suất chiếu trong ngày của nhân viên. "Bây giờ" truyền vào tay nên kiểm được mọi mốc
 * trong ngày, chạy lúc nào cũng ra cùng kết quả. Phim mẫu dài 120 phút, khoảng nghỉ 15 phút.
 */
@DisplayName("Bảng suất chiếu trong ngày cho nhân viên")
class DailyShowtimeServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DailyShowtimeService dailyShowtimeService;

    private LocalDate day;
    private Room roomA;
    private Room roomB;
    private Showtime morning;

    @BeforeEach
    void createDay() {
        day = LocalDate.now().plusDays(3);
        Movie movie = testDataFactory.createMovie("Phim buổi sáng");
        roomA = testDataFactory.createRoom("Cinema 2", 1, 4);
        roomB = testDataFactory.createRoom("Cinema 5", 1, 4);
        List<Seat> seats = List.of(testDataFactory.createSeat(roomA, "A", 1), testDataFactory.createSeat(roomA, "A", 2),
                testDataFactory.createSeat(roomA, "A", 3), testDataFactory.createSeat(roomA, "A", 4));

        morning = testDataFactory.createShowtime(movie, roomA, at(10, 0));      // hết phim 12:00
        testDataFactory.createShowtime(movie, roomA, at(12, 20));               // nghỉ 20 phút: đủ
        testDataFactory.createShowtime(movie, roomA, at(14, 30));               // nghỉ 10 phút: thiếu (dữ liệu cũ)
        testDataFactory.createShowtime(movie, roomB, at(9, 0));
        testDataFactory.createShowtime(movie, roomB, day.plusDays(1).atTime(10, 0)); // ngày khác, không hiện

        User customer = testDataFactory.createCustomer("khach.ngay@example.com");
        Ticket checkedIn = paidTicket(morning, seats.get(0), customer);
        checkedIn.setCheckedInAt(at(9, 50));
        ticketRepository.save(checkedIn);
        ticketRepository.save(paidTicket(morning, seats.get(1), customer));
        ticketRepository.save(testDataFactory.newHeldTicket(morning, seats.get(2), customer)); // đang giữ: không tính
    }

    @Test
    @DisplayName("Gom theo phòng, suất xếp theo giờ, có giờ hết phim và giờ phòng trống lại")
    void shouldGroupByRoom_withMovieEndAndRoomReadyTime() {
        DailyShowtimeBoard board = dailyShowtimeService.buildBoard(day, null, at(8, 0));

        assertThat(board.rooms()).extracting(RoomDaySchedule::roomName).containsExactly("Cinema 2", "Cinema 5");
        List<DailyShowtimeEntry> roomAEntries = board.rooms().getFirst().entries();
        assertThat(roomAEntries).extracting(DailyShowtimeEntry::start)
                .containsExactly(at(10, 0), at(12, 20), at(14, 30));
        DailyShowtimeEntry first = roomAEntries.getFirst();
        assertThat(first.movieEnd()).isEqualTo(at(12, 0));
        assertThat(first.roomReadyAt()).isEqualTo(at(12, 15));
        assertThat(board.breakMinutes()).isEqualTo(15);
        assertThat(board.getShowtimeCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("Tính khoảng nghỉ tới suất sau và đánh dấu chỗ nghỉ ngắn hơn quy định")
    void shouldMeasureBreakToNextShowtime_andFlagShortBreaks() {
        List<DailyShowtimeEntry> entries = dailyShowtimeService.buildBoard(day, null, at(8, 0))
                .rooms().getFirst().entries();

        assertThat(entries).extracting(DailyShowtimeEntry::minutesToNext).containsExactly(20L, 10L, null);
        assertThat(entries).extracting(DailyShowtimeEntry::isBreakTooShort).containsExactly(false, true, false);
    }

    @Test
    @DisplayName("Đếm vé đã bán và khách đã vào phòng, không tính ghế đang giữ")
    void shouldCountSoldAndCheckedInTickets() {
        DailyShowtimeEntry first = dailyShowtimeService.buildBoard(day, null, at(8, 0))
                .rooms().getFirst().entries().getFirst();

        assertThat(first.soldSeats()).isEqualTo(2);
        assertThat(first.checkedIn()).isEqualTo(1);
        assertThat(first.getWaitingGuests()).isEqualTo(1);
        assertThat(first.seatCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("Trạng thái theo giờ: mở cửa đón khách 15 phút trước, đang chiếu, dọn phòng, đã xong")
    void shouldFollowShowtimeLifecycle_whenTimePasses() {
        assertThat(statusOfMorningAt(9, 0)).isEqualTo(ShowtimeDayStatus.UPCOMING);
        assertThat(statusOfMorningAt(9, 45)).isEqualTo(ShowtimeDayStatus.BOARDING);
        assertThat(statusOfMorningAt(10, 30)).isEqualTo(ShowtimeDayStatus.SHOWING);
        assertThat(statusOfMorningAt(12, 5)).isEqualTo(ShowtimeDayStatus.CLEANING);
        assertThat(statusOfMorningAt(12, 15)).isEqualTo(ShowtimeDayStatus.FINISHED);
    }

    @Test
    @DisplayName("Lọc một phòng thì chỉ hiện phòng đó")
    void shouldShowOnlySelectedRoom_whenFilteringByRoom() {
        DailyShowtimeBoard board = dailyShowtimeService.buildBoard(day, roomB.getId(), at(8, 0));

        assertThat(board.rooms()).singleElement().extracting(RoomDaySchedule::roomName).isEqualTo("Cinema 5");
    }

    @Test
    @DisplayName("Vạch bây giờ chỉ hiện khi đang xem đúng hôm nay")
    void shouldShowNowMarker_onlyForToday() {
        assertThat(dailyShowtimeService.buildBoard(day, null, at(11, 0)).nowLeft()).isNotNull();
        assertThat(dailyShowtimeService.buildBoard(day, null, day.minusDays(1).atTime(11, 0)).nowLeft()).isNull();
    }

    private ShowtimeDayStatus statusOfMorningAt(int hour, int minute) {
        return dailyShowtimeService.buildBoard(day, roomA.getId(), at(hour, minute))
                .rooms().getFirst().entries().stream()
                .filter(entry -> entry.showtimeId().equals(morning.getId()))
                .findFirst().orElseThrow().status();
    }

    private Ticket paidTicket(Showtime showtime, Seat seat, User customer) {
        Ticket ticket = testDataFactory.newHeldTicket(showtime, seat, customer);
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPaidAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    private LocalDateTime at(int hour, int minute) {
        return day.atTime(LocalTime.of(hour, minute));
    }
}
