package edu.hcmute.cnpm.cinema.movie;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.service.ShowtimeService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Quy tắc khoảng nghỉ giữa hai suất liên tiếp trong cùng phòng (mặc định 15 phút).
 * Phim mẫu dài 120 phút: suất 10:00 hết phim lúc 12:00, phòng trống lại lúc 12:15.
 */
@DisplayName("Khoảng nghỉ giữa hai suất trong cùng phòng")
class ShowtimeBreakRuleIntegrationTest extends IntegrationTestBase {

    private static final BigDecimal PRICE = new BigDecimal("75000");

    @Autowired
    private ShowtimeService showtimeService;

    private Movie movie;
    private Room room;
    private LocalDateTime tenAm;

    @BeforeEach
    void createExistingShowtime() {
        movie = testDataFactory.createMovie("Phim xếp lịch");
        room = testDataFactory.createRoom("Phòng 2", 5, 8);
        tenAm = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        showtimeService.createShowtime(movie.getId(), room.getId(), tenAm, PRICE);
    }

    @Test
    @DisplayName("Thiếu một phút nghỉ là bị từ chối, lời nhắc nêu khoảng nghỉ và giờ sớm nhất được bắt đầu")
    void shouldRejectAndSuggestEarliestStart_whenBreakIsOneMinuteShort() {
        assertThatThrownBy(() -> showtimeService.createShowtime(movie.getId(), room.getId(), tenAm.plusMinutes(134), PRICE))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("Giờ chiếu trùng với suất chiếu")
                .hasMessageContaining("nghỉ ít nhất 15 phút")
                .hasMessageContaining("sớm nhất bắt đầu lúc")
                .hasMessageContaining("12:15");
    }

    @Test
    @DisplayName("Suất mới đặt TRƯỚC suất cũ cũng phải chừa đủ khoảng nghỉ trước giờ suất cũ bắt đầu")
    void shouldRequireBreakBeforeExistingShowtime_whenNewOneComesFirst() {
        // Suất mới 07:50 hết phim 09:50, chỉ còn 10 phút trước suất 10:00.
        assertThatThrownBy(() -> showtimeService.createShowtime(movie.getId(), room.getId(), tenAm.minusMinutes(130), PRICE))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("kết thúc trước 09:45");

        Showtime earlier = showtimeService.createShowtime(movie.getId(), room.getId(), tenAm.minusMinutes(135), PRICE);
        assertThat(earlier.getEndTime()).isEqualTo(tenAm);
    }

    @Test
    @DisplayName("Suất cũ lưu theo khoảng nghỉ ngắn hơn vẫn bị so theo khoảng nghỉ hiện hành")
    void shouldApplyCurrentBreak_whenExistingShowtimeWasSavedWithShorterBreak() {
        // Dữ liệu cũ: giờ kết thúc lưu đúng giờ hết phim, không có khoảng nghỉ.
        Room oldRoom = testDataFactory.createRoom("Phòng cũ", 5, 8);
        testDataFactory.createShowtime(movie, oldRoom, tenAm);

        assertThatThrownBy(() -> showtimeService.createShowtime(movie.getId(), oldRoom.getId(), tenAm.plusMinutes(125), PRICE))
                .isInstanceOf(InvalidBookingException.class);
        assertThat(showtimeService.createShowtime(movie.getId(), oldRoom.getId(), tenAm.plusMinutes(135), PRICE).getId())
                .isNotNull();
    }

    @Test
    @DisplayName("Sửa giờ một suất thì không bị chính nó chặn")
    void shouldNotConflictWithItself_whenUpdatingShowtime() {
        Showtime later = showtimeService.createShowtime(movie.getId(), room.getId(), tenAm.plusHours(4), PRICE);

        Showtime moved = showtimeService.updateShowtime(later.getId(), movie.getId(), room.getId(),
                tenAm.plusHours(4).plusMinutes(30), PRICE);

        assertThat(moved.getStartTime()).isEqualTo(tenAm.plusHours(4).plusMinutes(30));
    }
}
