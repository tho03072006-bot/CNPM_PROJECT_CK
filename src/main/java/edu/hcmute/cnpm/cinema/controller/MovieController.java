package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.service.MovieService;
import edu.hcmute.cnpm.cinema.service.ShowtimeService;
import edu.hcmute.cnpm.cinema.service.ShowtimeAvailabilityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import edu.hcmute.cnpm.cinema.entity.Showtime;

@Controller
@RequestMapping("/movies")
public class MovieController {
    private final MovieService movieService;
    private final ShowtimeService showtimeService;
    private final ShowtimeAvailabilityService availabilityService;

    public MovieController(MovieService movieService, ShowtimeService showtimeService,
                           ShowtimeAvailabilityService availabilityService) {
        this.movieService = movieService;
        this.showtimeService = showtimeService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String keyword,
                       @RequestParam(name = "genre", required = false) String genre,
                       Model model) {
        model.addAttribute("movies", movieService.findActiveMovies(keyword, genre));
        model.addAttribute("genres", movieService.findActiveGenres());
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("selectedGenre", genre == null ? "" : genre.trim());
        model.addAttribute("filtering", (keyword != null && !keyword.isBlank())
                || (genre != null && !genre.isBlank()));
        return "movie/movie-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Movie movie = movieService.findActiveById(id);
        List<Showtime> showtimes = showtimeService.findUpcomingByMovie(id);
        Map<LocalDate, List<Showtime>> showtimesByDate = showtimes
                .stream().collect(Collectors.groupingBy(
                        showtime -> showtime.getStartTime().toLocalDate(), LinkedHashMap::new,
                        Collectors.toList()));
        model.addAttribute("movie", movie);
        model.addAttribute("showtimesByDate", showtimesByDate);
        model.addAttribute("availabilityByShowtimeId", availabilityService.findForShowtimes(showtimes));
        model.addAttribute("ageNotice", resolveAgeNotice(movie.getAgeRating()));
        return "movie/movie-detail";
    }

    private String resolveAgeNotice(String rating) {
        if (rating == null || rating.isBlank()) {
            return "Vui lòng kiểm tra quy định độ tuổi tại quầy vé.";
        }
        return switch (rating.trim().toUpperCase()) {
            case "P" -> "Phim phù hợp với khán giả ở mọi độ tuổi.";
            case "K" -> "Khán giả dưới 13 tuổi cần xem cùng cha, mẹ hoặc người giám hộ.";
            case "T13", "C13" -> "Phim dành cho khán giả từ đủ 13 tuổi.";
            case "T16", "C16" -> "Phim dành cho khán giả từ đủ 16 tuổi.";
            case "T18", "C18" -> "Phim dành cho khán giả từ đủ 18 tuổi.";
            default -> "Vui lòng kiểm tra quy định độ tuổi tại quầy vé.";
        };
    }
}
