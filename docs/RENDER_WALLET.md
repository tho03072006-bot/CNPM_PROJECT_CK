# Ví MoMo giả lập Nhóm 8 trên Render Free

© 2026 Nhóm 8. Đây là ví mô phỏng của đồ án, không thuộc hệ thống MoMo chính thức và không phát sinh tiền thật.

## Mô hình

Web rạp chạy local; chỉ ví điện thoại và backend xác nhận thanh toán chạy trên Render. Hai ứng dụng đọc/ghi cùng SQL Server cloud nhóm. Render không chuyển tiếp về localhost của laptop và không cần Cloudflare.

- Laptop tạo giao dịch PENDING và QR chứa URL HTTPS của **đúng giao dịch**, gồm UUID và khóa trong fragment.
- Điện thoại quét QR bằng camera/trình duyệt, đọc thông tin, đồng ý mô phỏng rồi xác nhận.
- Backend online khóa suất chiếu và giao dịch trên database, kiểm tra khóa, CSRF, số tiền, lượt giữ, phim và hạn giữ.
- Vé PAID, hóa đơn PAID và giao dịch SUCCESS được cập nhật trong cùng transaction.
- Web local kiểm tra trạng thái mỗi 3 giây và tự mở vé. Nếu laptop tạm tắt sau khi tạo đơn, ví vẫn xác nhận được khi lượt giữ còn hạn; laptop đọc lại kết quả khi mở lại. Tạo đơn mới cần web local hoạt động.

QR được ưu tiên trên trang, kích thước tối đa 420px, đen trên trắng ở cả giao diện sáng/tối. URL nằm trong mục dự phòng. QR kết thúc sẽ được ẩn sau khi backend xác nhận trạng thái.

## 1. Tạo tài khoản miễn phí

1. Mở https://dashboard.render.com/register, chọn đăng nhập bằng GitHub của bạn.
2. Chọn workspace Hobby miễn phí. Không chọn tài nguyên trả phí.
3. Khi Render yêu cầu quyền truy cập repository, chọn repository CNPM_PROJECT_CK mà tài khoản bạn có quyền truy cập. Với repository của tổ chức/nhóm, chủ repository có thể cần cấp quyền cho ứng dụng Render.

Nếu repository chưa hiện, vào Account Settings → Account Security → Git Deployment Credentials → Add credential → GitHub, rồi cấp quyền cho đúng repository. Chủ repository nhóm có thể cần cho phép ứng dụng Render.

Không đưa mật khẩu GitHub vào code hoặc gửi qua chat. Các bước đăng nhập/cấp quyền tài khoản thực hiện trực tiếp trên Render/GitHub.

## 2. Đưa code và triển khai Blueprint

Các thay đổi ở máy local chưa tự xuất hiện trên GitHub. Nhánh hiện tại là **Hữu_Thắng**. Xem thay đổi rồi chạy:

~~~powershell
cd E:\Cong_Nghe_Phan_Mem\Project_Cuoi_Ky
git status
git add .
git commit -m "Bổ sung ví MoMo giả lập Nhóm 8 chạy online, QR HTTPS và xác nhận thanh toán qua database chung"
git push -u origin "Hữu_Thắng"
~~~

File secret và target đã được Git bỏ qua. Sau khi push xong:

1. Trong Render Dashboard chọn **New → Blueprint**.
2. Chọn repository **tho03072006-bot/CNPM_PROJECT_CK**, nhánh **Hữu_Thắng**, đường dẫn Blueprint **render.yaml**.
3. Kiểm tra chỉ có **một Web Service**, runtime Docker, gói **Free**, vùng Singapore. Không thêm database Render: dùng database cloud nhóm hiện có.
4. Nhập ba biến bí mật khi được hỏi:

| Biến trên Render | Lấy giá trị từ đâu |
|---|---|
| WALLET_DB_URL | Chuỗi JDBC SQL Server bên dưới, điền server, port và tên database cloud nhóm |
| WALLET_DB_USERNAME | cloud.db.username trong application-secrets-cloud.properties |
| WALLET_DB_PASSWORD | cloud.db.password trong application-secrets-cloud.properties |

Ví dụ **cấu trúc**, thay SERVER, PORT, DBNAME bằng thông tin nhóm:

