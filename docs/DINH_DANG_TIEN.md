# Định dạng tiền trong project

Tiền hiển thị theo dạng Việt Nam: `200.000 đ`, `1.234.567 đ`, `0 đ`. Không hiện phần thập phân `.00`, dùng dấu chấm ngăn hàng nghìn và một khoảng trắng trước đơn vị. Trong câu dài có thể dùng đơn vị `đồng`.

- Java: `MoneyFormatter.format(amount)` cho cả số và đơn vị; `MoneyFormatter.digits(amount)` cho phần số.
- Thymeleaf: `${@money.number(amount)}` rồi thêm `đ`/`đồng` theo nội dung câu.
- JavaScript: `window.CinemaMoney.format(amount)`. `money.js` được nạp trong phần head của layout, trước các đoạn script cập nhật số tiền.

Bộ định dạng làm tròn phần hiển thị tới một đồng theo HALF_UP và không thay đổi số tiền gốc. Email, thông báo voucher, hoàn vé, hoàn MoMo và các trang khách hàng/nhân viên/quản trị dùng cùng quy tắc.

Database vẫn lưu tiền bằng DECIMAL/NUMERIC; `200000.00` khi xem trực tiếp trong SQL Server là giá trị số đúng, không phải chuỗi hiển thị. Không lưu dấu chấm phân cách hàng nghìn hay đơn vị tiền vào cột số. API và giá trị nhập/gửi trong form vẫn là số để tính toán và kiểm tra dữ liệu.

Rà soát ngày 03/10/2026:

- Cloud: kiểm tra 15 cột tiền, không có giá trị lẻ dưới một đồng; quét 57 cột văn bản với 2.590 giá trị, không phát hiện chuỗi tiền sai định dạng.
- Local `cinema_booking`: kiểm tra 2 cột tiền và 14 cột văn bản với 286 giá trị, không phát hiện chuỗi tiền sai định dạng hoặc tiền có phần lẻ.
- Không cần cập nhật dữ liệu tiền hay đổi schema trong database.

Kiểm thử gồm số tiền có scale `.00`, hàng nghìn/triệu, số 0, số âm, số lớn, làm tròn phần hiển thị và thông báo điều kiện UTE20K từ giá trị `200000.00` trong database.

Kết quả: toàn bộ 484 kiểm thử Maven đạt với `mvn -q -Dspring.jpa.hibernate.ddl-auto=validate test`. Kiểm tra JavaScript đạt 6 trường hợp định dạng tiền, cú pháp 8 file script và phép cộng tiền bắp nước với dữ liệu giá có scale `.00`.
