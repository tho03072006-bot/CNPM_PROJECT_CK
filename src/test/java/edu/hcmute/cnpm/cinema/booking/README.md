# Module 2 — Ghế và vé (Thắng)

## Phạm vi hiện tại

- `SeatService.findSeatMap(showtimeId)`: sơ đồ ghế, trạng thái và giá theo loại ghế.
- `SeatPricingService.calculateSeatPrice(basePrice, seatType)`: NORMAL giá gốc, VIP ×1,5, COUPLE ×2.
- `SeatBookingService.holdSeats(showtimeId, request, currentUser)`: giữ nhiều ghế trong một giao dịch.
- `GET /booking/showtime/{showtimeId}`: trang chọn ghế dùng layout và CSS chung.
- `POST /booking/showtime/{showtimeId}/hold`: chỉ nhận JSON `{"seatIds":[1,2]}`.
- `POST /booking/showtime/{showtimeId}/cancel`: huỷ lượt giữ của người dùng trong session.
- Thành công trả `{success, message, ticketIds, totalPrice, expiresAt}`. Lỗi nghiệp vụ dùng handler chung;
  AJAX cần header `X-Requested-With: XMLHttpRequest` hoặc `Accept: application/json`.
- JSON sai định dạng được Spring trả 400; giao diện có thông báo dự phòng khi phản hồi không có `message`.

Người dùng lấy từ session `Constants.SESSION_USER` do Module 3 cung cấp. Không tạo đăng nhập giả,
không nhận `userId` hoặc giá từ trình duyệt. Chưa tích hợp đăng nhập thì vẫn xem/chọn ghế được,
nhưng không gửi giữ ghế. Không thêm liên kết vào trang phim hay header của thành viên khác.

Validation tại service: tài khoản còn tồn tại, mã dương, danh sách ghế không rỗng/không trùng,
ghế tồn tại và đúng phòng, tối đa 8 chỗ mỗi lượt, cùng hàng/liền nhau/không để ghế lẻ,
suất còn cách giờ chiếu trên 5 phút, phim đang hoạt động, cấu hình phòng hợp lệ,
giá gốc dương và loại ghế được hỗ trợ. Một tài khoản chỉ được có một lượt giữ còn hiệu lực
cho cùng suất chiếu.

## Vòng đời giữ ghế

M2.5, M2.6 và M2.7 đã triển khai:

- Giao diện đếm ngược theo `expiresAt` do server trả về.
- Tải lại trang vẫn khôi phục đúng ghế, tổng tiền và thời gian còn lại; không đặt lại đồng hồ.
- Vé `HELD` hết hạn được xoá để giải phóng ràng buộc `UNIQUE(showtime_id, seat_id)` theo ADR-2.
- Người dùng có thể tự huỷ lượt giữ; vé chưa thanh toán cũng được xoá theo ADR-2.
- Hai request đồng thời của cùng tài khoản được tuần tự hoá bằng khoá ngắn trên tài khoản,
  ngăn tạo hai lượt giữ cho cùng suất chiếu.

Không đổi trạng thái vé hết hạn sang `EXPIRED`/`CANCELLED`, vì ràng buộc `UNIQUE` hiện tại vẫn
chặn người khác đặt lại ghế. Vé đã thanh toán không bị tác vụ dọn hoặc thao tác huỷ tác động.

## Kiểm thử

Đặt `JAVA_HOME` trỏ JDK 21 trở lên trước khi chạy Maven. Không đổi phiên bản trong `pom.xml`.

Chạy kiểm thử không dùng database:

```powershell
mvn "-Dtest=SeatPricingServiceTest,SeatSelectionPolicyTest,SeatServiceTest,SeatBookingServiceTest,SeatHoldServiceTest,BookingControllerTest" test
```

Test MVC render template thật, kiểm tra session, định dạng yêu cầu và HTTP 400/404/409.
Unit test kiểm tra tính giá, validation và chuyển lỗi tranh chấp; không dùng unit test để khẳng định
rollback hoặc chống tranh chấp thật.

Các test SQL Server kế thừa `IntegrationTestBase`, tạo dữ liệu bằng `TestDataFactory`, kiểm tra
tranh chấp, rollback toàn bộ, khôi phục lượt giữ sau khi tải lại và xoá vé hết hạn.
Chỉ chạy trên database test theo `docs/DATABASE.md` vì lớp cha xóa dữ liệu trước mỗi test.

Không chạy integration test trên database cloud dùng chung.

```powershell
mvn test
```

Trước PR vào `develop`: toàn bộ test phải xanh, kiểm tra giao diện sáng/tối và màn hình nhỏ,
đính kèm ảnh, ghi mã công việc, mô tả phần chưa hoàn thành và nhờ một thành viên khác duyệt.

## Kết quả kiểm tra tại máy ngày 29/09/2026

- Biên dịch thành công bằng Maven với JDK 26 và đích biên dịch Java 21.
- 55 unit/MVC test của Module 2 chạy xanh; integration test SQL Server biên dịch thành công.
- Profile `cloud` kết nối thành công và Hibernate xác thực schema hợp lệ.
- JavaScript của trang chọn ghế đã qua kiểm tra cú pháp.
- Chưa chạy integration test trên database `_test` cục bộ và chưa kiểm tra trực quan đầy đủ ở
  chế độ sáng/tối, màn hình nhỏ; cần hoàn thành hai mục này trước Pull Request nếu môi trường cho phép.

JDK đã dùng kiểm tra là bản cài trên máy, không nằm trong project.
Để dùng tạm trong PowerShell tại thư mục gốc dự án:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26.0.2.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -v
```

Không thay đổi Java mặc định của máy; project vẫn biên dịch theo Java 21 như cấu hình trong `pom.xml`.
