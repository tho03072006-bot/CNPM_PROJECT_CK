document.addEventListener("DOMContentLoaded", () => {
    const moviePreview = document.querySelector("[data-movie-preview]");
    if (moviePreview) {
        const titleInput = document.querySelector("#title");
        const posterInput = document.querySelector("#posterUrl");
        const genreInput = document.querySelector("#genre");
        const durationInput = document.querySelector("#durationMin");
        const ratingInput = document.querySelector("#ageRating");
        const title = moviePreview.querySelector("[data-preview-title]");
        const meta = moviePreview.querySelector("[data-preview-meta]");
        const image = moviePreview.querySelector("[data-poster-image]");
        const fallback = moviePreview.querySelector("[data-poster-fallback]");

        const renderMovie = () => {
            const movieTitle = titleInput.value.trim();
            const posterUrl = posterInput.value.trim();
            title.textContent = movieTitle || "Tên phim";
            const genre = genreInput.value.trim() || "Chưa phân loại";
            const duration = durationInput.value ? `${durationInput.value} phút` : "Chưa có thời lượng";
            const rating = ratingInput.value.trim() || "Chưa xếp hạng";
            meta.textContent = `${genre} · ${duration} · ${rating}`;
            fallback.textContent = movieTitle ? movieTitle.charAt(0).toUpperCase() : "P";
            if (posterUrl) {
                image.hidden = false;
                image.src = posterUrl;
            } else {
                image.hidden = true;
                image.removeAttribute("src");
            }
        };
        image.addEventListener("error", () => { image.hidden = true; });
        titleInput.addEventListener("input", renderMovie);
        posterInput.addEventListener("input", renderMovie);
        genreInput.addEventListener("input", renderMovie);
        durationInput.addEventListener("input", renderMovie);
        ratingInput.addEventListener("input", renderMovie);
        renderMovie();
    }

    const roomPreview = document.querySelector("[data-room-preview]");
    if (roomPreview) {
        const rowsInput = document.querySelector("#totalRows");
        const columnsInput = document.querySelector("#totalColumns");
        const renderCapacity = () => {
            const rows = Math.max(0, Number.parseInt(rowsInput.value, 10) || 0);
            const columns = Math.max(0, Number.parseInt(columnsInput.value, 10) || 0);
            const vip = Math.min(2, rows) * columns;
            roomPreview.querySelector("[data-seat-total]").textContent = rows * columns;
            roomPreview.querySelector("[data-seat-vip]").textContent = vip;
            roomPreview.querySelector("[data-seat-normal]").textContent = rows * columns - vip;
        };
        rowsInput.addEventListener("input", renderCapacity);
        columnsInput.addEventListener("input", renderCapacity);
        renderCapacity();
    }

    const showtimePreview = document.querySelector("[data-showtime-preview]");
    if (showtimePreview) {
        const movieSelect = document.querySelector("#movieId");
        const startInput = document.querySelector("#startTime");
        const range = showtimePreview.querySelector("[data-showtime-range]");
        // Khoảng nghỉ giữa hai suất do máy chủ gửi xuống (app.showtime.break-minutes), không ghi cứng.
        const breakMinutes = Number.parseInt(showtimePreview.dataset.breakMinutes, 10) || 0;
        const twoDigits = value => String(value).padStart(2, "0");
        const renderShowtime = () => {
            const option = movieSelect.selectedOptions[0];
            const duration = Number.parseInt(option?.dataset.duration, 10);
            const start = startInput.value ? new Date(startInput.value) : null;
            if (!duration || !start || Number.isNaN(start.getTime())) {
                range.textContent = "Chọn phim và giờ bắt đầu";
                return;
            }
            const end = new Date(start.getTime() + (duration + breakMinutes) * 60 * 1000);
            const endsNextDay = end.getFullYear() !== start.getFullYear()
                || end.getMonth() !== start.getMonth() || end.getDate() !== start.getDate();
            range.textContent = `${twoDigits(start.getHours())}:${twoDigits(start.getMinutes())} – ${twoDigits(end.getHours())}:${twoDigits(end.getMinutes())}${endsNextDay ? " (+1 ngày)" : ""}, ${twoDigits(start.getDate())}/${twoDigits(start.getMonth() + 1)}`;
        };
        movieSelect.addEventListener("change", renderShowtime);
        startInput.addEventListener("input", renderShowtime);
        renderShowtime();
    }
});
