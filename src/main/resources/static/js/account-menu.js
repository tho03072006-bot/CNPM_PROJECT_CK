// Đóng menu tài khoản khi nhấn ra ngoài, chọn liên kết hoặc nhấn Escape.
document.querySelectorAll('.account-dropdown').forEach(function (menu) {
    document.addEventListener('click', function (event) {
        if (!menu.contains(event.target) || event.target.closest('a')) menu.open = false;
    });
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && menu.open) {
            menu.open = false;
            menu.querySelector('summary').focus();
        }
    });
});
