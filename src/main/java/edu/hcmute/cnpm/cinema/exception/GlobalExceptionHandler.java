package edu.hcmute.cnpm.cinema.exception;

import edu.hcmute.cnpm.cinema.constants.Constants;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bắt lỗi TẬP TRUNG cho toàn bộ ứng dụng.
 *
 * Nhờ lớp này, 3 module còn lại KHÔNG phải tự try/catch rồi tự render trang lỗi:
 * cứ ném {@link BusinessException} (hoặc lớp con của nó) từ tầng Service là được,
 * ở đây sẽ lo phần đổi sang mã HTTP và hiển thị cho người dùng.
 *
 * Trả về cái gì:
 *   - Request thường (người dùng bấm link, submit form) -> trang error.html theo layout chung.
 *   - Request AJAX / API (header X-Requested-With: XMLHttpRequest, hoặc Accept: application/json)
 *     -> JSON dạng {"success": false, "message": "..."} để JavaScript hiện thông báo tại chỗ.
 *     Phần này dành cho màn hình chọn ghế của Module 2 (bấm giữ ghế bằng AJAX).
 *
 * CỐ Ý KHÔNG bắt {@code Exception.class}: để lỗi ngoài dự kiến rơi về cơ chế xử lý lỗi sẵn
 * của Spring Boot (vẫn hiển thị error.html), tránh nuốt mất lỗi 404 của file tĩnh.
 *
 * Phụ trách: Thọ (Module 4 - Kiến trúc dùng chung).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String DATA_CONFLICT_MESSAGE =
            "Dữ liệu bị trùng hoặc không hợp lệ nên không lưu được. Vui lòng tải lại trang và thử lại.";

    /** Ghế đã có người khác giữ -> 409 Conflict (ADR-1). */
    @ExceptionHandler(SeatAlreadyTakenException.class)
    public Object handleSeatAlreadyTaken(SeatAlreadyTakenException ex, HttpServletRequest request) {
        log.warn("Giữ ghế thất bại vì đã có người giữ trước: showtimeId={}, seatId={}",
                ex.getShowtimeId(), ex.getSeatId());
        return buildErrorResponse(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Không tìm thấy dữ liệu -> 404 Not Found. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Không tìm thấy dữ liệu cho đường dẫn {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Yêu cầu đặt vé không hợp lệ -> 400 Bad Request. */
    @ExceptionHandler(InvalidBookingException.class)
    public Object handleInvalidBooking(InvalidBookingException ex, HttpServletRequest request) {
        log.warn("Yêu cầu đặt vé không hợp lệ tại {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Các lỗi nghiệp vụ còn lại -> 400 Bad Request. */
    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Lỗi nghiệp vụ tại {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Lưới an toàn cho ADR-1: nếu Service quên đổi {@link DataIntegrityViolationException}
     * thành {@link SeatAlreadyTakenException} thì người dùng vẫn thấy thông báo tử tế
     * chứ không thấy lỗi 500. Vẫn ghi log ERROR để còn biết đường mà sửa lại Service.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                               HttpServletRequest request) {
        log.error("Vi phạm ràng buộc dữ liệu tại {} - Service nên bắt và đổi thành BusinessException",
                request.getRequestURI(), ex);
        return buildErrorResponse(request, HttpStatus.CONFLICT, DATA_CONFLICT_MESSAGE);
    }

    /**
     * Dựng 1 thông báo lỗi -> trả về trang HTML hoặc JSON tuỳ theo ai gọi.
     */
    private Object buildErrorResponse(HttpServletRequest request, HttpStatus status, String userMessage) {
        if (prefersJsonResponse(request)) {
            Map<String, Object> responseBody = new LinkedHashMap<>();
            responseBody.put("success", false);
            responseBody.put("message", userMessage);
            return ResponseEntity.status(status).body(responseBody);
        }

        ModelAndView modelAndView = new ModelAndView(Constants.VIEW_ERROR);
        modelAndView.setStatus(status);
        modelAndView.addObject(Constants.MODEL_ERROR_CODE, status.value());
        modelAndView.addObject(Constants.MODEL_ERROR_MESSAGE, userMessage);
        modelAndView.addObject(Constants.MODEL_ERROR_PATH, request.getRequestURI());
        return modelAndView;
    }

    /** Request này do JavaScript gọi (AJAX/API) hay do trình duyệt mở trang? */
    private boolean prefersJsonResponse(HttpServletRequest request) {
        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return true;
        }
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        return acceptHeader != null
                && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE)
                && !acceptHeader.contains(MediaType.TEXT_HTML_VALUE);
    }
}
