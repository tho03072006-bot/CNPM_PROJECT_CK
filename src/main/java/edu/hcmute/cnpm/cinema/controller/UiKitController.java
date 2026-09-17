package edu.hcmute.cnpm.cinema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang trưng bày bộ giao diện dùng chung (design system) tại đường dẫn /ui-kit.
 *
 * Đây là trang dành cho NỘI BỘ NHÓM, không phải chức năng cho khách hàng: mở trang này
 * để xem sẵn các thành phần giao diện đã có (nút, thẻ, form, bảng, sơ đồ ghế...) rồi
 * chép đúng class về dùng, thay vì mỗi người tự chế một kiểu.
 *
 * Trước khi nộp bài có thể xoá controller này và templates/ui-kit.html nếu không muốn
 * trang phụ xuất hiện trong sản phẩm cuối.
 *
 * Phụ trách: Thọ (Module 4 - Kiến trúc dùng chung).
 */
@Controller
public class UiKitController {

    @GetMapping("/ui-kit")
    public String uiKit() {
        return "ui-kit";
    }
}
