package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.constants.Constants;
import edu.hcmute.cnpm.cinema.controller.form.MovieForm;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/movies")
public class AdminMovieController {
    private final MovieService movieService;

    public AdminMovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String genre,
                       @RequestParam(required = false) Boolean active,
                       Model model) {
        var allMovies = movieService.findAllMovies();
        model.addAttribute("movies", movieService.findMovies(q, genre, active));
        model.addAttribute("genres", movieService.findAllGenres());
        model.addAttribute("keyword", q == null ? "" : q.trim());
        model.addAttribute("selectedGenre", genre == null ? "" : genre);
        model.addAttribute("selectedActive", active);
        model.addAttribute("filtering", (q != null && !q.isBlank())
                || (genre != null && !genre.isBlank()) || active != null);
        model.addAttribute("totalMovies", allMovies.size());
        model.addAttribute("activeMovies", allMovies.stream().filter(movie -> Boolean.TRUE.equals(movie.getActive())).count());
        model.addAttribute("inactiveMovies", allMovies.stream().filter(movie -> !Boolean.TRUE.equals(movie.getActive())).count());
        model.addAttribute("incompleteMovies", allMovies.stream().filter(this::isIncomplete).count());
        return "movie/admin-movie-list";
    }

    private boolean isIncomplete(Movie movie) {
        return movie.getPosterUrl() == null || movie.getPosterUrl().isBlank()
                || movie.getDescription() == null || movie.getDescription().isBlank()
                || movie.getGenre() == null || movie.getGenre().isBlank()
                || movie.getAgeRating() == null || movie.getAgeRating().isBlank();
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("movieForm", new MovieForm());
        return "movie/movie-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("movieForm") MovieForm form, BindingResult errors,
                         Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            return "movie/movie-form";
        }
        try {
            movieService.createMovie(form.toMovie());
        } catch (BusinessException exception) {
            model.addAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            return "movie/movie-form";
        }
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã thêm phim thành công.");
        return "redirect:/admin/movies";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("movieForm", MovieForm.from(movieService.findById(id)));
        model.addAttribute("movieId", id);
        return "movie/movie-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("movieForm") MovieForm form,
                         BindingResult errors, Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute("movieId", id);
            return "movie/movie-form";
        }
        try {
            movieService.updateMovie(id, form.toMovie());
        } catch (BusinessException exception) {
            // Ví dụ: đổi thời lượng của phim đã có suất chiếu.
            model.addAttribute(Constants.MODEL_ERROR_MESSAGE, exception.getMessage());
            model.addAttribute("movieId", id);
            return "movie/movie-form";
        }
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã cập nhật phim thành công.");
        return "redirect:/admin/movies";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        movieService.deactivateMovie(id);
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã ngừng chiếu phim.");
        return "redirect:/admin/movies";
    }

    @PostMapping("/{id}/reactivate")
    public String reactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        movieService.reactivateMovie(id);
        redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Đã khôi phục phim vào danh sách đang chiếu.");
        return "redirect:/admin/movies";
    }
}
