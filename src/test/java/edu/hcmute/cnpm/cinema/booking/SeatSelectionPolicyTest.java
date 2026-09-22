package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.service.SeatSelectionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeatSelectionPolicyTest {
    private BookingTestFixture fixture;
    private SeatSelectionPolicy seatSelectionPolicy;

    @BeforeEach
    void setUp() {
        fixture = new BookingTestFixture();
        seatSelectionPolicy = fixture.seatSelectionPolicy;
    }

    @Test
    @DisplayName("Cho phép chọn các ghế liền nhau mà không tạo ghế trống đơn độc")
    void shouldAcceptSelection_whenSeatsAreAdjacentAndLeaveNoSingleGap() {
        List<Seat> roomSeats = createRow("A", 6, "NORMAL");

        assertThatCode(() -> seatSelectionPolicy.validateSelection(
                roomSeats, roomSeats.subList(2, 4), Set.of(roomSeats.get(4).getId())))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Từ chối các ghế nằm trên hai hàng khác nhau")
    void shouldRejectSelection_whenSeatsAreInDifferentRows() {
        List<Seat> roomSeats = new ArrayList<>(createRow("A", 3, "NORMAL"));
        roomSeats.addAll(createRow("B", 3, "NORMAL", 10L));

        assertThatThrownBy(() -> seatSelectionPolicy.validateSelection(
                roomSeats, List.of(roomSeats.get(0), roomSeats.get(3)), Set.of()))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("cùng một hàng");
    }

    @Test
    @DisplayName("Từ chối lựa chọn bỏ trống ghế ở giữa")
    void shouldRejectSelection_whenSelectedSeatsAreNotAdjacent() {
        List<Seat> roomSeats = createRow("A", 5, "NORMAL");

        assertThatThrownBy(() -> seatSelectionPolicy.validateSelection(
                roomSeats, List.of(roomSeats.get(0), roomSeats.get(2)), Set.of()))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("liền nhau");
    }

    @Test
    @DisplayName("Từ chối lựa chọn tạo thêm một ghế trống đơn độc")
    void shouldRejectSelection_whenItCreatesNewSingleSeatGap() {
        List<Seat> roomSeats = createRow("A", 5, "NORMAL");
        Set<Long> unavailableSeatIds = Set.of(roomSeats.get(3).getId());

        assertThatThrownBy(() -> seatSelectionPolicy.validateSelection(
                roomSeats, List.of(roomSeats.get(1)), unavailableSeatIds))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("trống một mình");
    }

    @Test
    @DisplayName("Không bắt người dùng sửa khoảng trống đã tồn tại từ lượt đặt trước")
    void shouldAcceptSelection_whenSingleSeatGapAlreadyExisted() {
        List<Seat> roomSeats = new ArrayList<>(createRow("A", 2, "NORMAL"));
        roomSeats.addAll(createRow("B", 4, "NORMAL", 10L));
        Set<Long> unavailableSeatIds = new HashSet<>();
        unavailableSeatIds.add(roomSeats.get(1).getId());

        assertThatCode(() -> seatSelectionPolicy.validateSelection(
                roomSeats, List.of(roomSeats.get(2), roomSeats.get(3)), unavailableSeatIds))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Ghế đôi được tính là hai chỗ trong giới hạn tám chỗ")
    void shouldRejectSelection_whenCoupleSeatsExceedAdmissionLimit() {
        List<Seat> roomSeats = createRow("C", 5, "COUPLE");

        assertThatThrownBy(() -> seatSelectionPolicy.validateSelection(
                roomSeats, roomSeats, Set.of()))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("tối đa 8 chỗ");
    }

    private List<Seat> createRow(String seatRow, int seatCount, String seatType) {
        return createRow(seatRow, seatCount, seatType, 1L);
    }

    private List<Seat> createRow(String seatRow, int seatCount, String seatType, long firstSeatId) {
        List<Seat> seats = new ArrayList<>();
        for (int seatColumn = 1; seatColumn <= seatCount; seatColumn++) {
            Seat seat = fixture.testDataFactory.createSeat(fixture.room, seatRow, seatColumn);
            seat.setId(firstSeatId + seatColumn - 1);
            seat.setSeatType(seatType);
            seats.add(seat);
        }
        return seats;
    }
}
