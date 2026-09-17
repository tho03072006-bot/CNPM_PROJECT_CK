/* =====================================================================
   Cinema Booking - NHÓM 05 - Chuyển chế độ sáng / tối
   Phụ trách: Thọ (Module 4)

   Cách hoạt động:
   - Đặt thuộc tính data-theme="light" hoặc "dark" trên thẻ <html>,
     CSS trong style.css tự đổi bảng màu theo thuộc tính này.
   - Lựa chọn của người dùng lưu vào localStorage nên lần sau vào vẫn nhớ.
   - Chưa chọn bao giờ thì KHÔNG đặt data-theme, để trang đi theo cài đặt
     sáng/tối của hệ điều hành.

   Lưu ý: đoạn script chống nháy màn hình nằm ngay trong <head> của
   layout/base.html, phải chạy TRƯỚC khi trang vẽ. File này chỉ lo phần
   bấm nút, được nạp ở cuối <body>.
   ===================================================================== */
(function () {
    "use strict";

    var STORAGE_KEY = "nhom05-theme";
    var THEME_LIGHT = "light";
    var THEME_DARK = "dark";

    /** Đọc lựa chọn đã lưu. Trả về null nếu người dùng chưa từng chọn. */
    function getSavedTheme() {
        try {
            var saved = window.localStorage.getItem(STORAGE_KEY);
            return (saved === THEME_LIGHT || saved === THEME_DARK) ? saved : null;
        } catch (error) {
            // Trình duyệt chặn localStorage (chế độ ẩn danh) thì coi như chưa chọn.
            return null;
        }
    }

    function saveTheme(theme) {
        try {
            window.localStorage.setItem(STORAGE_KEY, theme);
        } catch (error) {
            // Không lưu được thì thôi, trang vẫn đổi màu bình thường trong phiên này.
        }
    }

    /** Chế độ đang hiển thị thực tế, tính cả trường hợp đang theo hệ điều hành. */
    function getCurrentTheme() {
        var chosen = document.documentElement.getAttribute("data-theme");
        if (chosen === THEME_LIGHT || chosen === THEME_DARK) {
            return chosen;
        }
        return window.matchMedia("(prefers-color-scheme: dark)").matches ? THEME_DARK : THEME_LIGHT;
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute("data-theme", theme);
        updateToggleLabel(theme);
    }

    /**
     * Cập nhật nhãn cho người dùng dùng trình đọc màn hình biết nút này làm gì,
     * vì bản thân biểu tượng mặt trời/mặt trăng thì họ không "nhìn" được.
     */
    function updateToggleLabel(theme) {
        var toggle = document.querySelector("[data-theme-toggle]");
        if (!toggle) {
            return;
        }
        var label = (theme === THEME_DARK) ? "Chuyển sang giao diện sáng" : "Chuyển sang giao diện tối";
        toggle.setAttribute("aria-label", label);
        toggle.setAttribute("title", label);
        toggle.setAttribute("aria-pressed", theme === THEME_DARK ? "true" : "false");
    }

    function setUpToggle() {
        var toggle = document.querySelector("[data-theme-toggle]");
        if (!toggle) {
            return;
        }

        updateToggleLabel(getCurrentTheme());

        toggle.addEventListener("click", function () {
            var nextTheme = (getCurrentTheme() === THEME_DARK) ? THEME_LIGHT : THEME_DARK;
            applyTheme(nextTheme);
            saveTheme(nextTheme);
        });
    }

    /** Người dùng chưa tự chọn thì đổi theo hệ điều hành ngay khi họ đổi cài đặt. */
    function followSystemWhenNotChosen() {
        window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", function () {
            if (getSavedTheme() === null) {
                document.documentElement.removeAttribute("data-theme");
                updateToggleLabel(getCurrentTheme());
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", setUpToggle);
    } else {
        setUpToggle();
    }

    followSystemWhenNotChosen();
})();
