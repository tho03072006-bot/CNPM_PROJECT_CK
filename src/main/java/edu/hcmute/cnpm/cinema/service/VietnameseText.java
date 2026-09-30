package edu.hcmute.cnpm.cinema.service;

import java.text.Normalizer;
import java.util.Locale;

/** Xử lý chữ tiếng Việt dùng chung cho các ô tìm kiếm. */
final class VietnameseText {

    private VietnameseText() {}

    /**
     * Bỏ dấu, đổi đ thành d và về chữ thường để so khớp.
     *
     * Nhờ vậy gõ "bong ma" vẫn tìm ra "Bóng Ma Nhà Hát", vì nhiều người gõ trên máy
     * không bật bộ gõ tiếng Việt. Chữ đ phải đổi tay vì nó là một chữ riêng chứ không
     * phải chữ d cộng dấu, Normalizer không tách ra được.
     */
    static String fold(String text) {
        if (text == null) {
            return "";
        }
        String withoutMarks = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutMarks.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
}
