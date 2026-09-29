package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.dto.schedule.ShowtimeAvailability;
import edu.hcmute.cnpm.cinema.entity.Room;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShowtimeAvailabilityService {
    private final TicketRepository ticketRepository;

    public ShowtimeAvailabilityService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Trả sức chứa của toàn bộ suất chiếu bằng đúng một truy vấn đếm vé.
     * Mọi dòng vé còn tồn tại đều chiếm ghế vì ràng buộc UNIQUE(showtime, seat).
     */
    @Transactional(readOnly = true)
    public Map<Long, ShowtimeAvailability> findForShowtimes(Collection<Showtime> showtimes) {
        if (showtimes == null || showtimes.isEmpty()) {
            return Map.of();
        }
        List<Long> showtimeIds = showtimes.stream().map(Showtime::getId).distinct().toList();
        Map<Long, Integer> reservedByShowtime = new LinkedHashMap<>();
        for (Object[] row : ticketRepository.countReservedSeatsByShowtimeIds(showtimeIds)) {
            reservedByShowtime.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }

        Map<Long, ShowtimeAvailability> result = new LinkedHashMap<>();
        for (Showtime showtime : showtimes) {
            Room room = showtime.getRoom();
            int totalSeats = room == null || room.getTotalRows() == null || room.getTotalColumns() == null
                    ? 0 : room.getTotalRows() * room.getTotalColumns();
            result.put(showtime.getId(), new ShowtimeAvailability(
                    totalSeats, reservedByShowtime.getOrDefault(showtime.getId(), 0)));
        }
        return result;
    }
}
