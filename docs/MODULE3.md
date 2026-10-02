# Module 3 — Tuấn Thanh: tài khoản và OTP

Code dùng kiến trúc session của `main`, `Constants.SESSION_USER`, BCrypt và design system chung. Không thêm Spring Security filter hoặc thay đổi schema database.

## Các luồng đã tích hợp

- `/dang-ky`: kiểm tra họ tên, email, điện thoại, mật khẩu và xác nhận mật khẩu → gửi OTP → `/dang-ky/xac-thuc` → tạo tài khoản khách hàng → đăng nhập. Trước khi xác minh không có tài khoản trong database; mật khẩu chờ đăng ký đã được BCrypt hash.
- `/quen-mat-khau` hoặc `/khoi-phuc-tai-khoan`: nhập email → OTP → token đặt mật khẩu mới → `/dat-lai-mat-khau` → đăng nhập lại. Email tồn tại hay không đều có cùng phản hồi. Token gắn với hash mật khẩu hiện tại, đổi mật khẩu làm token cũ mất hiệu lực. Khoá tài khoản khi cập nhật để chống dùng token đồng thời.
- `/thanh-toan/{showtimeId}`: chọn MoMo QR hoặc thẻ qua MoMo → gửi OTP → xác minh → tiếp tục phương thức đã chọn. Vé online không hỗ trợ trả tiền mặt tại quầy. Không chuyển vé sang `PAID` chỉ vì OTP đúng; vẫn phải chờ kết quả xác thực từ MoMo. Gọi thẳng endpoint thanh toán mà không có OTP bị chặn.

OTP gồm 6 chữ số, sinh bằng `SecureRandom`, lưu hash kèm salt. Mã hết hạn sau 5 phút, dùng một lần, tối đa 5 lần nhập sai. Gửi lại sau 60 giây, tối đa 5 lần gửi mỗi email/mục đích trong 15 phút. Mã mới vô hiệu mã cũ. OTP thanh toán gắn với tài khoản, suất chiếu, phương thức, danh sách vé, giá tiền và thời điểm giữ ghế. OTP không gia hạn 5 phút giữ ghế.

Trang thanh toán luôn hiển thị hai lựa chọn QR MoMo và thẻ ATM/thẻ quốc tế qua MoMo. Khi thiếu `momo.partner-code`, `momo.access-key` hoặc `momo.secret-key`, các nút bị vô hiệu và hiện thông báo chưa sẵn sàng. Đặt bộ khoá MoMo của dự án trong `application-secrets-cloud.properties` nếu chạy cloud, hoặc `application-secrets.properties` nếu chạy local, rồi khởi động lại. Không xác nhận vé bằng OTP đơn thuần và không dùng tiền mặt làm phương thức thay thế.

Form được kiểm tra cả trên trình duyệt và server. Server kiểm tra lại email tối đa 150 ký tự, họ tên tối đa 150 ký tự, điện thoại tùy chọn bắt đầu bằng 0 và có 10–11 chữ số, mật khẩu ít nhất 6 ký tự, không chỉ là khoảng trắng và tối đa **72 byte UTF-8** theo BCrypt. Mật khẩu không được in lại vào HTML khi form lỗi. Session ID được đổi sau đăng nhập; đường dẫn quay lại chỉ nhận nội bộ.

## Setup email thật

Thiết lập tự động một lần trong PowerShell tại thư mục dự án:

```powershell
python scripts/setup-mail.py
```

Nhập Gmail gửi thư và Gmail App Password (mật khẩu nhập ẩn). Trình thiết lập kiểm tra STARTTLS và đăng nhập SMTP, không gửi thư thử. Chỉ khi đăng nhập thành công mới lưu `application-secrets-mail.properties`, tự được nạp cho cả local và cloud và bật `app.mail.enabled=true`. Không sửa cấu hình database, không cần cấu hình từng email người nhận. File mail secret và file tạm đã được bỏ qua trong Git. Khởi động lại ứng dụng sau khi thiết lập.

Kiểm tra lại kết nối bằng cấu hình đã lưu: `python scripts/setup-mail.py --check`.

Nếu muốn cấu hình thủ công, dùng cách bên dưới; cấu hình trong `application-secrets-mail.properties` được ưu tiên hơn các file secret database.

Điền vào `application-secrets.properties` trên máy, giữ nguyên các dòng database hiện có:

```properties
spring.mail.username=dia_chi_gmail_cua_ban@gmail.com
spring.mail.password=app_password_cua_gmail
app.password-reset.secret=chuoi_ngau_nhien_dai_va_bi_mat
```

Dùng Gmail App Password của tài khoản đã bật xác minh hai bước. Không dùng mật khẩu đăng nhập Gmail. Không commit file secret hoặc gửi mật khẩu vào tài liệu. Khi chạy profile cloud, đặt các dòng email trong `application-secrets-cloud.properties` nếu cấu hình local không được nạp.

SMTP mặc định là Gmail cổng 587 với STARTTLS, có timeout kết nối/đọc/ghi 5 giây. Nếu SMTP chưa cấu hình hoặc gửi lỗi, đăng ký và thanh toán báo không gửi được mã và không tiếp tục. Khôi phục vẫn trả thông báo chung để không tiết lộ tài khoản. Không có mã OTP mặc định, mã bypass hoặc log mã thật.

Các tài khoản mẫu `@utecinema.local` vẫn đăng nhập được nhưng **không nhận email**, nên không thể hoàn tất thanh toán OTP. Đăng ký bằng email thật để thử trọn vẹn các luồng. Test mock dịch vụ mail và MoMo, không gửi email hoặc trừ tiền thật.

## Chạy và kiểm thử

Máy đã có JDK 21 trong `.tools`. Nếu Maven mặc định dùng Java 17, chạy:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/mvn21.ps1 test
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/mvn21.ps1 spring-boot:run
```

Database kiểm thử phải là `cinema_booking_test`; các integration test dùng `IntegrationTestBase` kiểm tra tên database trước khi dọn dữ liệu. Bộ kiểm thử bao gồm đăng ký/khôi phục qua HTTP, mã sai/hết hạn/dùng lại, giới hạn gửi lại, xác thực đồng thời, sai mục đích, ràng buộc đơn vé, chặn bỏ qua OTP và luồng thanh toán tại quầy/MoMo.

Kiểm tra thủ công ở cả giao diện sáng/tối và màn hình mobile: đăng nhập sai, nút hiện/ẩn mật khẩu, OTP dán từ email, đồng hồ hết hạn, gửi lại mã, đổi email, hết thời gian giữ ghế. Trang đăng nhập dùng màu/nền hero của trang chủ và CSS chung.

OTP lưu trong bộ nhớ của **một instance** ứng dụng. Khởi động lại làm các phiên OTP mất hiệu lực; khi triển khai nhiều instance cần chuyển kho challenge và rate limit sang Redis/database chung. Các thay đổi cục bộ trước khi pull được giữ trong Git stash và `.local-backup/before-main/` để tham khảo, không đưa vào Git.
