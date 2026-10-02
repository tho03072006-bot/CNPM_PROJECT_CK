package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.booking.*;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.*;
import edu.hcmute.cnpm.cinema.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeatBookingService {
    private final SeatService seatService;
    private final SeatPricingService pricing;
    private final SeatSelectionPolicy policy;
    private final SeatHoldService holds;
    private final SeatRepository seats;
    private final TicketRepository tickets;
    private final UserRepository users;
    private final BookingLockService locks;
    private final BookingClock clock;
    private final BookingConsentPolicy consent;

    public SeatBookingService(SeatService seatService, SeatPricingService pricing,
            SeatSelectionPolicy policy, SeatHoldService holds, SeatRepository seats,
            TicketRepository tickets, UserRepository users, BookingLockService locks,
            BookingClock clock, BookingConsentPolicy consent) {
        this.seatService = seatService; this.pricing = pricing; this.policy = policy;
        this.holds = holds; this.seats = seats; this.tickets = tickets; this.users = users;
        this.locks = locks; this.clock = clock; this.consent = consent;
    }

    /** Creation, retry and replacement are atomic. Replacement never extends the original deadline. */
    @Transactional
    public HoldSeatsResponse holdSeats(Long showtimeId, HoldSeatsRequest request, User currentUser) {
        if (currentUser == null || currentUser.getId() == null || currentUser.getId() <= 0)
            throw new InvalidBookingException("Bạn cần đăng nhập trước khi giữ ghế.");
        List<Long> ids = validateSeatIds(request);
        locks.lock(showtimeId);
        User customer = users.findByIdForBookingUpdate(currentUser.getId())
                .orElseThrow(() -> new InvalidBookingException("Tài khoản không còn tồn tại. Vui lòng đăng nhập lại."));
        Showtime showtime = seatService.findFutureShowtime(showtimeId);
        consent.validate(showtime.getMovie().getAgeRating(), request);
        Optional<ActiveSeatHoldView> active = holds.findActiveHold(customer.getId(), showtimeId);
        boolean replacing = request.getExpectedTicketIds() != null;
        if (replacing) {
            HoldIdentity.requireMatch(request.getExpectedTicketIds(),
                    active.map(ActiveSeatHoldView::getTicketIds).orElse(List.of()));
        }
        if (active.isPresent() && new HashSet<>(ids).equals(new HashSet<>(active.get().getSeatIds()))) {
            ActiveSeatHoldView existing = active.get();
            return new HoldSeatsResponse(existing.getTicketIds(), existing.getTotalPrice(), existing.getExpiresAt());
        }
        if (active.isPresent() && !replacing)
            throw new InvalidBookingException("Bạn đang có lượt giữ ghế. Hãy chọn Đổi ghế để cập nhật lượt giữ hiện tại.");
        seatService.findBookableShowtime(showtimeId);
        List<Seat> selected = new ArrayList<>();
        for (Long id : ids) {
            Seat seat = seats.findById(id).orElseThrow(() -> new ResourceNotFoundException("ghế", id));
            if (seat.getRoom() == null || !showtime.getRoom().getId().equals(seat.getRoom().getId()))
                throw new InvalidBookingException("Ghế được chọn không thuộc phòng của suất chiếu này.");
            pricing.calculateSeatPrice(showtime.getBasePrice(), seat.getSeatType());
            selected.add(seat);
        }
        Set<Long> blocked = tickets.findByShowtimeIdAndStatusIn(showtimeId, List.of(TicketStatus.values()))
                .stream().map(ticket -> ticket.getSeat().getId()).collect(Collectors.toSet());
        active.ifPresent(hold -> blocked.removeAll(hold.getSeatIds()));
        for (Seat seat : selected) {
            if (blocked.contains(seat.getId())) throw new SeatAlreadyTakenException(showtimeId, seat.getId());
        }
        policy.validateSelection(seats.findByRoomId(showtime.getRoom().getId()), selected, blocked);
        LocalDateTime heldAt = active.map(hold -> hold.getExpiresAt().minusMinutes(Constants.SEAT_HOLD_MINUTES))
                .orElseGet(clock::now);
        // Recheck at the write boundary after validation has finished.
        seatService.findBookableShowtime(showtimeId);
        if (!heldAt.plusMinutes(Constants.SEAT_HOLD_MINUTES).isAfter(clock.now()))
            throw new InvalidBookingException("Lượt giữ đã hết hạn. Vui lòng chọn ghế lại.");
        if (active.isPresent()) {
            int deleted = tickets.deleteHeldTickets(customer.getId(), showtimeId, TicketStatus.HELD,
                    active.get().getTicketIds());
            if (deleted != active.get().getTicketIds().size())
                throw new InvalidBookingException("Lượt giữ ghế đã thay đổi. Vui lòng cập nhật trang.");
        }
        List<Long> ticketIds = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Seat seat : selected) {
            Ticket ticket = new Ticket();
            ticket.setShowtime(showtime); ticket.setSeat(seat); ticket.setUser(customer);
            ticket.setStatus(TicketStatus.HELD); ticket.setHeldAt(heldAt);
            ticket.setPrice(pricing.calculateSeatPrice(showtime.getBasePrice(), seat.getSeatType()));
            try { ticketIds.add(tickets.saveAndFlush(ticket).getId()); }
            catch (DataIntegrityViolationException exception) {
                throw new SeatAlreadyTakenException(showtimeId, seat.getId(), exception);
            }
            total = total.add(ticket.getPrice());
        }
        return new HoldSeatsResponse(ticketIds, total, heldAt.plusMinutes(Constants.SEAT_HOLD_MINUTES));
    }

    public int getMaximumAdmissionsPerBooking() { return policy.getMaximumAdmissionsPerBooking(); }

    private List<Long> validateSeatIds(HoldSeatsRequest request) {
        if (request == null || request.getSeatIds() == null || request.getSeatIds().isEmpty())
            throw new InvalidBookingException("Vui lòng chọn ít nhất một ghế.");
        List<Long> ids = request.getSeatIds();
        if (ids.size() > policy.getMaximumAdmissionsPerBooking())
            throw new InvalidBookingException("Mỗi lượt chỉ được đặt tối đa 8 chỗ.");
        if (ids.stream().anyMatch(id -> id == null || id <= 0))
            throw new InvalidBookingException("Mã ghế phải là số nguyên dương.");
        if (new HashSet<>(ids).size() != ids.size())
            throw new InvalidBookingException("Bạn không thể chọn cùng một ghế nhiều lần.");
        return ids.stream().sorted().toList();
    }
}
