package edu.hcmute.cnpm.cinema.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Đọc số tiền thành chữ cho dòng "Số tiền viết bằng chữ" trên hóa đơn.
 *
 * Theo cách đọc trên chứng từ kế toán: 1.005 là "một nghìn không trăm linh năm",
 * 21 là "hai mươi mốt", 15 là "mười lăm".
 */
final class VietnameseMoneyWords {

    private static final String[] DIGITS = {
            "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };
    private static final String[] SCALES = {"", "nghìn", "triệu", "tỷ"};

    private VietnameseMoneyWords() {}

    /** Ví dụ 280000 thành "Hai trăm tám mươi nghìn đồng chẵn." */
    static String of(BigDecimal amount) {
        long value = amount == null ? 0 : amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (value < 0) {
            throw new IllegalArgumentException("Số tiền trên hóa đơn không được âm.");
        }
        String words = read(value) + " đồng" + (value > 0 && value % 1000 == 0 ? " chẵn" : "") + ".";
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    static String read(long number) {
        if (number == 0) {
            return DIGITS[0];
        }
        List<Integer> groups = new ArrayList<>();
        for (long rest = number; rest > 0; rest /= 1000) {
            groups.add((int) (rest % 1000));
        }
        List<String> words = new ArrayList<>();
        for (int index = groups.size() - 1; index >= 0; index--) {
            int group = groups.get(index);
            if (group == 0) {
                continue;
            }
            readGroup(group, index == groups.size() - 1, words);
            String scale = scaleName(index);
            if (!scale.isEmpty()) {
                words.add(scale);
            }
        }
        return String.join(" ", words);
    }

    /** Nhóm ba chữ số. Nhóm đứng đầu không đọc "không trăm", các nhóm sau thì có. */
    private static void readGroup(int group, boolean leading, List<String> words) {
        int hundreds = group / 100;
        int tens = group / 10 % 10;
        int units = group % 10;
        boolean readHundreds = !leading || hundreds > 0;
        if (readHundreds) {
            words.add(DIGITS[hundreds]);
            words.add("trăm");
        }
        if (tens == 0) {
            if (units != 0 && readHundreds) {
                words.add("linh");
            }
        } else if (tens == 1) {
            words.add("mười");
        } else {
            words.add(DIGITS[tens]);
            words.add("mươi");
        }
        if (units == 0) {
            return;
        }
        if (units == 1 && tens > 1) {
            words.add("mốt");
        } else if (units == 5 && tens > 0) {
            words.add("lăm");
        } else {
            words.add(DIGITS[units]);
        }
    }

    /** 0 hàng đơn vị, 1 nghìn, 2 triệu, 3 tỷ, 4 nghìn tỷ, 5 triệu tỷ, 6 tỷ tỷ. */
    private static String scaleName(int index) {
        if (index < SCALES.length) {
            return SCALES[index];
        }
        String rest = scaleName(index - 3);
        return rest.isEmpty() ? "tỷ" : rest + " tỷ";
    }
}
