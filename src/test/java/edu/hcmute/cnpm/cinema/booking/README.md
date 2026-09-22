# Module 2 — Ghế và vé (Thắng)

## Phạm vi bản cơ bản

- `SeatService.findSeatMap(showtimeId)`: sơ đồ ghế, trạng thái và giá theo loại ghế.
- `SeatPricingService.calculateSeatPrice(basePrice, seatType)`: NORMAL giá gốc, VIP ×1,5, COUPLE ×2.
- `SeatBookingService.holdSeats(showtimeId, request, currentUser)`: giữ nhiều ghế trong một giao dịch.
- `GET /booking/showtime/{showtimeId}`: trang chọn ghế dùng layout và CSS chung.
- `POST /booking/showtime/{showtimeId}/hold`: chỉ nhận JSON `{"seatIds":[1,2]}`.
- Thành công trả `{success, message, ticketIds, totalPrice, expiresAt}`. Lỗi nghiệp vụ dùng handler chung;
  AJAX cần header `X-Requested-With: XMLHttpRequest` hoặc `Accept: application/json`.
- JSON sai định dạng được Spring trả 400; giao diện có thông báo dự phòng khi phản hồi không có `message`.

Người dùng lấy từ session `Constants.SESSION_USER` do Module 3 cung cấp. Không tạo đăng nhập giả,
không nhận `userId` hoặc giá từ trình duyệt. Chưa tích hợp đăng nhập thì vẫn xem/chọn ghế được,
nhưng không gửi giữ ghế. Không thêm liên kết vào trang phim hay header của thành viên khác.

Validation tại service: tài khoản còn tồn tại, mã dương, danh sách ghế không rỗng/không trùng,
ghế tồn tại và đúng phòng, suất chưa bắt đầu, phim đang hoạt động, cấu hình phòng hợp lệ,
giá gốc dương và loại ghế được hỗ trợ. Chưa tự đặt giới hạn số ghế mỗi lượt khi nhóm chưa chốt số lượng.

## Phần cần thống nhất trước khi mở rộng

M2.5 (đếm ngược), M2.6 (hết hạn) và M2.7 (hủy giữ) chưa triển khai trong bản này.
`expiresAt` chỉ cung cấp mốc 5 phút; chưa có tác vụ tự giải phóng ghế.

Schema hiện có `UNIQUE(showtime_id, seat_id)` áp dụng cho **mọi trạng thái**. Chuyển vé sang
`EXPIRED`/`CANCELLED` vẫn không cho tạo vé mới trên cùng ghế/suất. Tái sử dụng hoặc xóa bản ghi cũ
lại ảnh hưởng lịch sử vé của Module 3. Cần Thọ và Thanh thống nhất cách giữ lịch sử và giải phóng
ràng buộc trước khi thực hiện; bản này không sửa entity/schema/repository dùng chung.

Vì thế vé hủy, hết hạn hoặc HELD đã quá 5 phút đang hiển thị `UNAVAILABLE` (chưa mở lại), không
cho chọn nhầm thành ghế trống. Đây là giới hạn bản cơ bản, chưa đáp ứng đầy đủ tiêu chí M2.6/M2.7.

## Kiểm thử

Đặt `JAVA_HOME` trỏ JDK 21 trước khi chạy Maven. Không đổi phiên bản trong `pom.xml`.

Chạy kiểm thử không dùng database:

```powershell
mvn "-Dtest=SeatPricingServiceTest,SeatServiceTest,SeatBookingServiceTest,BookingControllerTest" test
```

Test MVC render template thật, kiểm tra session, định dạng yêu cầu và HTTP 400/404/409.
Unit test kiểm tra tính giá, validation và chuyển lỗi tranh chấp; không dùng unit test để khẳng định
rollback hoặc chống tranh chấp thật.

Test SQL Server: `SeatBookingServiceIntegrationTest` kế thừa `IntegrationTestBase`, tạo dữ liệu bằng
`TestDataFactory`, kiểm tra hai request đồng thời, rollback toàn bộ khi một ghế trùng và giữ nhiều ghế.
Chỉ chạy trên database test theo `docs/DATABASE.md` vì lớp cha xóa dữ liệu trước mỗi test.

```powershell
mvn test
```

Trước PR vào `develop`: toàn bộ test phải xanh, kiểm tra giao diện sáng/tối và màn hình nhỏ,
đính kèm ảnh, ghi mã công việc, mô tả phần chưa hoàn thành và nhờ một thành viên khác duyệt.

## Kết quả kiểm tra tại máy ngày 19/09/2026

- Biên dịch thành công bằng Maven 3.9.16 và Temurin 21.0.12.1.
- 41 unit/MVC test chạy xanh; các test SQL Server đã biên dịch thành công.
- Bộ test đầy đủ chưa xanh: SQL Server tại `localhost:1433` từ chối kết nối; dịch vụ
  `MSSQLSERVER` đang dừng và tiến trình hiện tại không có quyền khởi động dịch vụ.
- Máy chưa có `application-secrets.properties`. Cần cấu hình kết nối theo README chung,
  tạo database test rồi chạy lại `mvn test`. Chưa xác nhận chống tranh chấp/rollback trên database thật.
- Chưa kiểm tra trực quan bằng trình duyệt ở chế độ sáng/tối và màn hình nhỏ.

JDK dùng kiểm tra nằm trong `target/module2-tooling/jdk-21.0.12.1+1` (được Git bỏ qua).
Để dùng tạm trong PowerShell tại thư mục gốc dự án:

```powershell
$env:JAVA_HOME = (Resolve-Path 'target/module2-tooling/jdk-21.0.12.1+1').Path
mvn -v
```

Không thay đổi Java mặc định của máy; nếu chạy `mvn clean` thì cần dùng JDK 21 cài riêng ngoài `target`.
