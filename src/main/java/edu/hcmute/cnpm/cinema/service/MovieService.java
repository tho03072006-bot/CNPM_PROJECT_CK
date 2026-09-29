package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.MovieRepository;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    /**
     * Tìm phim đang chiếu theo tên và thể loại. Chuỗi tìm kiếm được bỏ dấu để
     * khách gõ "hanh dong" vẫn tìm thấy "Hành Động".
     */
    @Transactional(readOnly = true)
    public List<Movie> findActiveMovies(String keyword, String genre) {
        String normalizedKeyword = normalizeForSearch(keyword);
        String normalizedGenre = normalizeForSearch(genre);
        return findActiveMovies().stream()
                .filter(movie -> normalizedKeyword.isEmpty()
                        || normalizeForSearch(movie.getTitle()).contains(normalizedKeyword))
                .filter(movie -> normalizedGenre.isEmpty()
                        || splitGenres(movie.getGenre()).stream()
                        .map(MovieService::normalizeForSearch)
                        .anyMatch(normalizedGenre::equals))
                .toList();
    }

    /** Danh sách thể loại thật sự đang có để dựng bộ lọc, không hardcode trên giao diện. */
    @Transactional(readOnly = true)
    public List<String> findActiveGenres() {
        return findActiveMovies().stream()
                .flatMap(movie -> splitGenres(movie.getGenre()).stream())
                .distinct()
                .sorted(Comparator.comparing(MovieService::normalizeForSearch))
                .toList();
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

    @Transactional(readOnly = true)
    public Movie findActiveById(Long movieId) {
        Movie movie = findById(movieId);
        if (!Boolean.TRUE.equals(movie.getActive())) {
            throw new ResourceNotFoundException("phim", movieId);
        }
        return movie;
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

    @Transactional
    public void reactivateMovie(Long movieId) {
        findById(movieId).setActive(true);
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

    private static List<String> splitGenres(String genres) {
        if (genres == null || genres.isBlank()) {
            return List.of();
        }
        return Arrays.stream(genres.split("[,;/]"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private static String normalizeForSearch(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd');
    }
}
