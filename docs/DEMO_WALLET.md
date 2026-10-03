# MoMo giả lập Nhóm 8 — ví online, web rạp local

© 2026 Nhóm 8. Đây là ứng dụng mô phỏng của đồ án, không thuộc hệ thống MoMo chính thức và không phát sinh tiền thật.

## Sử dụng

**Chạy trực tiếp trong IDE:** chọn JDK 21, mở đúng thư mục dự án và Run `CinemaBookingApplication` dưới dạng Spring Boot App. Không cần nhập profile hay chạy script: profile mặc định là `cloud`, web dùng cổng 8082, tự đọc `application-secrets-cloud.properties`, bật giữ ghế/thanh toán và dùng URL Render của nhóm. Khi Spring đã sẵn sàng, ứng dụng gọi health ví ở nền một lần để đánh thức Render. Nếu ví hoặc mạng chưa sẵn sàng, web vẫn chạy; mở ví trước khi giữ ghế. Không thể bảo đảm Render Free hoặc database ngoài luôn hoạt động ngay lập tức.

File secret cần có sẵn như hiện tại, không được commit. Hibernate cloud chỉ `validate`, không tạo/xóa/sửa schema. Profile chỉ định rõ (`test`, `wallet-online` hoặc `local`) vẫn được ưu tiên. Profile online không tự gọi warmup của chính nó. Muốn tắt warmup trên máy local: `demo-wallet.warmup-enabled=false`.

Ví hiện đã được triển khai tại https://momo-gia-lap-nhom8.onrender.com/demo-wallet. Endpoint /demo-wallet/health được kiểm tra trả HTTP 200, status UP ngày 02/10/2026.

1. Cấu hình application-secrets-cloud.properties với database nhóm và demo-wallet.public-base-url=https://momo-gia-lap-nhom8.onrender.com.
2. Chạy web local bằng IDE với profile cloud hoặc:

~~~powershell
.\scripts\start-demo-wallet.ps1 -JavaHome "C:\Program Files\Java\jdk-21.0.12.1"
~~~

3. Trên web local, đăng nhập, chọn ghế/bắp nước, chọn MoMo — Giả lập Nhóm 8.
4. Quét QR lớn bằng camera điện thoại. QR mở thẳng https://momo-gia-lap-nhom8.onrender.com/demo-wallet/pay/{UUID}#token={khoa}, đúng giao dịch và số tiền.
5. Kiểm tra thông tin, đánh dấu xác nhận giả lập rồi bấm xác nhận. Web local tự đọc kết quả và mở vé.
6. URL dự phòng nằm trong mục **Không quét được QR? Dùng liên kết thanh toán**. Sao chép đầy đủ URL, bao gồm #token.
7. Dừng web local do script quản lý:

~~~powershell
.\scripts\stop-demo-wallet.ps1
~~~

Ví trên Render hoạt động độc lập với laptop. Tạo đơn mới cần web local; đơn đã được tạo có thể xác nhận trên ví online khi lượt giữ còn hạn. Render Free có thể ngủ khi không truy cập; nên mở ví trước khi chọn ghế.

## Tách local và online

- Profile cloud/local: demo-wallet.phone-enabled=false, không có giao diện ví, API xác nhận/hủy/trạng thái ví hoặc health của ví trên local.
- GET /demo-wallet và /demo-wallet/pay/{UUID} trên local chuyển hướng sang ví HTTPS cấu hình sẵn.
- Web local giữ các endpoint /thanh-toan để tạo QR, đọc trạng thái theo đúng chủ đơn và mở vé đã trả.
- Profile wallet-online trên Render: demo-wallet.phone-enabled=true, có giao diện ví và API thanh toán.
- Gateway trên Render chỉ mở ví/health/CSS/JS/favicon, chặn các trang rạp/tài khoản/quản trị.
- Không còn mở cổng ví local 8084 hoặc Cloudflare khi chạy script start-demo-wallet.ps1.
- Code chấp nhận origin HTTPS hoặc link có /demo-wallet và tự chuẩn hóa trước khi ghép đường dẫn thanh toán.

## Validation và database

Hai backend dùng chung SQL Server cloud. Điện thoại chỉ gọi API HTTPS, không kết nối database trực tiếp.

| Trạng thái | Ý nghĩa |
|---|---|
| PENDING | Đúng lượt giữ, còn hạn, chờ xác nhận |
| SUCCESS | Vé, hóa đơn và giao dịch đã cập nhật trong cùng transaction |
| CANCELLED | Hủy mã; giữ ghế vẫn theo hạn ban đầu |
| EXPIRED | Đến hạn giữ hoặc giờ chiếu |
| INVALIDATED | Ghế, đơn, phim thay đổi, QR bị thay thế hoặc đã thanh toán bằng cách khác |

- QR gắn với chủ đơn, suất, toàn bộ ID vé và tổng tiền vé + bắp nước.
- Backend kiểm tra khóa QR, CSRF, xác nhận mô phỏng, tiền nguyên dương khớp database, trạng thái phim và hạn giữ.
- Khóa QR ngẫu nhiên 256 bit, chỉ lưu SHA-256 trong database. Fragment không gửi trong URL HTTP/access log; trình duyệt giữ khóa trong sessionStorage của tab.
- Xác nhận trùng/cùng lúc chỉ tạo một hóa đơn. Không gia hạn giữ ghế khi tạo lại QR hoặc service khởi động chậm.
- Mất phản hồi xác nhận: đọc lại trạng thái, không tự gửi thêm một lần thanh toán.
- Bảng demo_payments và enum MOMO_DEMO dùng migration database/migrations/20261001-demo-wallet.sql đã được áp dụng lên cloud nhóm trước đó. Lần sửa URL/tách local này không sửa schema cloud.
- Các thành viên nhóm cần dùng code có enum MOMO_DEMO trước khi đọc vé giả lập trên database chung.
- Kiểm thử có xóa dữ liệu chỉ được chạy trên database riêng _test; không dùng cloud nhóm.

## Triển khai và quản lý

Xem [hướng dẫn Render](RENDER_WALLET.md), Dockerfile.wallet và render.yaml. Sau khi push code mới, chọn Manual Deploy → Deploy latest commit trên Render. File secret và target được Git bỏ qua.

Script start-demo-wallet.ps1 bật web local, giữ URL Render đã cấu hình và ghi PID/giờ tạo vào target/demo-wallet/runtime.json. Script stop chỉ dừng đúng tiến trình nó tạo; trạng thái Cloudflare cũ được hỗ trợ để dọn tiến trình cũ nếu còn.

Kiểm thử JavaScript:

~~~powershell
node --test src/test/js/seat-booking.test.cjs src/test/js/demo-wallet.test.cjs
~~~

## Kiểm chứng bản sửa ngày 02/10/2026

379 kiểm thử Java và 21 kiểm thử JavaScript đạt. Web 8082 đã khởi động lại: GET / trả 200, GET /demo-wallet trả 302 đúng URL Render, GET /demo-wallet/health và POST API confirm ví trên local trả 404. QR được kiểm thử chứa URL HTTPS giao dịch đúng một lần, kể cả cấu hình nhập đuôi /demo-wallet. Ví Render health UP. Chỉ dữ liệu _test được sử dụng để kiểm thử giao dịch.
