package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.booking.HoldSeatsRequest;
import edu.hcmute.cnpm.cinema.dto.booking.HoldSeatsResponse;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.exception.SeatAlreadyTakenException;
import edu.hcmute.cnpm.cinema.repository.SeatRepository;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import edu.hcmute.cnpm.cinema.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SeatBookingService {
    private final SeatService seatService;
    private final SeatPricingService seatPricingService;
    private final SeatSelectionPolicy seatSelectionPolicy;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public SeatBookingService(SeatService seatService, SeatPricingService seatPricingService,
                              SeatSelectionPolicy seatSelectionPolicy,
                              SeatRepository seatRepository, TicketRepository ticketRepository,
                              UserRepository userRepository) {
        this.seatService = seatService;
        this.seatPricingService = seatPricingService;
        this.seatSelectionPolicy = seatSelectionPolicy;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    /** Giữ toàn bộ ghế hoặc không giữ ghế nào nếu một ghế không hợp lệ/bị tranh chấp. */
    @Transactional
    public HoldSeatsResponse holdSeats(Long showtimeId, HoldSeatsRequest request, User currentUser) {
        User customer = findCurrentCustomer(currentUser);
        List<Long> seatIds = validateSeatIds(request);
        Showtime showtime = seatService.findBookableShowtime(showtimeId);
        List<Seat> seats = new ArrayList<>();
        List<BigDecimal> prices = new ArrayList<>();

        // Kiểm tra hết yêu cầu trước khi ghi. Cùng thứ tự mã ghế giúp giảm nguy cơ deadlock.
        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("ghế", seatId));
            if (seat.getRoom() == null || !showtime.getRoom().getId().equals(seat.getRoom().getId())) {
                throw new InvalidBookingException("Ghế được chọn không thuộc phòng của suất chiếu này.");
            }
            seats.add(seat);
            prices.add(seatPricingService.calculateSeatPrice(showtime.getBasePrice(), seat.getSeatType()));
        }

        List<Ticket> unavailableTickets = ticketRepository.findByShowtimeIdAndStatusIn(
                showtimeId, List.of(TicketStatus.values()));
        Set<Long> unavailableSeatIds = unavailableTickets.stream()
                .map(ticket -> ticket.getSeat().getId())
                .collect(Collectors.toSet());
        for (Seat seat : seats) {
            if (unavailableSeatIds.contains(seat.getId())) {
                throw new SeatAlreadyTakenException(showtimeId, seat.getId());
            }
        }
        List<Seat> roomSeats = seatRepository.findByRoomId(showtime.getRoom().getId());
        seatSelectionPolicy.validateSelection(roomSeats, seats, unavailableSeatIds);

        LocalDateTime heldAt = LocalDateTime.now();
        if (!showtime.getStartTime().isAfter(heldAt)) {
            throw new InvalidBookingException("Suất chiếu này đã bắt đầu, bạn không thể đặt vé nữa.");
        }
        List<Long> ticketIds = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (int index = 0; index < seats.size(); index++) {
            Seat seat = seats.get(index);
            Ticket ticket = new Ticket();
            ticket.setShowtime(showtime);
            ticket.setSeat(seat);
            ticket.setUser(customer);
            ticket.setStatus(TicketStatus.HELD);
            ticket.setHeldAt(heldAt);
            ticket.setPrice(prices.get(index));
            try {
                ticketIds.add(ticketRepository.saveAndFlush(ticket).getId());
            } catch (DataIntegrityViolationException exception) {
                // Ném ra khỏi giao dịch để rollback mọi vé đã ghi trong cùng yêu cầu.
                throw new SeatAlreadyTakenException(showtimeId, seat.getId(), exception);
            }
            totalPrice = totalPrice.add(ticket.getPrice());
        }
        return new HoldSeatsResponse(ticketIds, totalPrice,
                heldAt.plusMinutes(Constants.SEAT_HOLD_MINUTES));
    }

    public int getMaximumAdmissionsPerBooking() {
        return seatSelectionPolicy.getMaximumAdmissionsPerBooking();
    }

    private User findCurrentCustomer(User currentUser) {
        if (currentUser == null || currentUser.getId() == null || currentUser.getId() <= 0) {
            throw new InvalidBookingException("Bạn cần đăng nhập trước khi giữ ghế.");
        }
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new InvalidBookingException(
                        "Tài khoản không còn tồn tại. Vui lòng đăng nhập lại."));
    }

    private List<Long> validateSeatIds(HoldSeatsRequest request) {
        if (request == null || request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new InvalidBookingException("Vui lòng chọn ít nhất một ghế.");
        }
        List<Long> seatIds = request.getSeatIds();
        if (seatIds.stream().anyMatch(seatId -> seatId == null || seatId <= 0)) {
            throw new InvalidBookingException("Mã ghế phải là số nguyên dương.");
        }
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new InvalidBookingException("Bạn không thể chọn cùng một ghế nhiều lần.");
        }
        return seatIds.stream().sorted().toList();
    }
}
