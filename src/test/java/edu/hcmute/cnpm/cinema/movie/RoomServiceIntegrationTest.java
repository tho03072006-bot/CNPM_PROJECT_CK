package edu.hcmute.cnpm.cinema.movie;

import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.RoomService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomServiceIntegrationTest extends IntegrationTestBase {
    @Autowired private RoomService roomService;

    @Test
    void shouldGenerateEightySeatsWithLastTwoRowsVipWhenCreatingRoom() {
        Room room = roomService.createRoom("Phòng mới", 8, 10);

        assertThat(seatRepository.findByRoomId(room.getId())).hasSize(80)
                .filteredOn(seat -> "VIP".equals(seat.getSeatType())).hasSize(20)
                .extracting(Seat::getSeatRow).containsOnly("G", "H");
    }

    @Test
    void shouldNotDuplicateSeatsWhenGeneratingAgain() {
        Room room = roomService.createRoom("Phòng mới", 8, 10);
        roomService.generateSeats(room.getId());
        assertThat(seatRepository.findByRoomId(room.getId())).hasSize(80);
    }

    @Test
    void shouldRejectInvalidDimensionsWhenCreatingRoom() {
        assertThatThrownBy(() -> roomService.createRoom("Phòng lỗi", 0, 10))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldAllowResizingRoomWithoutShowtimes() {
        Room room = roomService.createRoom("Phòng mới", 8, 10);
        roomService.updateRoom(room.getId(), "Phòng mới", 4, 5);
        assertThat(seatRepository.findByRoomId(room.getId())).hasSize(20);
    }
}
