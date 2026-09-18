package edu.hcmute.cnpm.cinema.exception;

/**
 * Lớp CHA cho mọi lỗi NGHIỆP VỤ của hệ thống (ghế đã có người giữ, không tìm thấy phim,
 * suất chiếu đã bắt đầu...).
 *
 * Nguyên tắc dùng:
 *   - Tầng Service ném exception loại này khi nghiệp vụ không cho phép tiếp tục.
 *   - Message truyền vào phải là câu NGƯỜI DÙNG ĐỌC HIỂU, vì nó sẽ được hiển thị thẳng
 *     ra màn hình. Không nhét tên class hay stack trace vào message.
 *   - Exception này CỐ Ý KHÔNG mang mã HTTP: tầng Service không cần biết gì về web.
 *     Việc đổi sang mã HTTP (404 / 409 / 400) là của {@link GlobalExceptionHandler}.
 *
 * Là RuntimeException (unchecked) nên không bắt buộc khai báo throws - tránh cho code
 * của 4 người đầy try/catch lung tung.
 *
 * Phụ trách: Thọ (Module 4 - Kiến trúc dùng chung).
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
