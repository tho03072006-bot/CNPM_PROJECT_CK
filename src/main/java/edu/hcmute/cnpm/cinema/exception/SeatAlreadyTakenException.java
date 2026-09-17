package edu.hcmute.cnpm.cinema.exception;

/**
 * Nem ra khi ghe da co nguoi khac giu / dat mat truoc do cho cung mot suat chieu.
 *
 * DAY LA MAT XICH CUOI CUA ADR-001. Luong xu ly bat buoc cua Module 2:
 *
 * <pre>
 *   try {
 *       ticketRepository.saveAndFlush(ticket);
 *   } catch (DataIntegrityViolationException ex) {
 *       // database da chan bang UNIQUE (showtime_id, seat_id)
 *       throw new SeatAlreadyTakenException(showtimeId, seatId, ex);
 *   }
 * </pre>
 *
 * KHONG duoc kiem tra kieu "select xem ghe co trong khong roi moi insert" rui bo qua try/catch:
 * giua luc select va luc insert van co the co nguoi khac chen vao (race-condition).
 * Chi co rang buoc UNIQUE o database moi chan chac chan duoc - xem test
 * SeatBookingConcurrencyIntegrationTest.
 *
 * Phu trach: Tho (Module 4) - nguoi dung chinh: Thang (Module 2).
 */
public class SeatAlreadyTakenException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private static final String USER_MESSAGE = "Ghe nay vua co nguoi khac giu mat roi. Vui long chon ghe khac.";

    private final Long showtimeId;
    private final Long seatId;

    public SeatAlreadyTakenException(Long showtimeId, Long seatId) {
        super(USER_MESSAGE);
        this.showtimeId = showtimeId;
        this.seatId = seatId;
    }

    public SeatAlreadyTakenException(Long showtimeId, Long seatId, Throwable cause) {
        super(USER_MESSAGE, cause);
        this.showtimeId = showtimeId;
        this.seatId = seatId;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public Long getSeatId() {
        return seatId;
    }
}
