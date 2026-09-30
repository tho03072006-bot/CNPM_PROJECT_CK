package edu.hcmute.cnpm.cinema.booking;

import edu.hcmute.cnpm.cinema.dto.booking.SeatMapView;
import edu.hcmute.cnpm.cinema.dto.booking.SeatRowView;
import edu.hcmute.cnpm.cinema.dto.booking.SeatTypeOption;
import edu.hcmute.cnpm.cinema.dto.booking.SeatView;
import edu.hcmute.cnpm.cinema.entity.RoomType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Dữ liệu hiển thị sơ đồ ghế")
class SeatMapViewTest {

    @Test
    @DisplayName("Gom ghế theo hàng đúng thứ tự, mỗi hàng biết dòng của mình trên lưới")
    void shouldGroupSeatsByRow_inOrder() {
        SeatMapView seatMap = seatMap("Cinema 3", List.of(
                seat(1L, "A", 1, "NORMAL", "115000", "AVAILABLE"),
                seat(2L, "A", 2, "NORMAL", "115000", "PAID"),
                seat(3L, "B", 1, "VIP", "172500", "AVAILABLE"),
                seat(4L, "C", 1, "COUPLE", "230000", "HELD")));

        List<SeatRowView> rows = seatMap.getRows();

        assertThat(rows).extracting(SeatRowView::rowLabel).containsExactly("A", "B", "C");
        assertThat(rows).extracting(SeatRowView::gridRow).containsExactly(1, 2, 3);
        assertThat(rows.getFirst().seats()).extracting(SeatView::getId).containsExactly(1L, 2L);
        assertThat(seatMap.getAvailableSeatCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Chú thích chỉ liệt kê loại ghế có trong phòng, từ rẻ tới đắt, kèm giá của suất")
    void shouldListSeatTypesInRoom_withPriceOfThisShowtime() {
        SeatMapView seatMap = seatMap("Cinema 3", List.of(
                seat(1L, "A", 1, "COUPLE", "230000", "AVAILABLE"),
                seat(2L, "B", 1, "NORMAL", "115000", "AVAILABLE"),
                seat(3L, "B", 2, "NORMAL", "115000", "AVAILABLE")));

        List<SeatTypeOption> options = seatMap.getSeatTypeOptions();

        assertThat(options).extracting(SeatTypeOption::code).containsExactly("NORMAL", "COUPLE");
        assertThat(options).extracting(SeatTypeOption::seatCount).containsExactly(2, 1);
        assertThat(options.get(1).price()).isEqualByComparingTo("230000");
        assertThat(options.get(1).capacity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Nhận ra loại phòng từ tên phòng")
    void shouldResolveRoomType_fromRoomName() {
        assertThat(seatMap("Cinema 8 - GOLD CLASS", List.of()).getRoomType()).isEqualTo(RoomType.GOLD);
        assertThat(seatMap("Cinema 7 - PREMIUM", List.of()).getRoomType()).isEqualTo(RoomType.PREMIUM);
        assertThat(seatMap("Cinema 3", List.of()).getRoomType()).isEqualTo(RoomType.STANDARD);
        assertThat(RoomType.fromRoomName(null)).isEqualTo(RoomType.STANDARD);
    }

    @Test
    @DisplayName("Lưới nới rộng khi có ghế nằm ngoài số cột khai báo của phòng")
    void shouldWidenGrid_whenSeatColumnExceedsRoomColumns() {
        SeatMapView seatMap = new SeatMapView(1L, "Phim", "Cinema 1", LocalDateTime.now().plusDays(1), 2,
                List.of(seat(1L, "A", 3, "NORMAL", "115000", "AVAILABLE")));

        assertThat(seatMap.getGridColumns()).isEqualTo(3);
    }

    private SeatMapView seatMap(String roomName, List<SeatView> seats) {
        return new SeatMapView(1L, "Phim kiểm thử", roomName, LocalDateTime.now().plusDays(1), 14, seats);
    }

    private SeatView seat(Long id, String row, int column, String type, String price, String status) {
        return new SeatView(id, row, column, type, new BigDecimal(price), status);
    }
}
