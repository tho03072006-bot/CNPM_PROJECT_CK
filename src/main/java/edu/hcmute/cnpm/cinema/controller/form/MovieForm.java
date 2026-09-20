package edu.hcmute.cnpm.cinema.controller.form;

import edu.hcmute.cnpm.cinema.entity.Movie;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MovieForm {
    @NotBlank(message = "Vui lòng nhập tên phim.")
    @Size(max = 200, message = "Tên phim không được dài quá 200 ký tự.")
    private String title;
    @Size(max = 100, message = "Thể loại không được dài quá 100 ký tự.")
    private String genre;
    @NotNull(message = "Vui lòng nhập thời lượng.")
    @Min(value = 1, message = "Thời lượng phải lớn hơn 0 phút.")
    private Integer durationMin;
    private String description;
    @Size(max = 500, message = "Đường dẫn poster không được dài quá 500 ký tự.")
    private String posterUrl;
    @Size(max = 10, message = "Nhãn độ tuổi không được dài quá 10 ký tự.")
    private String ageRating;

    public static MovieForm from(Movie movie) {
        MovieForm form = new MovieForm();
        form.title = movie.getTitle();
        form.genre = movie.getGenre();
        form.durationMin = movie.getDurationMin();
        form.description = movie.getDescription();
        form.posterUrl = movie.getPosterUrl();
        form.ageRating = movie.getAgeRating();
        return form;
    }

    public Movie toMovie() {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setGenre(genre);
        movie.setDurationMin(durationMin);
        movie.setDescription(description);
        movie.setPosterUrl(posterUrl);
        movie.setAgeRating(ageRating);
        return movie;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getAgeRating() { return ageRating; }
    public void setAgeRating(String ageRating) { this.ageRating = ageRating; }
}
