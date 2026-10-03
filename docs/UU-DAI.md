# Ưu đãi và menu tài khoản

Danh sách voucher và điều kiện áp dụng được đọc trực tiếp từ bảng `dbo.vouchers` trong database. Không còn cấu hình `app.promotions.offers` trong `application.properties`.

| Cột trong vouchers | Ý nghĩa |
| --- | --- |
| `id` | Mã định danh tự tăng |
| `code` | Mã voucher duy nhất, ví dụ UTE10 |
| `title` | Tên ưu đãi |
| `discount_percent` | Phần trăm giảm; 0 khi giảm tiền cố định |
| `discount_amount` | Tiền giảm cố định; 0 khi giảm theo phần trăm |
| `minimum_ticket_subtotal` | Tiền vé tối thiểu, không tính bắp nước |
| `maximum_discount` | Số tiền giảm tối đa |
| `starts_on`, `ends_on` | Ngày bắt đầu/kết thúc; NULL nếu không giới hạn |
| `active` | 1: đang áp dụng; 0: ngừng áp dụng |

Hai mã mẫu: `UTE10` giảm 10% tiền vé (tối đa 30.000 đ, tiền vé tối thiểu 100.000 đ); `UTE20K` giảm 20.000 đ (tiền vé tối thiểu 200.000 đ). Mỗi đơn dùng một mã, không giảm bắp nước. Chọn giảm theo phần trăm hoặc tiền cố định; database kiểm tra quy tắc này và thời hạn. Ngày kết thúc tính đến hết ngày theo giờ Việt Nam.

Xem voucher trong SQL Server:

```sql
SELECT * FROM dbo.vouchers ORDER BY code;
```

Ví dụ ngừng một mã:

```sql
UPDATE dbo.vouchers SET active = 0 WHERE code = 'UTE10';
```

Thay đổi điều kiện trong database có hiệu lực khi ứng dụng đọc lại, không cần khởi động lại để đổi voucher.

`booking_orders.voucher_code` và `booking_orders.discount_amount` lưu mã đã dùng và khoản giảm của giao dịch. Đơn đã trả giữ nguyên các giá trị này khi chỉnh sửa hoặc ngừng voucher; không gắn khóa ngoại từ hóa đơn vào voucher để giữ lịch sử.

Máy chủ kiểm tra mã và điều kiện, không nhận số tiền giảm từ trình duyệt. Đơn nháp được kiểm tra lại khi thay đổi và trước khi thanh toán. Nếu mã mất hiệu lực hoặc không đủ điều kiện, hệ thống bỏ mã và cập nhật tổng tiền; giao dịch cũ sai số tiền bị từ chối. Sau khi đổi mã, cần tạo lại giao dịch QR/OTP.

Khoản giảm được phân bổ theo giá từng vé; giá thực trả dùng để tính doanh thu và hoàn tiền, giá gốc giữ lại cho hóa đơn kể cả khi vé đã hủy.

Menu gồm Hồ sơ của tôi, Ưu đãi, Lịch sử đặt vé, Đăng xuất; trang hiện tại được đánh dấu. `/lich-su-dat-ve` hiển thị giao dịch và chi tiết đơn; liên kết `/ve-cua-toi` cũ chuyển tới lịch sử.

Thanh điều hướng chính nằm bên trái, menu hồ sơ nằm bên phải. Ưu đãi và Vé của tôi chỉ xuất hiện trong menu hồ sơ. Tiền hiển thị theo định dạng Việt Nam, ví dụ `200.000 đ`. Sau thanh toán, khách tới thẳng hóa đơn A4, có tiền vé, bắp nước, voucher, tổng thanh toán và QR chung với mã vé 8 số. Trang ví điện thoại nạp `money.js` trước `demo-wallet.js`, cả hai tài nguyên được gateway cho phép.

Chạy `database/migrations/20261002-promotions.sql` để bổ sung các cột giảm giá và giá gốc. Chạy `database/migrations/20261002-vouchers.sql` để tạo bảng và thêm hai mã mẫu. Script không ghi đè mã đã tồn tại. Schema local/cloud cũng có phần tạo bảng và thêm mã mẫu cho lần cài đặt mới.

Sau tích hợp, chạy thêm `20261003-voucher-title-length.sql` để mở rộng tên đã lưu lên 200 ký tự và `20261003-draft-order-totals.sql` để đồng bộ tổng tiền đơn nháp cũ. Hai migration đã kiểm tra chạy lại an toàn; không thay đổi chứng từ PAID/CANCELLED. Migration lịch sử lấy `original_price` khi có để hóa đơn không trừ voucher hai lần.

Web local và Render phải cùng bản tích hợp. `/demo-wallet/health` của bản này trả `status=UP` và `checkoutVersion=2`; chỉ `UP` chưa chứng minh ví hỗ trợ voucher. Trên Render, `money.js` phải trả 200. Kiểm chứng hiện tại: xem [báo cáo](MODULE_2_INTEGRATION_REVIEW.md).

Đã áp dụng migration voucher lên cloud ngày 02/10/2026 và xác nhận hai mã UTE10, UTE20K cùng điều kiện nằm trong bảng `dbo.vouchers`. Toàn bộ 473 kiểm thử đạt với `mvn -q -Dspring.jpa.hibernate.ddl-auto=validate test`, gồm đọc thay đổi voucher từ database, ngừng mã và giữ nguyên hóa đơn đã thanh toán.
