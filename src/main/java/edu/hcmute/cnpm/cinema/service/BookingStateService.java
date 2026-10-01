package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.booking.*;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingStateService {
    private final BookingLockService locks;
    private final BookingClock clock;
    private final SeatHoldService holds;
    private final SeatService seatService;
    private final SeatRepository seats;
    private final TicketRepository tickets;
    private final SeatSelectionPolicy policy;
    private final SeatPricingService pricing;
    private final BookingConsentPolicy consent;
    public BookingStateService(BookingLockService locks, BookingClock clock, SeatHoldService holds,
            SeatService seatService, SeatRepository seats, TicketRepository tickets,
            SeatSelectionPolicy policy, SeatPricingService pricing, BookingConsentPolicy consent) {
        this.locks = locks; this.clock = clock; this.holds = holds; this.seatService = seatService;
        this.seats = seats; this.tickets = tickets; this.policy = policy; this.pricing = pricing; this.consent = consent;
    }
    public record Hold(List<Long> ticketIds, List<Long> seatIds, List<String> seatLabels,
                       BigDecimal totalPrice, long expiresAtMillis) {}
    public record State(SeatMapView seatMap, Hold activeHold, long serverTimeMillis,
                        boolean bookingOpen, boolean paymentOpen, String ageRating, String ageMessage) {}

    @Transactional
    public State read(Long showtimeId, Long userId) {
        locks.lock(showtimeId);
        holds.releaseExpiredHolds(showtimeId);
        ActiveSeatHoldView active = userId == null ? null : holds.findActiveHold(userId, showtimeId).orElse(null);
        // Bulk cleanup clears the persistence context. Reload before resolving lazy movie/room data.
        Showtime showtime = locks.lock(showtimeId);
        boolean future = showtime.getStartTime() != null && showtime.getStartTime().isAfter(clock.now());
        boolean movieActive = showtime.getMovie() != null && Boolean.TRUE.equals(showtime.getMovie().getActive());
        String rating = consent.normalize(showtime.getMovie().getAgeRating());
        Hold hold = active == null ? null : new Hold(active.getTicketIds(), active.getSeatIds(),
                active.getSeatLabels(), active.getTotalPrice(), clock.epochMillis(active.getExpiresAt()));
        return new State(seatService.buildSeatMap(showtime), hold, clock.millis(),
                movieActive && future && showtime.getStartTime().minusMinutes(seatService.getOnlineBookingCutoffMinutes())
                        .isAfter(clock.now()), future && movieActive, rating, consent.message(rating));
    }

    public record Suggestion(List<Long> seatIds, List<String> seatLabels, int admissions, BigDecimal totalPrice) {}

    @Transactional
    public Suggestion suggest(Long showtimeId, Long userId, int admissions, String seatType, BigDecimal budget) {
        if (admissions < 1 || admissions > policy.getMaximumAdmissionsPerBooking())
            throw new InvalidBookingException("Số người phải từ 1 đến 8.");
        String type = seatType == null ? "ANY" : seatType.toUpperCase(Locale.ROOT);
        if (!Set.of("ANY", "NORMAL", "VIP", "COUPLE").contains(type))
            throw new InvalidBookingException("Loại ghế không hợp lệ.");
        if (budget != null && (budget.signum() <= 0 || budget.scale() > 2
                || budget.compareTo(new BigDecimal("1000000000")) > 0))
            throw new InvalidBookingException("Ngân sách phải lớn hơn 0 và có tối đa hai chữ số thập phân.");
        locks.lock(showtimeId);
        Showtime showtime = seatService.findBookableShowtime(showtimeId);
        holds.releaseExpiredHolds(showtimeId);
        Set<Long> own = userId == null ? Set.of() : holds.findActiveHold(userId, showtimeId)
                .map(hold -> new HashSet<>(hold.getSeatIds())).orElseGet(HashSet::new);
        List<Seat> room = seats.findByRoomId(showtime.getRoom().getId()).stream()
                .sorted(Comparator.comparing(Seat::getSeatRow).thenComparing(Seat::getSeatColumn)).toList();
        Set<Long> blocked = tickets.findByShowtimeIdAndStatusIn(showtimeId, List.of(TicketStatus.values()))
                .stream().map(ticket -> ticket.getSeat().getId()).filter(id -> !own.contains(id)).collect(Collectors.toSet());
        List<String> rows = room.stream().map(Seat::getSeatRow).distinct().toList();
        List<Seat> best = null;
        BigDecimal bestPrice = null;
        double bestScore = Double.MAX_VALUE;
        for (int start = 0; start < room.size(); start++) {
            List<Seat> candidate = new ArrayList<>();
            int count = 0;
            BigDecimal total = BigDecimal.ZERO;
            for (int end = start; end < room.size(); end++) {
                Seat seat = room.get(end);
                if (blocked.contains(seat.getId()) || (!"ANY".equals(type) && !type.equals(seat.getSeatType()))) break;
                if (!candidate.isEmpty()) {
                    Seat previous = candidate.getLast();
                    if (!previous.getSeatRow().equals(seat.getSeatRow())
                            || seat.getSeatColumn() != previous.getSeatColumn() + 1) break;
                }
                count += "COUPLE".equals(seat.getSeatType()) ? 2 : 1;
                if (count > admissions) break;
                candidate.add(seat);
                total = total.add(pricing.calculateSeatPrice(showtime.getBasePrice(), seat.getSeatType()));
                if (count != admissions) continue;
                if (budget != null && total.compareTo(budget) > 0) break;
                try { policy.validateSelection(room, candidate, blocked); }
                catch (InvalidBookingException ignored) { break; }
                double center = candidate.stream().mapToInt(Seat::getSeatColumn).average().orElse(0);
                double score = Math.abs(center - (showtime.getRoom().getTotalColumns() + 1) / 2.0)
                        + Math.abs(rows.indexOf(seat.getSeatRow()) - (rows.size() - 1) * 0.6);
                if (score < bestScore || (score == bestScore && (bestPrice == null || total.compareTo(bestPrice) < 0))) {
                    best = List.copyOf(candidate); bestPrice = total; bestScore = score;
                }
                break;
            }
        }
        if (best == null) throw new InvalidBookingException("Không tìm được nhóm ghế liền nhau phù hợp. Bạn có thể đổi loại ghế, ngân sách hoặc chọn ghế thủ công.");
        return new Suggestion(best.stream().map(Seat::getId).toList(),
                best.stream().map(seat -> seat.getSeatRow() + seat.getSeatColumn()).toList(), admissions, bestPrice);
    }
}
