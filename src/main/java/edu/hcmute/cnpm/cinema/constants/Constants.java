package edu.hcmute.cnpm.cinema.constants;

public class Constants {
    private Constants() {}

    public static final int SEAT_HOLD_MINUTES = 5;

    // ===== He so gia theo loai ghe =====
    // Gia cuoi cung cua mot ghe = base_price cua suat chieu x he so duoi day.
    // De o day (khong de rieng trong tung service) vi ca Module 2 khi tinh tien
    // lan trang bang gia ve cua Module 4 deu phai dung CUNG mot con so - lech
    // nhau la khach thay mot gia tren bang gia, tra mot gia khac luc dat ve.
    /** Ghe thuong: giu nguyen gia goc. */
    public static final String SEAT_PRICE_MULTIPLIER_NORMAL = "1";
    /** Ghe VIP: cong them 50%. */
    public static final String SEAT_PRICE_MULTIPLIER_VIP = "1.5";
    /** Ghe doi (Sweetbox): gap doi vi ngoi duoc hai nguoi. */
    public static final String SEAT_PRICE_MULTIPLIER_COUPLE = "2";

    // Ten session attribute luu thong tin user dang dang nhap
    public static final String SESSION_USER = "LOGGED_IN_USER";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    // ===== Ten view dung chung =====
    /** Trang bao loi chung: src/main/resources/templates/error.html */
    public static final String VIEW_ERROR = "error";

    // ===== Ten bien trong Model / Flash attribute =====
    // Dung DUNG 2 ten nay cho thong bao, dung tu che ten khac, vi fragment
    // "fragments/alert.html" (da nhung san trong layout/base.html) chi doc 2 ten nay.
    //
    // Vi du sau khi luu thanh cong trong Controller:
    //   redirectAttributes.addFlashAttribute(Constants.MODEL_SUCCESS_MESSAGE, "Da luu phim moi.");
    //   return "redirect:/admin/movies";
    /** Thong bao thanh cong (mau xanh). */
    public static final String MODEL_SUCCESS_MESSAGE = "successMessage";
    /** Thong bao loi (mau do). */
    public static final String MODEL_ERROR_MESSAGE = "errorMessage";

    /** Ma loi HTTP hien thi tren trang error.html. */
    public static final String MODEL_ERROR_CODE = "errorCode";
    /** Duong dan gay ra loi, hien thi tren trang error.html. */
    public static final String MODEL_ERROR_PATH = "errorPath";
}
