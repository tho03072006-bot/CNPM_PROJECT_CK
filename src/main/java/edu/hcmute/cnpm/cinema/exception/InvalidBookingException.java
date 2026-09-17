package edu.hcmute.cnpm.cinema.exception;

/**
 * Nem ra khi yeu cau dat ve khong hop le ve mat nghiep vu, vi du:
 *   - suat chieu da bat dau hoac da chieu xong;
 *   - ve da het han giu (qua {@code Constants.SEAT_HOLD_MINUTES} phut);
 *   - dat qua so ghe toi da cho 1 lan;
 *   - thanh toan cho ve khong con o trang thai HELD;
 *   - ghe duoc chon khong thuoc phong chieu cua suat chieu do.
 *
 * Message truyen vao phai noi RO ly do cho nguoi dung biet phai lam gi tiep,
 * vi du: "Suat chieu nay da bat dau, ban khong the dat ve nua."
 *
 * Phu trach: Tho (Module 4) - nguoi dung chinh: Thang (Module 2), Thanh (Module 3).
 */
public class InvalidBookingException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InvalidBookingException(String message) {
        super(message);
    }

    public InvalidBookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
