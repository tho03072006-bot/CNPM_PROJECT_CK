/* Định dạng hiển thị; dữ liệu gửi máy chủ vẫn là số. */
(() => {
    'use strict';
    const formatter = new Intl.NumberFormat('vi-VN', {
        useGrouping: true, minimumFractionDigits: 0, maximumFractionDigits: 0
    });
    window.CinemaMoney = Object.freeze({
        format(amount) {
            return formatter.format(amount == null ? 0 : amount) + ' đ';
        }
    });
})();
