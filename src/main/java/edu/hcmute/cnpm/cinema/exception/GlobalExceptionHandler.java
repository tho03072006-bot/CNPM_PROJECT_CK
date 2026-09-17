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
 * Bat loi TAP TRUNG cho toan bo ung dung.
 *
 * Nho lop nay, 3 module con lai KHONG phai tu try/catch roi tu render trang loi:
 * cu nem {@link BusinessException} (hoac lop con cua no) tu tang Service la duoc,
 * o day se lo phan doi sang ma HTTP va hien thi cho nguoi dung.
 *
 * Tra ve cai gi:
 *   - Request thuong (nguoi dung bam link, submit form) -> trang error.html theo layout chung.
 *   - Request AJAX / API (header X-Requested-With: XMLHttpRequest, hoac Accept: application/json)
 *     -> JSON dang {"success": false, "message": "..."} de JavaScript hien thong bao tai cho.
 *     Phan nay danh cho man hinh chon ghe cua Module 2 (bam giu ghe bang AJAX).
 *
 * CO Y KHONG bat {@code Exception.class}: de loi ngoai du kien roi ve co che xu ly loi san
 * cua Spring Boot (van hien thi error.html), tranh nuot mat loi 404 cua file tinh.
 *
 * Phu trach: Tho (Module 4 - Kien truc dung chung).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String DATA_CONFLICT_MESSAGE =
            "Du lieu bi trung hoac khong hop le nen khong luu duoc. Vui long tai lai trang va thu lai.";

    /** Ghe da co nguoi khac giu -> 409 Conflict (ADR-001). */
    @ExceptionHandler(SeatAlreadyTakenException.class)
    public Object handleSeatAlreadyTaken(SeatAlreadyTakenException ex, HttpServletRequest request) {
        log.warn("Giu ghe that bai vi da co nguoi giu truoc: showtimeId={}, seatId={}",
                ex.getShowtimeId(), ex.getSeatId());
        return buildErrorResponse(request, HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Khong tim thay du lieu -> 404 Not Found. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Khong tim thay du lieu cho duong dan {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(request, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Yeu cau dat ve khong hop le -> 400 Bad Request. */
    @ExceptionHandler(InvalidBookingException.class)
    public Object handleInvalidBooking(InvalidBookingException ex, HttpServletRequest request) {
        log.warn("Yeu cau dat ve khong hop le tai {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Cac loi nghiep vu con lai -> 400 Bad Request. */
    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Loi nghiep vu tai {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(request, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Luoi an toan cho ADR-001: neu Service quen doi {@link DataIntegrityViolationException}
     * thanh {@link SeatAlreadyTakenException} thi nguoi dung van thay thong bao tu te
     * chu khong thay loi 500. Van ghi log ERROR de con biet duong ma sua lai Service.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                               HttpServletRequest request) {
        log.error("Vi pham rang buoc du lieu tai {} - Service nen bat va doi thanh BusinessException",
                request.getRequestURI(), ex);
        return buildErrorResponse(request, HttpStatus.CONFLICT, DATA_CONFLICT_MESSAGE);
    }

    /**
     * Dung 1 thong bao loi -> tra ve trang HTML hoac JSON tuy theo ai goi.
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

    /** Request nay do JavaScript goi (AJAX/API) hay do trinh duyet mo trang? */
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
