package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.booking.SeatMapView;
import edu.hcmute.cnpm.cinema.dto.booking.SeatView;
import edu.hcmute.cnpm.cinema.entity.Seat;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import edu.hcmute.cnpm.cinema.entity.TicketStatus;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.SeatRepository;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SeatService {
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final SeatPricingService seatPricingService;

    public SeatService(ShowtimeRepository showtimeRepository, SeatRepository seatRepository,
                       TicketRepository ticketRepository, SeatPricingService seatPricingService) {
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
        this.seatPricingService = seatPricingService;
    }

    public SeatMapView findSeatMap(Long showtimeId) {
        Showtime showtime = findBookableShowtime(showtimeId);
        Map<Long, String> seatStatuses = new HashMap<>();
        LocalDateTime currentTime = LocalDateTime.now();
        List<Ticket> tickets = ticketRepository.findByShowtimeIdAndStatusIn(
                showtimeId, List.of(TicketStatus.values()));
        for (Ticket ticket : tickets) {
            seatStatuses.put(ticket.getSeat().getId(), determineSeatStatus(ticket, currentTime));
        }

        List<SeatView> seats = seatRepository.findByRoomId(showtime.getRoom().getId()).stream()
                .sorted(Comparator.comparing(Seat::getSeatRow).thenComparing(Seat::getSeatColumn))
                .map(seat -> new SeatView(seat.getId(), seat.getSeatRow(), seat.getSeatColumn(),
                        seat.getSeatType(),
                        seatPricingService.calculateSeatPrice(showtime.getBasePrice(), seat.getSeatType()),
                        seatStatuses.getOrDefault(seat.getId(), "AVAILABLE")))
                .toList();
        return new SeatMapView(showtimeId, showtime.getMovie().getTitle(), showtime.getRoom().getName(),
                showtime.getStartTime(), showtime.getRoom().getTotalColumns(), seats);
    }

    public Showtime findBookableShowtime(Long showtimeId) {
        if (showtimeId == null || showtimeId <= 0) {
            throw new InvalidBookingException("Mã suất chiếu phải là số nguyên dương.");
        }
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("suất chiếu", showtimeId));
        if (showtime.getStartTime() == null || !showtime.getStartTime().isAfter(LocalDateTime.now())) {
            throw new InvalidBookingException("Suất chiếu này đã bắt đầu, bạn không thể đặt vé nữa.");
        }
        if (showtime.getMovie() == null || !Boolean.TRUE.equals(showtime.getMovie().getActive())) {
            throw new InvalidBookingException("Phim này hiện không nhận đặt vé.");
        }
        if (showtime.getRoom() == null || showtime.getRoom().getTotalColumns() == null
                || showtime.getRoom().getTotalColumns() <= 0) {
            throw new InvalidBookingException("Phòng chiếu chưa có cấu hình ghế hợp lệ.");
        }
        return showtime;
    }

    private String determineSeatStatus(Ticket ticket, LocalDateTime currentTime) {
        if (ticket.getStatus() == TicketStatus.PAID) {
            return "PAID";
        }
        if (ticket.getStatus() == TicketStatus.HELD && ticket.getHeldAt() != null
                && ticket.getHeldAt().plusMinutes(Constants.SEAT_HOLD_MINUTES).isAfter(currentTime)) {
            return "HELD";
        }
        // UNIQUE hiện vẫn giữ chỗ cho vé cũ. Chờ thống nhất M2.6/M2.7 với Thọ,
        // không hiển thị ghế trống khi database chưa cho phép tạo vé mới.
        return "UNAVAILABLE";
    }
}
