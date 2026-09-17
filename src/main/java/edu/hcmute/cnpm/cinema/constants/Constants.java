package edu.hcmute.cnpm.cinema.constants;

public class Constants {
    private Constants() {}

    public static final int SEAT_HOLD_MINUTES = 5;

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
