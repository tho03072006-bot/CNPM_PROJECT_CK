package edu.hcmute.cnpm.cinema.service;

import edu.hcmute.cnpm.cinema.exception.InvalidBookingException;
import java.util.*;

public final class HoldIdentity {
    private HoldIdentity() {}
    public static void requireMatch(List<Long> expected, List<Long> actual) {
        if (expected == null || expected.isEmpty() || expected.size() > 8
                || expected.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(expected).size() != expected.size()
                || expected.size() != actual.size() || !new HashSet<>(expected).equals(new HashSet<>(actual))) {
            throw new InvalidBookingException("Lượt giữ ghế đã thay đổi. Vui lòng cập nhật trang trước khi tiếp tục.");
        }
    }
}
