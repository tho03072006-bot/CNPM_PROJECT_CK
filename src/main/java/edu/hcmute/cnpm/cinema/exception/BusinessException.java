package edu.hcmute.cnpm.cinema.exception;

/**
 * Lop CHA cho moi loi NGHIEP VU cua he thong (ghe da co nguoi giu, khong tim thay phim,
 * suat chieu da bat dau...).
 *
 * Nguyen tac dung:
 *   - Tang Service nem exception loai nay khi nghiep vu khong cho phep tiep tuc.
 *   - Message truyen vao phai la cau NGUOI DUNG DOC HIEU, vi no se duoc hien thi thang
 *     ra man hinh. Khong nhet ten class hay stack trace vao message.
 *   - Exception nay CO Y KHONG mang ma HTTP: tang Service khong can biet gi ve web.
 *     Viec doi sang ma HTTP (404 / 409 / 400) la cua {@link GlobalExceptionHandler}.
 *
 * La RuntimeException (unchecked) nen khong bat buoc khai bao throws - tranh cho code
 * cua 4 nguoi day try/catch lung tung.
 *
 * Phu trach: Tho (Module 4 - Kien truc dung chung).
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
