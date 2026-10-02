# Validation ghế, giá vé và suất chiếu

Nhánh Hữu_Thắng; sửa trực tiếp trên máy, không tự commit/push.

- Giá cơ bản là số đồng nguyên từ 1 đến 49.999.999. Giá `75000.00` vẫn được nhận như `75000`. Form, service và tính giá ghế cùng kiểm tra; từ chối giá âm, 0, phần lẻ hoặc quá lớn trước khi ghi database.
- Giới hạn giá dựa trên `DECIMAL(10,2)` hiện có: ghế COUPLE nhân 2 vẫn nằm trong phạm vi cột tiền. Không sửa schema hoặc dữ liệu cloud.
- Làm tròn HALF_UP cho từng ghế tới đồng nguyên rồi cộng hóa đơn. Ví dụ giá cơ bản 75.001, VIP = 112.502; hai ghế VIP = 225.004. QR, vé giữ và hóa đơn dùng cùng số tiền. Không tính lại giá các vé đã giữ/đã thanh toán trước đó.
- Mã phim/phòng phải dương. Giờ chiếu dùng `BookingClock` theo giờ Việt Nam; kiểm tra thời lượng, phạm vi ngày giờ SQL Server, lịch trùng và kiểm tra lại giờ bắt đầu trước lúc ghi.
- Sửa/xóa suất dùng khóa suất chiếu của giữ ghế/hủy/thanh toán trước khi kiểm tra vé. Khi quản trị đổi phòng trước, request giữ ghế theo phòng cũ bị từ chối; khi đã có vé HELD/PAID, sửa/xóa bị từ chối và dữ liệu cũ được giữ.

Chạy `CinemaBookingApplication` bằng JDK 21 trong IDE là mặc định chọn cloud, web cổng 8082 và ví Render. Secret cloud vẫn nằm trong file bị Git bỏ qua. Ứng dụng đánh thức ví ở nền, không cần Cloudflare hoặc ví local. Chi tiết trong [DEMO_WALLET.md](DEMO_WALLET.md).

Kiểm thử bổ sung: tạo phim/suất qua HTTP quản trị rồi giữ VIP và xác nhận ví cho P/K/T13/T16/T18; giá hợp lệ/không hợp lệ trên form và service; đồng thời sửa/xóa và giữ ghế trên SQL Server riêng; bảo toàn vé HELD/PAID; warmup không làm lỗi web khi ví mất kết nối.

Tất cả test có dọn dữ liệu dùng database riêng `cinema_booking_module2_review_test`, không dùng database cloud nhóm.

Kiểm chứng ngày 02/10/2026: toàn bộ 418 kiểm thử Java và 21 kiểm thử JavaScript đạt, đóng gói thành công. Chạy jar không chỉ định profile trên cổng kiểm tra 18082: cloud validate thành công, trang chủ HTTP 200, `/demo-wallet` HTTP 302 về đúng Render, warmup health báo ví sẵn sàng. Lần smoke tắt cleanup/email và không tạo đơn trên cloud. App 8082 của người dùng được giữ nguyên; app smoke và cầu nối test được dừng sau kiểm tra.
