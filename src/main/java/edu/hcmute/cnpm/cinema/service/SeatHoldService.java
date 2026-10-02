package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.booking.ActiveSeatHoldView;
import edu.hcmute.cnpm.cinema.entity.*;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SeatHoldService {
    private final TicketRepository tickets;
    private final BookingLockService locks;
    private final BookingClock clock;
    public SeatHoldService(TicketRepository tickets, BookingLockService locks, BookingClock clock) {
        this.tickets = tickets; this.locks = locks; this.clock = clock;
    }

    /** ADR-2: only expired HELD rows are deleted; PAID rows are excluded in SQL itself. */
    @Transactional
    public int releaseExpiredHolds() {
        LocalDateTime cutoff = clock.now().minusMinutes(Constants.SEAT_HOLD_MINUTES);
        int released = 0;
        for (Long id : tickets.findExpiredShowtimeIds(TicketStatus.HELD, cutoff)) {
            locks.lock(id);
            released += tickets.deleteExpiredHolds(id, TicketStatus.HELD, cutoff);
        }
        return released;
    }

    @Transactional
    public int releaseExpiredHolds(Long showtimeId) {
        locks.lock(showtimeId);
        return tickets.deleteExpiredHolds(showtimeId, TicketStatus.HELD,
                clock.now().minusMinutes(Constants.SEAT_HOLD_MINUTES));
    }

    @Transactional
    public Optional<ActiveSeatHoldView> findActiveHold(Long userId, Long showtimeId) {
        if (userId == null || userId <= 0 || showtimeId == null || showtimeId <= 0) return Optional.empty();
        releaseExpiredHolds(showtimeId);
        List<Ticket> active = tickets.findByUserIdAndShowtimeIdAndStatus(userId, showtimeId, TicketStatus.HELD)
                .stream().filter(ticket -> !clock.expired(ticket))
                .sorted(Comparator.comparing((Ticket ticket) -> ticket.getSeat().getSeatRow())
                        .thenComparing(ticket -> ticket.getSeat().getSeatColumn())).toList();
        if (active.isEmpty()) return Optional.empty();
        return Optional.of(new ActiveSeatHoldView(active.stream().map(Ticket::getId).toList(),
                active.stream().map(ticket -> ticket.getSeat().getId()).toList(),
                active.stream().map(ticket -> ticket.getSeat().getSeatRow() + ticket.getSeat().getSeatColumn()).toList(),
                active.stream().map(Ticket::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add),
                active.stream().map(ticket -> ticket.getHeldAt().plusMinutes(Constants.SEAT_HOLD_MINUTES))
                        .min(LocalDateTime::compareTo).orElseThrow()));
    }

    @Transactional
    public int cancelHold(Long userId, Long showtimeId, List<Long> expectedTicketIds) {
        if (userId == null || userId <= 0) throw new InvalidBookingException("Bạn cần đăng nhập trước khi huỷ giữ ghế.");
        locks.lock(showtimeId);
        List<Ticket> held = tickets.findByUserIdAndShowtimeIdAndStatus(userId, showtimeId, TicketStatus.HELD);
        HoldIdentity.requireMatch(expectedTicketIds, held.stream().map(Ticket::getId).toList());
        return tickets.deleteHeldTickets(userId, showtimeId, TicketStatus.HELD, expectedTicketIds);
    }
}
