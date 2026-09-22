package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.stats.RevenueRow;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thống kê doanh thu cho trang quản trị.
 *
 * Chỉ đếm vé ở trạng thái đã thanh toán - vé đang giữ chưa phải là tiền thật.
 *
 * Phép gom nhóm làm bằng Java thay vì viết câu truy vấn gom nhóm riêng: một rạp
 * chỉ bán vài trăm tới vài nghìn vé trong khoảng thống kê, đọc hết vào bộ nhớ vẫn
 * nhẹ, mà code thì dễ đọc và dễ viết test hơn nhiều.
 */
@Service
public class StatsService {

    /** Số ngày mặc định của báo cáo. */
    public static final int DEFAULT_DAYS = 7;

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TicketRepository ticketRepository;

    public StatsService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /** Vé đã thanh toán trong {@code days} ngày gần nhất, tính cả hôm nay. */
    @Transactional(readOnly = true)
    public List<Ticket> findPaidTickets(int days) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(Math.max(days, 1) - 1L).atStartOfDay();
        LocalDateTime until = today.plusDays(1).atStartOfDay();
        return ticketRepository.findByStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(
                TicketStatus.PAID, from, until);
    }

    /**
     * Doanh thu từng ngày, ngày mới nhất lên đầu.
     *
     * Ngày không bán được vé nào vẫn xuất hiện với số 0, để nhìn vào biết là
     * "hôm đó ế" chứ không phải "hôm đó mất dữ liệu".
     */
    @Transactional(readOnly = true)
    public List<RevenueRow> findRevenueByDay(int days) {
        int range = Math.max(days, 1);
        Map<LocalDate, long[]> counts = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> revenues = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        for (int offset = 0; offset < range; offset++) {
            LocalDate date = today.minusDays(offset);
            counts.put(date, new long[]{0});
            revenues.put(date, BigDecimal.ZERO);
        }

        for (Ticket ticket : findPaidTickets(range)) {
            LocalDate date = ticket.getPaidAt().toLocalDate();
            if (counts.containsKey(date)) {
                counts.get(date)[0]++;
                revenues.put(date, revenues.get(date).add(safePrice(ticket)));
            }
        }

        List<RevenueRow> rows = new ArrayList<>();
        for (Map.Entry<LocalDate, long[]> entry : counts.entrySet()) {
            rows.add(new RevenueRow(DAY_FORMAT.format(entry.getKey()),
                    entry.getValue()[0], revenues.get(entry.getKey())));
        }
        return rows;
    }

    /** Phim bán chạy nhất, xếp theo doanh thu giảm dần. */
    @Transactional(readOnly = true)
    public List<RevenueRow> findTopMovies(int days, int limit) {
        Map<String, long[]> counts = new LinkedHashMap<>();
        Map<String, BigDecimal> revenues = new LinkedHashMap<>();

        for (Ticket ticket : findPaidTickets(days)) {
            String title = ticket.getShowtime().getMovie().getTitle();
            counts.computeIfAbsent(title, key -> new long[]{0})[0]++;
            revenues.merge(title, safePrice(ticket), BigDecimal::add);
        }

        List<RevenueRow> rows = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : counts.entrySet()) {
            rows.add(new RevenueRow(entry.getKey(), entry.getValue()[0], revenues.get(entry.getKey())));
        }
        rows.sort(Comparator.comparing(RevenueRow::getRevenue).reversed()
                .thenComparing(RevenueRow::getLabel));
        return rows.size() > limit ? rows.subList(0, limit) : rows;
    }

    /** Tổng doanh thu của cả khoảng thống kê. */
    public BigDecimal sumRevenue(List<RevenueRow> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (RevenueRow row : rows) {
            total = total.add(row.getRevenue());
        }
        return total;
    }

    /** Tổng số vé của cả khoảng thống kê. */
    public long sumTickets(List<RevenueRow> rows) {
        long total = 0;
        for (RevenueRow row : rows) {
            total += row.getTicketCount();
        }
        return total;
    }

    private BigDecimal safePrice(Ticket ticket) {
        return ticket.getPrice() == null ? BigDecimal.ZERO : ticket.getPrice();
    }
}
