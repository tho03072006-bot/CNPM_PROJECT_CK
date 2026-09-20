package edu.hcmute.cnpm.cinema.pricing;

import edu.hcmute.cnpm.cinema.dto.pricing.PriceRow;
import edu.hcmute.cnpm.cinema.dto.pricing.SeatSurchargeRow;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.service.ScheduleService;
import edu.hcmute.cnpm.cinema.service.TicketPricingService;
import edu.hcmute.cnpm.cinema.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cho trang bang gia ve (/gia-ve).
 *
 * Diem quan trong nhat: bang gia cong bo phai doi chieu duoc voi gia that su
 * gan vao tung suat chieu, de hai ben lech nhau la phat hien ngay.
 */
@DisplayName("Bang gia ve cua rap")
class TicketPricingServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TicketPricingService ticketPricingService;

    @Test
    @DisplayName("Bang gia liet ke du ba loai phong va cac nhom ngay")
    void shouldListAllRoomTypesAndDayGroups_whenBuildingPriceTable() {
        List<PriceRow> rows = ticketPricingService.findPriceRows();

        assertThat(rows).hasSize(5);
        assertThat(rows).extracting(PriceRow::getRoomType).containsExactly(
                ScheduleService.ROOM_TYPE_STANDARD,
                ScheduleService.ROOM_TYPE_STANDARD,
                ScheduleService.ROOM_TYPE_STANDARD,
                ScheduleService.ROOM_TYPE_PREMIUM,
                ScheduleService.ROOM_TYPE_GOLD);
        assertThat(rows).extracting(PriceRow::getPrice).containsExactly(
                TicketPricingService.STANDARD_WEEKDAY,
                TicketPricingService.STANDARD_WEDNESDAY,
                TicketPricingService.STANDARD_WEEKEND,
                TicketPricingService.PREMIUM_ALL_DAYS,
                TicketPricingService.GOLD_ALL_DAYS);
    }

    @Test
    @DisplayName("Chi dong Thu Tu duoc danh dau la ngay uu dai")
    void shouldMarkOnlyWednesdayAsDeal_whenBuildingPriceTable() {
        List<PriceRow> rows = ticketPricingService.findPriceRows();

        List<PriceRow> uuDai = rows.stream().filter(PriceRow::isHighlighted).toList();
        assertThat(uuDai).hasSize(1);
        assertThat(uuDai.get(0).getDayGroup()).isEqualTo(TicketPricingService.DAY_GROUP_WEDNESDAY);
        assertThat(uuDai.get(0).getPrice()).isEqualTo(TicketPricingService.STANDARD_WEDNESDAY);
    }

    @Test
    @DisplayName("Bao KHOP khi gia trong lich chieu dung theo quy tac cong bo")
    void shouldReportMatching_whenSchedulePriceFollowsPublishedRule() {
        Movie movie = testDataFactory.createMovie("Phim kiem thu bang gia");
        Room standard = testDataFactory.createRoom("Cinema 1", 5, 5);

        // Tao suat chieu cho 6 ngay toi, moi ngay dat dung gia ma bang gia cong bo.
        // Di het 6 ngay nen chac chan phu du ca ngay thuong, Thu Tu va cuoi tuan.
        for (int offset = 1; offset <= 6; offset++) {
            LocalDate date = LocalDate.now().plusDays(offset);
            testDataFactory.createShowtime(movie, standard, date.atTime(19, 0), expectedStandardPrice(date));
        }

        List<PriceRow> rows = ticketPricingService.findPriceRows();

        assertThat(rows).filteredOn(PriceRow::isPresentInSchedule)
                .as("6 ngay lien tiep phai phu it nhat hai nhom ngay cua phong thuong")
                .hasSizeGreaterThanOrEqualTo(2);
        assertThat(rows).allSatisfy(row -> assertThat(row.isMatchingSchedule())
                .as("Dong '%s - %s' phai bao khop", row.getRoomType(), row.getDayGroup())
                .isTrue());
    }

    @Test
    @DisplayName("Bao LECH khi suat chieu gan gia khac gia cong bo")
    void shouldReportMismatch_whenSchedulePriceDiffersFromPublished() {
        Movie movie = testDataFactory.createMovie("Phim gia sai");
        // Phong Premium chi co mot muc gia cho moi ngay nen test khong phu thuoc
        // hom nay la thu may.
        Room premium = testDataFactory.createRoom("Cinema 7 - PREMIUM", 4, 4);
        testDataFactory.createShowtime(movie, premium,
                LocalDate.now().plusDays(1).atTime(19, 0), new BigDecimal("999000"));

        PriceRow premiumRow = ticketPricingService.findPriceRows().stream()
                .filter(row -> ScheduleService.ROOM_TYPE_PREMIUM.equals(row.getRoomType()))
                .findFirst().orElseThrow();

        assertThat(premiumRow.isPresentInSchedule()).isTrue();
        assertThat(premiumRow.isMatchingSchedule())
                .as("Gia cong bo 150.000 ma lich dat 999.000 thi phai bao lech")
                .isFalse();
        assertThat(premiumRow.getPricesInScheduleText())
                .as("Chuoi hien thi phai co dau cham ngan nghin cho de doc")
                .isEqualTo("999.000");
    }

    @Test
    @DisplayName("Bao khong co du lieu khi nhom ngay chua co suat chieu nao")
    void shouldReportNoData_whenScheduleIsEmpty() {
        List<PriceRow> rows = ticketPricingService.findPriceRows();

        assertThat(rows).allSatisfy(row -> {
            assertThat(row.isPresentInSchedule()).isFalse();
            // Khong co du lieu thi khong the ket luan la lech.
            assertThat(row.isMatchingSchedule()).isTrue();
        });
        assertThat(ticketPricingService.countUpcomingShowtimes()).isZero();
    }

    @Test
    @DisplayName("Phu thu theo loai ghe tinh dung theo he so dung chung")
    void shouldComputeSeatSurchargeExamples_fromSharedMultipliers() {
        List<SeatSurchargeRow> surcharges = ticketPricingService.findSeatSurcharges();

        assertThat(surcharges).hasSize(3);
        assertThat(surcharges).extracting(SeatSurchargeRow::getExamplePrice)
                .as("Gia goc vi du la 115.000: ghe thuong giu nguyen, VIP cong 50%%, ghe doi gap doi")
                .containsExactly(new BigDecimal("115000"),
                        new BigDecimal("172500"),
                        new BigDecimal("230000"));
    }

    /** Gia ghe thuong ma bang gia cong bo cho ngay nay. */
    private BigDecimal expectedStandardPrice(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.WEDNESDAY) {
            return TicketPricingService.STANDARD_WEDNESDAY;
        }
        if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY
                || dayOfWeek == DayOfWeek.SUNDAY) {
            return TicketPricingService.STANDARD_WEEKEND;
        }
        return TicketPricingService.STANDARD_WEEKDAY;
    }
}