~~~text
jdbc:sqlserver://SERVER:PORT;databaseName=DBNAME;encrypt=true;trustServerCertificate=true;loginTimeout=30
~~~

PORT mặc định 1433. Nếu nhóm dùng cloud.db.options khác, giữ đúng các tham số đó trong chuỗi JDBC. Không dán nguyên tên khóa cloud.db.* vào giá trị Render. Mật khẩu chỉ nhập ở mục Environment của Render; không ghi vào render.yaml/Dockerfile/Git.

5. Chọn triển khai và đợi service **Live**. Render tự cấp URL HTTPS riêng. Tên chính xác lấy từ Dashboard, không đoán trước.
6. Mở **URL-HTTPS-THỰC-TẾ/demo-wallet**. Trang phải có nhãn giả lập và bản quyền Nhóm 8.
7. Mở **URL-HTTPS-THỰC-TẾ/demo-wallet/health**: kết quả bình thường là {"status":"UP"}. HTTP 503 có nghĩa ví/database chưa sẵn sàng.

Render tự cung cấp RENDER_EXTERNAL_URL và PORT; không cần tự điền hai biến này. Docker chỉ mở cổng gateway công khai theo PORT; Spring ở 127.0.0.1:8080 trong container. Web rạp, đăng nhập, tài khoản, quản trị và lịch sử vé không đi qua gateway. Health chỉ trả trạng thái chung, không trả mật khẩu hay dữ liệu đơn.

Nếu Docker build/SQL kết nối thất bại, xem Logs. Không đổi ddl-auto sang update/create để thử sửa cloud: profile online chỉ validate schema. Migration 20261001-demo-wallet.sql đã được áp dụng lên cloud nhóm; lần triển khai này không thêm bảng hoặc xóa dữ liệu.

## 3. Nối web local với ví online

Lấy URL thật từ Render, sau đó chạy trong thư mục dự án:

~~~powershell
.\scripts\set-demo-wallet-url.ps1 -PublicUrl "https://URL-THUC-TE.onrender.com/demo-wallet"
~~~

URL trong ví dụ là chỗ điền, không phải một ví đã được triển khai. Script chấp nhận origin HTTPS hoặc đường dẫn /demo-wallet, lưu origin vào file secret cloud đang được Git bỏ qua, giữ nguyên cấu hình database, từ chối localhost/HTTP/query/khóa giao dịch.

Khởi động lại web rạp bằng IDE với profile cloud. Hoặc dùng JDK 21:

~~~powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run "-Dspring-boot.run.profiles=cloud"
~~~

Web mặc định http://localhost:8082. Nếu cổng bận, cấu hình cổng khác trong IDE. URL public ví không phụ thuộc cổng web local.

Có thể dùng scripts/start-demo-wallet.ps1 để bật web local với URL Render đã cấu hình. Script hiện không mở Cloudflare hoặc ví local. Dừng web do script tạo bằng scripts/stop-demo-wallet.ps1.

## 4. Kiểm tra hai thiết bị

1. Mở trang ví online trên điện thoại trước khi chọn ghế để đánh thức service Free nếu đang ngủ.
2. Mở web local, đăng nhập, chọn ghế/bắp nước và thanh toán bằng MoMo — Giả lập Nhóm 8.
3. QR phải chứa https://URL-VI-THUC-TE/demo-wallet/pay/{UUID}#token={khoa}. Không được chứa localhost hoặc địa chỉ trycloudflare cũ.
4. Quét QR bằng camera điện thoại; ví phải hiện đúng phim, ghế, suất chiếu và tổng tiền. Không dùng app MoMo thật để quét.
5. Nếu không quét được, mở mục **Không quét được QR? Dùng liên kết thanh toán** trên web local. Sao chép toàn bộ URL, bao gồm #token, rồi gửi sang điện thoại và mở bằng trình duyệt.
6. Trước khi đánh dấu xác nhận mô phỏng, nút thanh toán phải bị khóa. Sau khi đồng ý, bấm xác nhận một lần.
7. Điện thoại hiện thành công; laptop tự mở vé. Hai thiết bị xác nhận cùng lúc chỉ tạo một lần thanh toán.
8. Thử QR hết hạn/hủy, sai khóa, đổi số tiền hoặc đổi ghế: backend phải từ chối; không gia hạn giữ ghế để bù thời gian service khởi động.

