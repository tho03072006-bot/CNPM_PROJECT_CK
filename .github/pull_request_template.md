## Pull Request này làm gì

<!-- Mô tả ngắn gọn bằng tiếng Việt. Ví dụ: "Thêm màn hình chọn ghế cho một suất chiếu." -->

## Thuộc công việc nào

<!-- Ghi mã công việc trong docs/PHAN_CONG.md, ví dụ M2.3. Nếu có Issue thì ghi "Closes #12". -->

## Đã tự kiểm tra thế nào

<!-- Ghi rõ đã thử những trường hợp nào, kể cả trường hợp lỗi.
     Ví dụ: "Đặt ghế A1 bằng 2 trình duyệt cùng lúc -> trình duyệt thứ hai báo ghế đã có người giữ." -->

## Ảnh chụp màn hình

<!-- Bắt buộc nếu PR có thay đổi giao diện. Có cả chế độ sáng và tối thì càng tốt. -->

---

## Bảng kiểm trước khi nhờ duyệt

- [ ] `mvn test` chạy xanh trên máy tôi
- [ ] Không có logic nghiệp vụ nằm trong Controller (Controller gọi Service, Service gọi Repository)
- [ ] Có xử lý lỗi: ném `BusinessException` hoặc lớp con, không để lỗi 500 thô ra màn hình
- [ ] Không hardcode chuỗi kết nối database / mật khẩu / API key
- [ ] Đặt tên biến, hàm, class đúng quy ước ở Mục 1 của `CONTRIBUTING.md`
- [ ] Không còn `System.out.println` để debug sót lại
- [ ] Giao diện dùng class có sẵn trong `style.css`, không tự viết CSS riêng cho module mình
- [ ] Không sửa file thuộc module người khác (nếu có thì đã báo trước trong nhóm chat)

## Người duyệt

<!-- Nhắc tên người phù hợp nhất để review — ưu tiên người làm module liên quan gần nhất. -->
