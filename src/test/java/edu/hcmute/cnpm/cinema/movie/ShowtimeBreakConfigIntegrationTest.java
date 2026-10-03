package edu.hcmute.cnpm.cinema.movie;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.service.ShowtimeService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Nhóm đổi khoảng nghỉ (ví dụ 10 phút như đề bài gợi ý) thì chỉ sửa app.showtime.break-minutes,
 * phép kiểm tra lịch và giờ kết thúc lưu xuống đổi theo.
 */
@TestPropertySource(properties = "app.showtime.break-minutes=10")
@DisplayName("Đổi khoảng nghỉ bằng cấu hình")
class ShowtimeBreakConfigIntegrationTest extends IntegrationTestBase {

    private static final BigDecimal PRICE = new BigDecimal("75000");

    @Autowired
    private ShowtimeService showtimeService;

    @Test
    @DisplayName("Đặt 10 phút: phòng 2 hết phim lúc 12:00 thì suất sau sớm nhất là 12:10")
    void shouldFollowConfiguredBreak_whenBreakIsTenMinutes() {
        Movie movie = testDataFactory.createMovie("Phim mười phút nghỉ");
        Room room = testDataFactory.createRoom("Phòng 2", 5, 8);
        LocalDateTime tenAm = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Showtime first = showtimeService.createShowtime(movie.getId(), room.getId(), tenAm, PRICE);

        assertThat(showtimeService.getBreakMinutes()).isEqualTo(10);
        assertThat(first.getEndTime()).isEqualTo(tenAm.plusMinutes(130));
        assertThatThrownBy(() -> showtimeService.createShowtime(movie.getId(), room.getId(), tenAm.plusMinutes(129), PRICE))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("nghỉ ít nhất 10 phút");
        assertThat(showtimeService.createShowtime(movie.getId(), room.getId(), tenAm.plusMinutes(130), PRICE).getId())
                .isNotNull();
    }
}
