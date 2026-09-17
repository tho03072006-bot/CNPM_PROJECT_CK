package edu.hcmute.cnpm.cinema.exception;

/**
 * Nem ra khi khong tim thay du lieu theo ma (id) - phim, phong chieu, suat chieu, ghe, ve...
 *
 * Cach dung goi y trong Service:
 * <pre>
 *   Movie movie = movieRepository.findById(movieId)
 *           .orElseThrow(() -&gt; new ResourceNotFoundException("phim", movieId));
 * </pre>
 *
 * {@link GlobalExceptionHandler} se doi loi nay thanh trang 404.
 *
 * Phu trach: Tho (Module 4 - Kien truc dung chung).
 */
public class ResourceNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * @param resourceName ten du lieu bang tieng Viet de hien thi cho nguoi dung,
     *                     vi du "phim", "suat chieu", "ghe"
     * @param resourceId   ma cua du lieu khong tim thay
     */
    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super("Khong tim thay " + resourceName + " (ma: " + resourceId + ").");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
