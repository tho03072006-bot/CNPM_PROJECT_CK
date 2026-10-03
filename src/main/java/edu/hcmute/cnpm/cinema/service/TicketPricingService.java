package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.pricing.PriceRow;
import edu.hcmute.cnpm.cinema.dto.pricing.SeatSurchargeRow;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Dựng bảng giá vé công bố của rạp, phục vụ trang {@code /gia-ve}.
 *
 * Bảng giá ở đây là GIÁ CÔNG BỐ - tức là quy tắc rạp cam kết với khách. Giá thật
 * sự tính tiền nằm ở cột {@code base_price} của từng suất chiếu trong database.
 * Hai thứ đó phải khớp nhau, nên mỗi dòng của bảng còn kèm theo các mức giá đang
 * thật sự có trong lịch chiếu sắp tới để đối chiếu: lệch là nhìn thấy ngay chứ
 * không phải đi dò từng suất.
 */
@Service
public class TicketPricingService {

    // ===== Giá vé ghế thường của từng loại phòng =====
    public static final BigDecimal STANDARD_WEEKDAY = new BigDecimal("115000");
    public static final BigDecimal STANDARD_WEDNESDAY = new BigDecimal("79000");
    public static final BigDecimal STANDARD_WEEKEND = new BigDecimal("135000");
    public static final BigDecimal PREMIUM_ALL_DAYS = new BigDecimal("150000");
    public static final BigDecimal GOLD_ALL_DAYS = new BigDecimal("200000");

    public static final String DAY_GROUP_WEEKDAY = "Thứ Hai, Thứ Ba, Thứ Năm";
    public static final String DAY_GROUP_WEDNESDAY = "Thứ Tư";
    public static final String DAY_GROUP_WEEKEND = "Thứ Sáu, Thứ Bảy, Chủ nhật và ngày lễ";
    public static final String DAY_GROUP_EVERY_DAY = "Mọi ngày trong tuần";

    /** Giá gốc dùng làm ví dụ ở bảng phụ thu theo loại ghế. */
    private static final BigDecimal EXAMPLE_BASE_PRICE = STANDARD_WEEKDAY;

    private final ShowtimeRepository showtimeRepository;

    public TicketPricingService(ShowtimeRepository showtimeRepository) {
        this.showtimeRepository = showtimeRepository;
    }

    /**
     * Bảng giá công bố, mỗi dòng kèm các mức giá đang có thật trong lịch chiếu.
     */
    @Transactional(readOnly = true)
    public List<PriceRow> findPriceRows() {
        Map<String, TreeSet<BigDecimal>> pricesInSchedule = collectPricesInSchedule();

        List<PriceRow> rows = new ArrayList<>();
        rows.add(buildRow(ScheduleService.ROOM_TYPE_STANDARD, DAY_GROUP_WEEKDAY,
                STANDARD_WEEKDAY, false, pricesInSchedule));
        rows.add(buildRow(ScheduleService.ROOM_TYPE_STANDARD, DAY_GROUP_WEDNESDAY,
                STANDARD_WEDNESDAY, true, pricesInSchedule));
        rows.add(buildRow(ScheduleService.ROOM_TYPE_STANDARD, DAY_GROUP_WEEKEND,
                STANDARD_WEEKEND, false, pricesInSchedule));
        rows.add(buildRow(ScheduleService.ROOM_TYPE_PREMIUM, DAY_GROUP_EVERY_DAY,
                PREMIUM_ALL_DAYS, false, pricesInSchedule));
        rows.add(buildRow(ScheduleService.ROOM_TYPE_GOLD, DAY_GROUP_EVERY_DAY,
                GOLD_ALL_DAYS, false, pricesInSchedule));
        return rows;
    }

    /**
     * Bảng phụ thu theo loại ghế.
     *
     * Hệ số lấy từ {@link Constants} - cùng bộ số mà Module 2 dùng để tính tiền
     * thật, nên bảng giá không thể lệch với số tiền khách phải trả.
     */
    public List<SeatSurchargeRow> findSeatSurcharges() {
        List<SeatSurchargeRow> rows = new ArrayList<>();
        rows.add(buildSurcharge("Ghế thường", "Giữ nguyên giá gốc của suất chiếu.",
                Constants.SEAT_PRICE_MULTIPLIER_NORMAL));
        rows.add(buildSurcharge("Ghế VIP", "Hai hàng gần giữa phòng, cộng thêm 50%.",
                Constants.SEAT_PRICE_MULTIPLIER_VIP));
        rows.add(buildSurcharge("Ghế đôi", "Hàng cuối phòng thường, ngồi được hai người nên tính gấp đôi.",
                Constants.SEAT_PRICE_MULTIPLIER_COUPLE));
        return rows;
    }

    /** Giá gốc dùng trong cột ví dụ của bảng phụ thu. */
    public BigDecimal getExampleBasePrice() {
        return EXAMPLE_BASE_PRICE;
    }

    /** Số suất chiếu đang được đối chiếu, để ghi chú dưới bảng. */
    @Transactional(readOnly = true)
    public int countUpcomingShowtimes() {
        return findUpcomingShowtimes().size();
    }

    private PriceRow buildRow(String roomType, String dayGroup, BigDecimal price,
                              boolean highlighted, Map<String, TreeSet<BigDecimal>> pricesInSchedule) {
        TreeSet<BigDecimal> found = pricesInSchedule.get(roomType + "|" + dayGroup);
        List<BigDecimal> prices = found == null ? List.of() : new ArrayList<>(found);
        return new PriceRow(roomType, dayGroup, price, highlighted, prices);
    }

    private SeatSurchargeRow buildSurcharge(String label, String description, String multiplierText) {
        BigDecimal multiplier = new BigDecimal(multiplierText);
        BigDecimal example = EXAMPLE_BASE_PRICE.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
        return new SeatSurchargeRow(label, description, multiplier, example);
    }

    /**
     * Gom các mức giá ghế thường đang có trong lịch chiếu sắp tới, theo từng cặp
     * (loại phòng, nhóm ngày).
     */
    private Map<String, TreeSet<BigDecimal>> collectPricesInSchedule() {
        Map<String, TreeSet<BigDecimal>> prices = new LinkedHashMap<>();
        for (Showtime showtime : findUpcomingShowtimes()) {
            String roomType = ScheduleService.resolveRoomType(showtime.getRoom().getName());
            String dayGroup = resolveDayGroup(roomType, showtime.getStartTime().toLocalDate());
            prices.computeIfAbsent(roomType + "|" + dayGroup,
                    key -> new TreeSet<>(Comparator.naturalOrder())).add(showtime.getBasePrice());
        }
        return prices;
    }

    private List<Showtime> findUpcomingShowtimes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.toLocalDate().plusDays(ScheduleService.SCHEDULE_DAYS).atStartOfDay();
        return showtimeRepository
                .findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(now, until);
    }

    /**
     * Ngày này thuộc nhóm ngày nào trong bảng giá.
     *
     * Phòng Premium và Gold Class chỉ có một mức giá nên mọi ngày gom về một nhóm.
     */
    private String resolveDayGroup(String roomType, LocalDate date) {
        if (!ScheduleService.ROOM_TYPE_STANDARD.equals(roomType)) {
            return DAY_GROUP_EVERY_DAY;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.WEDNESDAY) {
            return DAY_GROUP_WEDNESDAY;
        }
        if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY
                || dayOfWeek == DayOfWeek.SUNDAY) {
            return DAY_GROUP_WEEKEND;
        }
        return DAY_GROUP_WEEKDAY;
    }
}
