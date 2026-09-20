package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.dto.schedule.ScheduleDate;
import edu.hcmute.cnpm.cinema.service.ScheduleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Trang lịch chiếu của cả rạp: chọn một ngày, xem toàn bộ phim và giờ chiếu
 * của ngày đó.
 */
@Controller
@RequestMapping("/lich-chieu")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public String showSchedule(@RequestParam(name = "ngay", required = false) String requestedDate,
                               Model model) {
        List<ScheduleDate> scheduleDates = scheduleService.findScheduleDates();
        LocalDate selectedDate = resolveSelectedDate(requestedDate, scheduleDates);

        model.addAttribute("scheduleDates", scheduleDates);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("schedule",
                selectedDate == null ? List.of() : scheduleService.findScheduleFor(selectedDate));
        return "movie/schedule";
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
