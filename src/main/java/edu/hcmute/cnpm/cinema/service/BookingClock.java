package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.entity.Ticket;
import org.springframework.stereotype.Component;
import java.time.*;

/** All booking timestamps use the cinema's timezone, independently of the host. */
@Component
public class BookingClock {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final Clock clock;
    public BookingClock() { this(Clock.system(ZONE)); }
    public BookingClock(Clock clock) { this.clock = clock; }
    public LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), ZONE); }
    public long millis() { return clock.millis(); }
    public long epochMillis(LocalDateTime value) { return value.atZone(ZONE).toInstant().toEpochMilli(); }
    public boolean expired(Ticket ticket) {
        return ticket.getHeldAt() == null
                || !ticket.getHeldAt().plusMinutes(Constants.SEAT_HOLD_MINUTES).isAfter(now());
    }
}
