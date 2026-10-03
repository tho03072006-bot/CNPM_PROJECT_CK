package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Lock order: showtime first, then user. Gateway checkout creation happens after the booking transaction completes. */
@Service
public class BookingLockService {
    private final ShowtimeRepository repository;
    public BookingLockService(ShowtimeRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.MANDATORY)
    public Showtime lock(Long id) {
        if (id == null || id <= 0) throw new InvalidBookingException("Mã suất chiếu phải là số nguyên dương.");
        return repository.findByIdForBookingUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("suất chiếu", id));
    }
}
