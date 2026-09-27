package edu.hcmute.cnpm.cinema.controller;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.service.MovieService;
import edu.hcmute.cnpm.cinema.service.ShowtimeService;
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

    public MovieController(MovieService movieService, ShowtimeService showtimeService) {
        this.movieService = movieService;
        this.showtimeService = showtimeService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String keyword,
                       @RequestParam(name = "theLoai", required = false) String genre,
                       Model model) {
        boolean isFiltering = (keyword != null && !keyword.isBlank()) || (genre != null && !genre.isBlank());
        model.addAttribute("movies", movieService.searchActiveMovies(keyword, genre));
        model.addAttribute("activeMovieCount", movieService.findActiveMovies().size());
        model.addAttribute("genres", movieService.findActiveGenres());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedGenre", genre);
        model.addAttribute("isFiltering", isFiltering);
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
