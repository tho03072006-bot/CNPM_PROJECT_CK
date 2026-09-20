package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.service.MovieService;
import edu.hcmute.cnpm.cinema.service.ShowtimeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

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

    public MovieController(MovieService movieService, ShowtimeService showtimeService) {
        this.movieService = movieService;
        this.showtimeService = showtimeService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("movies", movieService.findActiveMovies());
        return "movie/movie-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Movie movie = movieService.findActiveById(id);
        Map<LocalDate, List<Showtime>> showtimesByDate = showtimeService.findUpcomingByMovie(id)
                .stream().collect(Collectors.groupingBy(
                        showtime -> showtime.getStartTime().toLocalDate(), LinkedHashMap::new,
                        Collectors.toList()));
        model.addAttribute("movie", movie);
        model.addAttribute("showtimesByDate", showtimesByDate);
        return "movie/movie-detail";
    }
}
