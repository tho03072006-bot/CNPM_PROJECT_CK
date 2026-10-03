package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.form.ShowtimeForm;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import edu.hcmute.cnpm.cinema.service.MovieService;
import edu.hcmute.cnpm.cinema.service.RoomService;
import edu.hcmute.cnpm.cinema.service.ShowtimeService;
import edu.hcmute.cnpm.cinema.service.ShowtimeAvailabilityService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/showtimes")
public class AdminShowtimeController {
    private final ShowtimeService showtimeService;
    private final MovieService movieService;
    private final RoomService roomService;
    private final ShowtimeAvailabilityService availabilityService;

    public AdminShowtimeController(ShowtimeService showtimeService, MovieService movieService,
                                   RoomService roomService, ShowtimeAvailabilityService availabilityService) {
        this.showtimeService = showtimeService;
        this.movieService = movieService;
        this.roomService = roomService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public String list(@RequestParam(name = "date", required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(name = "movieId", required = false) Long movieId,
                       @RequestParam(name = "roomId", required = false) Long roomId,
                       Model model) {
        var showtimes = showtimeService.findShowtimes(date, movieId, roomId);
        model.addAttribute("showtimes", showtimes);
        model.addAttribute("availabilityByShowtimeId", availabilityService.findForShowtimes(showtimes));
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedMovieId", movieId);
        model.addAttribute("selectedRoomId", roomId);
        model.addAttribute("movies", movieService.findAllMovies());
        model.addAttribute("rooms", roomService.findAllRooms());
        return "movie/showtime-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("showtimeForm", new ShowtimeForm());
        addChoices(model);
        return "movie/showtime-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("showtimeForm") ShowtimeForm form, BindingResult errors,
                         Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            addChoices(model);
            return "movie/showtime-form";
        }
        try {
            showtimeService.createShowtime(form.getMovieId(), form.getRoomId(),
                    form.getStartTime(), form.getBasePrice());
        } catch (InvalidBookingException exception) {
            // Trùng giờ hoặc giờ chiếu đã qua: lời nhắc nằm ngay dưới ô giờ bắt đầu.
            errors.rejectValue("startTime", "showtime.schedule", exception.getMessage());
            addChoices(model);
            return "movie/showtime-form";
        } catch (BusinessException exception) {
            // Lỗi nghiệp vụ khác (phim đã ngừng chiếu, suất đã bán vé...) hiện ở đầu form,
            // dữ liệu vừa nhập vẫn còn.
            model.addAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            addChoices(model);
            return "movie/showtime-form";
        }
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã thêm suất chiếu.");
        return "redirect:/admin/showtimes";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("showtimeForm", ShowtimeForm.from(showtimeService.findById(id)));
        model.addAttribute("showtimeId", id);
        addChoices(model);
        return "movie/showtime-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("showtimeForm") ShowtimeForm form,
                         BindingResult errors, Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute("showtimeId", id);
            addChoices(model);
            return "movie/showtime-form";
        }
        try {
            showtimeService.updateShowtime(id, form.getMovieId(), form.getRoomId(),
                    form.getStartTime(), form.getBasePrice());
        } catch (InvalidBookingException exception) {
            // Trùng giờ hoặc giờ chiếu đã qua: lời nhắc nằm ngay dưới ô giờ bắt đầu.
            errors.rejectValue("startTime", "showtime.schedule", exception.getMessage());
            model.addAttribute("showtimeId", id);
            addChoices(model);
            return "movie/showtime-form";
        } catch (BusinessException exception) {
            // Lỗi nghiệp vụ khác (phim đã ngừng chiếu, suất đã bán vé...) hiện ở đầu form,
            // dữ liệu vừa nhập vẫn còn.
            model.addAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            model.addAttribute("showtimeId", id);
            addChoices(model);
            return "movie/showtime-form";
        }
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã cập nhật suất chiếu.");
        return "redirect:/admin/showtimes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            showtimeService.deleteShowtime(id);
            redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã xoá suất chiếu.");
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
        }
        return "redirect:/admin/showtimes";
    }

    private void addChoices(Model model) {
        model.addAttribute("movies", movieService.findActiveMovies());
        model.addAttribute("rooms", roomService.findAllRooms());
        model.addAttribute("breakMinutes", showtimeService.getBreakMinutes());
    }
}
