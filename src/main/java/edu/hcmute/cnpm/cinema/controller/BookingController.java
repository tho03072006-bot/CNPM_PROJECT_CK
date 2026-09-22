package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.dto.booking.HoldSeatsRequest;
import edu.hcmute.cnpm.cinema.dto.booking.HoldSeatsResponse;
import edu.hcmute.cnpm.cinema.entity.User;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.service.SeatBookingService;
import edu.hcmute.cnpm.cinema.service.SeatHoldService;
import edu.hcmute.cnpm.cinema.service.SeatService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
@RequestMapping("/booking")
public class BookingController {
    private final SeatService seatService;
    private final SeatBookingService seatBookingService;
    private final SeatHoldService seatHoldService;

    public BookingController(SeatService seatService, SeatBookingService seatBookingService,
                             SeatHoldService seatHoldService) {
        this.seatService = seatService;
        this.seatBookingService = seatBookingService;
        this.seatHoldService = seatHoldService;
    }

    @GetMapping("/showtime/{showtimeId}")
    public String showSeatMap(@PathVariable Long showtimeId, Model model) {
        model.addAttribute("seatMap", seatService.findSeatMap(showtimeId));
        return "booking/seat-map";
    }

    @PostMapping(value = "/showtime/{showtimeId}/hold", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public HoldSeatsResponse holdSeats(@PathVariable Long showtimeId,
                                      @RequestBody(required = false) HoldSeatsRequest request,
                                      @SessionAttribute(name = Constants.SESSION_USER, required = false)
                                      User currentUser) {
        return seatBookingService.holdSeats(showtimeId, request, currentUser);
    }

    /**
     * Khach tu huy cac ghe dang giu cua minh o suat chieu nay (M2.7).
     *
     * Xoa han dong ve chu khong doi sang CANCELLED - xem ADR-2 trong
     * docs/DATABASE.md, vi rang buoc UNIQUE khong nhin cot status.
     */
    @PostMapping(value = "/showtime/{showtimeId}/cancel",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CancelHoldResponse cancelHold(@PathVariable Long showtimeId,
                                         @SessionAttribute(name = Constants.SESSION_USER, required = false)
                                         User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new InvalidBookingException("Bạn cần đăng nhập trước khi huỷ giữ ghế.");
        }
        int released = seatHoldService.cancelHold(currentUser.getId(), showtimeId);
        return new CancelHoldResponse(true,
                "Đã huỷ giữ " + released + " ghế. Ghế được trả lại cho người khác đặt.", released);
    }

    /** Ket qua tra ve cho AJAX khi huy giu ghe. */
    public record CancelHoldResponse(boolean success, String message, int releasedSeats) {}
}
