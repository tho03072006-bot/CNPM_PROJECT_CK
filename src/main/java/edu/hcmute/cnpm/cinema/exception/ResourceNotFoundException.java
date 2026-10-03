package edu.hcmute.cnpm.cinema.exception;

/**
 * Ném ra khi không tìm thấy dữ liệu theo mã (id) - phim, phòng chiếu, suất chiếu, ghế, vé...
 *
 * Cách dùng gợi ý trong Service:
 * <pre>
 *   Movie movie = movieRepository.findById(movieId)
 *           .orElseThrow(() -&gt; new ResourceNotFoundException("phim", movieId));
 * </pre>
 *
 * {@link GlobalExceptionHandler} sẽ đổi lỗi này thành trang 404.
 *
 * Phụ trách: Thọ (Module 4 - Kiến trúc dùng chung).
 */
public class ResourceNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * @param resourceName tên dữ liệu bằng tiếng Việt để hiển thị cho người dùng,
     *                     ví dụ "phim", "suất chiếu", "ghế"
     * @param resourceId   mã của dữ liệu không tìm thấy
     */
    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super("Không tìm thấy " + resourceName + " (mã: " + resourceId + ").");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
