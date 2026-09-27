package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.entity.Movie;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.exception.ResourceNotFoundException;
import edu.hcmute.cnpm.cinema.repository.MovieRepository;
import edu.hcmute.cnpm.cinema.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.Arrays;
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

    /** Mở bán lại phim đã ngừng chiếu - trước đây bấm nhầm "Ngừng chiếu" là không có đường quay lại. */
    @Transactional
    public void activateMovie(Long movieId) {
        findById(movieId).setActive(true);
    }

    /**
     * Tìm phim đang chiếu theo tên và lọc theo thể loại. Bỏ trống cả hai thì trả về tất cả.
     * So khớp không phân biệt dấu và hoa thường, xem {@link VietnameseText#fold(String)}.
     */
    @Transactional(readOnly = true)
    public List<Movie> searchActiveMovies(String keyword, String genre) {
        String foldedKeyword = VietnameseText.fold(keyword);
        String foldedGenre = VietnameseText.fold(genre);
        return findActiveMovies().stream()
                .filter(movie -> foldedKeyword.isEmpty()
                        || VietnameseText.fold(movie.getTitle()).contains(foldedKeyword))
                .filter(movie -> foldedGenre.isEmpty()
                        || splitGenres(movie.getGenre()).stream()
                                .anyMatch(item -> VietnameseText.fold(item).equals(foldedGenre)))
                .toList();
    }

    /**
     * Danh sách thể loại của các phim đang chiếu, dùng cho ô lọc.
     *
     * Cột genre lưu kiểu "Hoạt hình, Hài, Phiêu lưu" nên phải tách theo dấu phẩy.
     */
    @Transactional(readOnly = true)
    public List<String> findActiveGenres() {
        Collator vietnameseOrder = Collator.getInstance(Locale.forLanguageTag("vi"));
        return findActiveMovies().stream()
                .flatMap(movie -> splitGenres(movie.getGenre()).stream())
                .distinct()
                .sorted(vietnameseOrder)
                .toList();
    }

    private List<String> splitGenres(String genre) {
        if (genre == null || genre.isBlank()) {
            return List.of();
        }
        return Arrays.stream(genre.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
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
