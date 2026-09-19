package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.MovieRepository;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MovieService {
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public MovieService(MovieRepository movieRepository, ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Transactional(readOnly = true)
    public List<Movie> findActiveMovies() {
        return movieRepository.findByActiveTrueOrderByTitleAsc();
    }

    @Transactional(readOnly = true)
    public List<Movie> findAllMovies() {
        return movieRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Movie findById(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("phim", movieId));
    }

    @Transactional
    public Movie createMovie(Movie input) {
        validate(input);
        Movie movie = new Movie();
        copyEditableFields(input, movie);
        movie.setActive(true);
        return movieRepository.save(movie);
    }

    @Transactional
    public Movie updateMovie(Long movieId, Movie input) {
        validate(input);
        Movie movie = findById(movieId);
        if (!movie.getDurationMin().equals(input.getDurationMin())
                && showtimeRepository.existsByMovieId(movieId)) {
            throw new BusinessException("Không thể đổi thời lượng phim đã có suất chiếu.");
        }
        copyEditableFields(input, movie);
        return movie;
    }

    @Transactional
    public void deactivateMovie(Long movieId) {
        findById(movieId).setActive(false);
    }

    private void validate(Movie movie) {
        if (movie == null || movie.getTitle() == null || movie.getTitle().isBlank()) {
            throw new BusinessException("Tên phim không được để trống.");
        }
        if (movie.getTitle().trim().length() > 200) {
            throw new BusinessException("Tên phim không được dài quá 200 ký tự.");
        }
        if (movie.getDurationMin() == null || movie.getDurationMin() <= 0) {
            throw new BusinessException("Thời lượng phim phải lớn hơn 0 phút.");
        }
    }

    private void copyEditableFields(Movie source, Movie target) {
        target.setTitle(source.getTitle().trim());
        target.setGenre(source.getGenre());
        target.setDurationMin(source.getDurationMin());
        target.setDescription(source.getDescription());
        target.setPosterUrl(source.getPosterUrl());
        target.setAgeRating(source.getAgeRating());
    }
}
