package edu.hcmute.cnpm.cinema.movie;

import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.RoomService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomServiceIntegrationTest extends IntegrationTestBase {
    @Autowired private RoomService roomService;

    @Test
    @DisplayName("Phòng 8 hàng 10 cột sinh 80 ghế, hai hàng cuối là ghế VIP")
    void shouldGenerateEightySeatsWithLastTwoRowsVip_whenCreatingRoom() {
        Room room = roomService.createRoom("Phòng mới", 8, 10);

        assertThat(seatRepository.findByRoomId(room.getId())).hasSize(80)
                .filteredOn(seat -> "VIP".equals(seat.getSeatType())).hasSize(20)
                .extracting(Seat::getSeatRow).containsOnly("G", "H");
    }

    @Test
    @DisplayName("Sinh ghế lần nữa không tạo ghế trùng")
    void shouldNotDuplicateSeats_whenGeneratingAgain() {
        Room room = roomService.createRoom("Phòng mới", 8, 10);
        roomService.generateSeats(room.getId());
        assertThat(seatRepository.findByRoomId(room.getId())).hasSize(80);
    }

    @Test
    @DisplayName("Từ chối phòng có số hàng không hợp lệ")
    void shouldRejectInvalidDimensions_whenCreatingRoom() {
        assertThatThrownBy(() -> roomService.createRoom("Phòng lỗi", 0, 10))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Cho đổi sơ đồ ghế khi phòng chưa có suất chiếu")
    void shouldAllowResizingRoom_whenWithoutShowtimes() {
        Room room = roomService.createRoom("Phòng mới", 8, 10);
        roomService.updateRoom(room.getId(), "Phòng mới", 4, 5);
        assertThat(seatRepository.findByRoomId(room.getId())).hasSize(20);
    }
}
