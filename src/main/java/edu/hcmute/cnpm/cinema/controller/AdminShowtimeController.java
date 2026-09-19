package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.form.ShowtimeForm;
import edu.hcmute.cnpm.cinema.service.MovieService;
import edu.hcmute.cnpm.cinema.service.RoomService;
import edu.hcmute.cnpm.cinema.service.ShowtimeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/showtimes")
public class AdminShowtimeController {
    private final ShowtimeService showtimeService;
    private final MovieService movieService;
    private final RoomService roomService;

    public AdminShowtimeController(ShowtimeService showtimeService, MovieService movieService,
                                   RoomService roomService) {
        this.showtimeService = showtimeService;
        this.movieService = movieService;
        this.roomService = roomService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("showtimes", showtimeService.findAllShowtimes());
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
        showtimeService.createShowtime(form.getMovieId(), form.getRoomId(),
                form.getStartTime(), form.getBasePrice());
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
        showtimeService.updateShowtime(id, form.getMovieId(), form.getRoomId(),
                form.getStartTime(), form.getBasePrice());
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã cập nhật suất chiếu.");
        return "redirect:/admin/showtimes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        showtimeService.deleteShowtime(id);
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã xoá suất chiếu.");
        return "redirect:/admin/showtimes";
    }

    private void addChoices(Model model) {
        model.addAttribute("movies", movieService.findActiveMovies());
        model.addAttribute("rooms", roomService.findAllRooms());
    }
}
