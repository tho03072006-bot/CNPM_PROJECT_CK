package edu.hcmute.cnpm.cinema.movie;

import edu.hcmute.cnpm.cinema.dto.schedule.MovieSchedule;
import edu.hcmute.cnpm.cinema.dto.schedule.ScheduleDate;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.service.ScheduleService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cho trang lich chieu theo ngay (/lich-chieu).
 *
 * Diem can chung minh: gom dung theo loai phong, bo phim ngung chieu, va khong
 * hien suat da bat dau.
 */
@DisplayName("Lich chieu theo ngay cua ca rap")
class ScheduleServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ScheduleService scheduleService;

    @Test
    @DisplayName("Gom suat chieu theo loai phong, dung thu tu thuong - Premium - Gold Class")
    void shouldGroupShowtimesByRoomType_whenDayHasManyRoomTypes() {
        Movie movie = testDataFactory.createMovie("Phim kiem thu lich chieu");
        // Dat ten phong dung quy uoc cua seed-data.sql: loai phong nam trong ten.
        Room standard = testDataFactory.createRoom("Cinema 1", 5, 5);
        Room premium = testDataFactory.createRoom("Cinema 7 - PREMIUM", 4, 4);
        Room gold = testDataFactory.createRoom("Cinema 8 - GOLD CLASS", 3, 3);

        LocalDate ngayMai = LocalDate.now().plusDays(1);
        // Tao theo thu tu nguoc lai de chac chan ket qua da duoc sap lai, chu khong
        // phai an may theo thu tu chen vao database.
        testDataFactory.createShowtime(movie, gold, ngayMai.atTime(20, 0));
        testDataFactory.createShowtime(movie, premium, ngayMai.atTime(18, 0));
        testDataFactory.createShowtime(movie, standard, ngayMai.atTime(10, 0));
        testDataFactory.createShowtime(movie, standard, ngayMai.atTime(14, 0));

        List<MovieSchedule> lich = scheduleService.findScheduleFor(ngayMai);

        assertThat(lich).hasSize(1);
        MovieSchedule cuaPhim = lich.get(0);
        assertThat(cuaPhim.getMovie().getTitle()).isEqualTo("Phim kiem thu lich chieu");
        assertThat(cuaPhim.getTotalShowtimes()).isEqualTo(4);
        assertThat(cuaPhim.getShowtimesByRoomType().keySet())
                .as("Thu tu nhom phong phai co dinh: thuong truoc, roi Premium, roi Gold Class")
                .containsExactly(ScheduleService.ROOM_TYPE_STANDARD,
                        ScheduleService.ROOM_TYPE_PREMIUM,
                        ScheduleService.ROOM_TYPE_GOLD);
        assertThat(cuaPhim.getShowtimesByRoomType().get(ScheduleService.ROOM_TYPE_STANDARD))
                .as("Trong mot nhom phong thi suat chieu phai tang dan theo gio")
                .extracting(showtime -> showtime.getStartTime().getHour())
                .containsExactly(10, 14);
    }

    @Test
    @DisplayName("Bo qua phim da ngung chieu")
    void shouldIgnoreInactiveMovies_whenBuildingSchedule() {
        Movie dangChieu = testDataFactory.createMovie("Phim dang chieu");
        Movie ngungChieu = testDataFactory.createMovie("Phim ngung chieu");
        ngungChieu.setActive(false);
        movieRepository.save(ngungChieu);

        Room room = testDataFactory.createRoom("Cinema 1", 5, 5);
        LocalDate ngayMai = LocalDate.now().plusDays(1);
        testDataFactory.createShowtime(dangChieu, room, ngayMai.atTime(10, 0));
        testDataFactory.createShowtime(ngungChieu, room, ngayMai.atTime(15, 0));

        List<MovieSchedule> lich = scheduleService.findScheduleFor(ngayMai);

        assertThat(lich)
                .as("Phim da ngung chieu khong duoc xuat hien tren lich chieu")
                .hasSize(1);
        assertThat(lich.get(0).getMovie().getTitle()).isEqualTo("Phim dang chieu");
    }

    @Test
    @DisplayName("Sap phim theo ten de thu tu luon on dinh")
    void shouldSortMoviesByTitle_whenDayHasSeveralMovies() {
        Movie phimC = testDataFactory.createMovie("C - phim thu ba");
        Movie phimA = testDataFactory.createMovie("A - phim thu nhat");
        Movie phimB = testDataFactory.createMovie("B - phim thu hai");

        Room room = testDataFactory.createRoom("Cinema 1", 5, 5);
        LocalDate ngayMai = LocalDate.now().plusDays(1);
        testDataFactory.createShowtime(phimC, room, ngayMai.atTime(10, 0));
        testDataFactory.createShowtime(phimA, room, ngayMai.atTime(13, 0));
        testDataFactory.createShowtime(phimB, room, ngayMai.atTime(16, 0));

        List<MovieSchedule> lich = scheduleService.findScheduleFor(ngayMai);

        assertThat(lich).extracting(muc -> muc.getMovie().getTitle())
                .containsExactly("A - phim thu nhat", "B - phim thu hai", "C - phim thu ba");
    }

    @Test
    @DisplayName("Khong hien suat chieu da bat dau")
    void shouldHideStartedShowtimes_whenBuildingTodaySchedule() {
        Movie movie = testDataFactory.createMovie("Phim hom nay");
        Room room = testDataFactory.createRoom("Cinema 1", 5, 5);

        LocalDateTime daBatDau = LocalDateTime.now().minusHours(2);
        testDataFactory.createShowtime(movie, room, daBatDau);

        List<MovieSchedule> lich = scheduleService.findScheduleFor(daBatDau.toLocalDate());

        assertThat(lich)
                .as("Suat chieu bat dau cach day 2 tieng thi khong con ban ve duoc nua")
                .isEmpty();
    }

    @Test
    @DisplayName("Dai chon ngay chi liet ke ngay con suat chieu")
    void shouldListOnlyDatesWithRemainingShowtimes_whenSomeDatesAreEmpty() {
        Movie movie = testDataFactory.createMovie("Phim kiem thu dai ngay");
        Room room = testDataFactory.createRoom("Cinema 1", 5, 5);

        LocalDate ngayMai = LocalDate.now().plusDays(1);
        LocalDate ngayKia = LocalDate.now().plusDays(3);
        testDataFactory.createShowtime(movie, room, ngayMai.atTime(19, 0));
        testDataFactory.createShowtime(movie, room, ngayKia.atTime(19, 0));

        List<ScheduleDate> cacNgay = scheduleService.findScheduleDates();

        assertThat(cacNgay).extracting(ScheduleDate::getDate)
                .as("Ngay khong co suat nao thi khong duoc xuat hien tren dai chon ngay")
                .containsExactly(ngayMai, ngayKia);
        assertThat(cacNgay.get(0).isToday()).isFalse();
    }

    @Test
    @DisplayName("Tra ve lich rong khi ngay do khong co suat chieu nao")
    void shouldReturnEmptySchedule_whenDateHasNoShowtime() {
        assertThat(scheduleService.findScheduleFor(LocalDate.now().plusDays(5))).isEmpty();
    }
}