Quét bằng camera mở đúng URL giao dịch, không cần camera tích hợp trong trang ví. Điện thoại có thể dùng 4G/5G hoặc mạng Wi-Fi khác. Không có khóa QR thì không xác nhận được giao dịch; không công khai khóa này trong ảnh/chia sẻ ngoài người thanh toán.

## Quản lý miễn phí

Trong Dashboard của service:
- **Logs**: xem khởi động/kết nối SQL/lỗi.
- **Environment**: sửa ba biến database, lưu và deploy lại.
- **Manual Deploy → Deploy latest commit**: cập nhật sau khi bạn push code. Blueprint tắt auto deploy để bạn chủ động cập nhật.
- **Settings**: kiểm tra gói Free, URL và health check /demo-wallet/health.
- Dừng/suspend hoặc xóa service theo chức năng Dashboard khi không dùng.

Free ngủ sau 15 phút không có truy cập, lần khởi động tiếp theo có thể khoảng một phút. Quota miễn phí cũng có giới hạn; không có cam kết 24/7. Không thêm thẻ thanh toán nếu bạn muốn tránh tự thanh toán phí vượt quota; không đổi gói sang loại trả phí. Nếu database nhóm giới hạn IP từ xa, cần cho phép IP outbound của service Render ở nhà cung cấp SQL rồi thử lại.

## Tài liệu chính thức đã đối chiếu

- https://render.com/docs/free
- https://render.com/docs/web-services
- https://render.com/docs/docker
- https://render.com/docs/blueprint-spec
- https://render.com/docs/environment-variables
- https://render.com/docs/health-checks

**Ví nhóm đã được triển khai: https://momo-gia-lap-nhom8.onrender.com/demo-wallet; ngày 02/10/2026 health trả UP.** Kiểm thử local không thay thế việc kiểm tra kết nối từ Render đến SQL cloud và quét thực tế trên điện thoại sau triển khai.

## Kết quả kiểm thử cấu hình mới trên máy local

- Java toàn dự án: 368 bài, không lỗi; JavaScript: 21 bài, không lỗi.
- Blueprint được đọc bằng trình phân tích YAML: đúng một dịch vụ Docker Free, triển khai thủ công, không thêm database. Hai Docker image chính thức đã được kiểm tra tồn tại.
- Script cấu hình được thử với file secret giả riêng: giữ thông tin database, chỉ một URL mới, từ chối HTTP/localhost/user-info/query/link giao dịch.
- Hai JVM riêng: web local tạo QR, ví profile wallet-online xác nhận, web local tự mở vé từ trạng thái chung. Dừng JVM web local rồi xác nhận một QR khác vẫn thành công; đúng hai vé thử chuyển PAID. Chỉ dùng cinema_booking_module2_test.
- Chrome hai context độc lập: QR desktop 420px, QR/giao diện phù hợp 375px, URL dự phòng thu gọn, không có lỗi JavaScript; cổng công khai chặn trang web rạp/tài khoản/quản trị.
- URL HTTPS trong phép thử trình duyệt được mô phỏng bằng chuyển tiếp nội bộ. Chưa build container tại máy này vì chưa có Docker, chưa deploy Render hoặc kiểm tra quét bằng điện thoại thật. Sau deploy cần kiểm tra kết nối SQL từ Render và QR HTTPS thực tế.

## Nhập link trang ví vào cấu hình

Code chấp nhận cả https://TEN-VI.onrender.com và https://TEN-VI.onrender.com/demo-wallet (có thể có dấu / cuối). Khi tạo QR, backend tự chuẩn hóa về địa chỉ gốc rồi ghép /demo-wallet/pay/{UUID} đúng một lần. Link giao dịch có #token, query, tài khoản/mật khẩu trong URL hoặc đường dẫn khác vẫn bị từ chối. Sau khi sửa file secret, cần khởi động lại backend local vì cấu hình được đọc khi ứng dụng khởi động.

## Bỏ ví local

Profile cloud/local tắt demo-wallet.phone-enabled; các API ví local không tồn tại. Hai đường dẫn GET ví local chỉ chuyển sang HTTPS. Profile wallet-online bật giao diện/API ví trên Render. QR luôn dùng demo-wallet.public-base-url của Render và đi thẳng đến /demo-wallet/pay/{UUID} cùng khóa fragment. Script local không tạo tunnel hoặc cổng ví local nữa.
