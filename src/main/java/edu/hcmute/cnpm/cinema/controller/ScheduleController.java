package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.dto.schedule.MovieSchedule;
import edu.hcmute.cnpm.cinema.dto.schedule.ScheduleDate;
import edu.hcmute.cnpm.cinema.entity.Showtime;
import edu.hcmute.cnpm.cinema.service.MovieService;
import edu.hcmute.cnpm.cinema.service.RoomService;
import edu.hcmute.cnpm.cinema.service.ScheduleService;
import edu.hcmute.cnpm.cinema.service.ShowtimeAvailabilityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/**
 * Trang lịch chiếu của cả rạp: chọn một ngày, xem toàn bộ phim và giờ chiếu
 * của ngày đó.
 */
@Controller
@RequestMapping("/lich-chieu")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final MovieService movieService;
    private final RoomService roomService;
    private final ShowtimeAvailabilityService availabilityService;

    public ScheduleController(ScheduleService scheduleService, MovieService movieService, RoomService roomService,
                              ShowtimeAvailabilityService availabilityService) {
        this.scheduleService = scheduleService;
        this.movieService = movieService;
        this.roomService = roomService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public String showSchedule(@RequestParam(name = "ngay", required = false) String requestedDate,
                               @RequestParam(required = false) Long movieId,
                               @RequestParam(required = false) Long roomId,
                               @RequestParam(required = false) String time,
                               Model model) {
        String selectedTime = normalizeTimePeriod(time);
        List<ScheduleDate> scheduleDates = scheduleService.findScheduleDates(movieId, roomId, selectedTime);
        LocalDate selectedDate = resolveSelectedDate(requestedDate, scheduleDates);
        List<MovieSchedule> schedule = selectedDate == null ? List.of()
                : scheduleService.findScheduleFor(selectedDate, movieId, roomId, selectedTime);
        List<Showtime> showtimes = schedule.stream()
                .flatMap(entry -> entry.getShowtimesByRoomType().values().stream())
                .flatMap(List::stream)
                .toList();

        model.addAttribute("scheduleDates", scheduleDates);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("movies", movieService.findActiveMovies());
        model.addAttribute("rooms", roomService.findAllRooms());
        model.addAttribute("selectedMovieId", movieId);
        model.addAttribute("selectedRoomId", roomId);
        model.addAttribute("selectedTime", selectedTime);
        model.addAttribute("filtering", movieId != null || roomId != null || !selectedTime.isEmpty());
        model.addAttribute("schedule", schedule);
        model.addAttribute("availabilityByShowtimeId", availabilityService.findForShowtimes(showtimes));
        return "movie/schedule";
    }

    private String normalizeTimePeriod(String time) {
        return time != null && Set.of("morning", "afternoon", "evening").contains(time.trim())
                ? time.trim() : "";
    }

    /**
     * Chọn ngày để hiển thị.
     *
     * Tham số trên URL sai định dạng, hoặc trỏ vào ngày không có suất chiếu nào,
     * thì lùi về ngày gần nhất còn suất - để khách gõ nhầm địa chỉ vẫn thấy lịch
     * chứ không gặp trang lỗi.
     */
    private LocalDate resolveSelectedDate(String requestedDate, List<ScheduleDate> scheduleDates) {
        if (requestedDate != null && !requestedDate.isBlank()) {
            try {
                LocalDate parsed = LocalDate.parse(requestedDate.trim());
                for (ScheduleDate scheduleDate : scheduleDates) {
                    if (scheduleDate.getDate().equals(parsed)) {
                        return parsed;
                    }
                }
            } catch (DateTimeParseException ignored) {
                // Tham số hỏng thì coi như khách chưa chọn ngày nào.
            }
        }
        return scheduleDates.isEmpty() ? null : scheduleDates.get(0).getDate();
    }
}
