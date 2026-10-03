# Mã vé 8 số và QR chung hóa đơn — Nhóm 8

Chạy `CinemaBookingApplication` như bình thường. Profile cloud mặc định kiểm tra schema; không tự sửa database. Migration cho cloud có trong `database/add-ticket-public-codes.sql`, cũng đã ghép vào `database/schema-cloud.sql`. Spring tự bổ sung mã cho vé lịch sử sau khi schema đã cập nhật. Không cần server quét QR hay dịch vụ QR bên ngoài.

## Hóa đơn và vé

- Thanh toán được backend xác nhận thì mở thẳng `/hoa-don/{receiptCode}`, có trạng thái thành công, đầy đủ khoản tiền và mã vé.
- Mua nhiều ghế trong một hóa đơn: một khối vé, **một QR chung** và bảng **ghế — mã 8 số — trạng thái**. Quét QR chung lấy đúng danh sách vé liên kết với hóa đơn; không lấy mọi vé cùng khách/suất.
- Mua riêng một ghế: một mã 8 số và QR riêng tham chiếu cùng vé.
- **In hóa đơn và vé** in cùng QR và bảng ghế. **Lưu ảnh QR** tải PNG có cùng nội dung với QR trên hóa đơn.
- Vé hủy vẫn có mã lịch sử, được ghi rõ đã hủy. Hủy hết vé thì không còn QR dùng để vào rạp. QR đã lưu trước đó cũng bị backend từ chối cho vào.
- Trang Vé của tôi đưa đơn nhiều ghế tới QR chung trên hóa đơn. Mã từng ghế vẫn dùng được nếu khách trong nhóm tới riêng.

## Soát vé

1. Đăng nhập STAFF hoặc ADMIN, mở **Soát vé**.
2. Bấm **Mở camera laptop** rồi cấp quyền; camera cần HTTPS hoặc localhost. Có thể chọn PNG/JPG/WEBP/BMP từ thư mục trên máy hoặc nhập mã 8 số.
3. Quét một QR vé hoặc QR chung. Ảnh tối đa 10 MB, 25 triệu điểm ảnh, được đọc ngay trên máy. Khi phát hiện nhiều QR khác nhau, yêu cầu chọn một QR, không tự chọn vé.
4. Quét chỉ tra cứu. QR chung hiện phim/suất/phòng và từng ghế, mã, kết quả hợp lệ/đã sử dụng/không hợp lệ.
5. Bấm **Cho vào phòng** để xác nhận. Với nhóm, chỉ đánh dấu những vé còn hợp lệ; vé đã dùng giữ nguyên thời điểm, vé đã hủy không được cho vào. Khách tới riêng có thể tra và xác nhận từng mã 8 số.
6. Quét lại báo đã sử dụng. Camera tắt khi đọc được mã, bấm tắt, rời trang hoặc chọn ảnh.

## Định danh và validation

- Mã ngẫu nhiên SecureRandom từ 10000000 đến 99999999, duy nhất, cố định; tách khỏi ID nội bộ. Không coi độ dài mã là quyền truy cập: tra cứu/xác nhận vẫn bắt buộc tài khoản nhân viên.
- Bảng `ticket_public_codes` có UNIQUE và CHECK đúng 8 số. Giữ bản ghi khi vé bị hủy/xóa để mã cũ không cấp lại. Khóa SQL Server dùng chung giữa các máy khi cấp mã; thử lại khi gặp trùng, lỗi thì không ghi đè.
- QR vé riêng: `UTE-CINEMA:TICKET:V2:<8 số>`. QR chung: `UTE-CINEMA:BOOKING:V2:<receiptCode>`. QR không chứa danh sách ghế do trình duyệt tự khai; backend đọc quan hệ hóa đơn–vé từ database.
- ID ngắn và QR phiên bản cũ ngừng dùng. Mở lại hóa đơn/Vé của tôi để lấy mã và QR mới. Không thêm số 0 vào ID.
- Nhập tay nhận đúng 8 chữ số (có thể thêm #), bỏ khoảng trắng ngoài; từ chối rỗng, 0/âm, chữ, số quá dài, số thập phân, URL/QR thanh toán, QR giả định dạng.
- PNG chỉ dành cho chủ vé/đơn, STAFF hoặc ADMIN; vé/đơn đã thanh toán, phản hồi no-store. Tài khoản khác nhận 404.
- Trước xác nhận: tồn tại, đã thanh toán, chưa dùng, phim hoạt động, ghế đúng phòng, thời gian suất hợp lệ/chưa hết và đã đến ngày chiếu theo giờ Việt Nam. Nhóm còn kiểm tra cùng chủ đơn, cùng suất, đúng hóa đơn.
- POST kiểm tra CSRF phiên và khớp mã vé–ID hoặc QR–hóa đơn. Khóa suất cùng thanh toán/hoàn vé và cập nhật SQL có điều kiện kiểm tra trạng thái/thời gian/phòng/đơn. Xác nhận nhóm trong một giao dịch: một vé thay đổi thì rollback toàn bộ lần xác nhận.
- Trạng thái tài chính vẫn PAID; checked_in_at ghi đã sử dụng. Không hoàn vé đã dùng.
- Hoàn vé lưu booking_order_id gốc. Chứng từ lịch sử chỉ ghép khi cùng chủ/suất, chính xác thời điểm/ref và có duy nhất một hóa đơn khớp; không đoán khi nhiều đơn trùng.

## Database và kiểm thử

- Migration chỉ thêm bảng mã, thêm cột nullable vào chứng từ hoàn tiền và ghép chứng từ lịch sử khi chắc chắn; không xóa vé, khách, đơn hay khoản tiền.
- Không xóa/truncate bảng mã trong database nhóm. Các test xóa dữ liệu chỉ chạy database riêng có hậu tố _test.
- jsQR 1.4.0 lưu cục bộ tại static/js/vendor, Apache-2.0, không CDN. [Nguồn jsQR](https://github.com/cozmo/jsQR).
- ZXing tạo SVG/PNG. [Quyền camera](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia).
- [Khóa SQL Server sp_getapplock](https://learn.microsoft.com/en-us/sql/relational-databases/system-stored-procedures/sp-getapplock-transact-sql).
