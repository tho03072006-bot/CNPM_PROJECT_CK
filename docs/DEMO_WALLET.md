# MoMo giả lập Nhóm 8 — ví web điện thoại

© 2026 Nhóm 8. Bản quyền phần mềm và giao diện giả lập thuộc Nhóm 8. Tên MoMo dùng để mô tả ví mô phỏng; thương hiệu MoMo thuộc chủ sở hữu của thương hiệu. Đây không phải ứng dụng MoMo chính thức và không phát sinh tiền thật.

## Chạy ví online độc lập trên Render Free

Web rạp vẫn local; ví và backend thanh toán chạy riêng trên Render, cùng database nhóm. Xem [hướng dẫn Render](RENDER_WALLET.md), Dockerfile.wallet và render.yaml. Đây là cấu hình khuyến nghị cho ví online độc lập với laptop. Chưa có URL Render thật trước khi triển khai thành công.

## Phương án Cloudflare tạm thời trên hai thiết bị

1. Cấu hình file application-secrets-cloud.properties theo file example. File chứa mật khẩu nằm ngoài Git.
2. Database nhóm cần migration database/migrations/20261001-demo-wallet.sql. Migration này đã được áp dụng lên cloud nhóm ngày 01/10/2026; có thể chạy lại an toàn.
3. Từ thư mục dự án, mở PowerShell và chạy:

~~~powershell
.\scripts\start-demo-wallet.ps1 -JavaHome "C:\Program Files\Java\jdk-21.0.12.1"
~~~

Script build ứng dụng, tải công cụ Cloudflare chính thức vào target/tools nếu chưa có, mở cổng ví riêng và in URL HTTPS. Dùng -SkipBuild khi file JAR đã được build từ code hiện tại. Có thể chọn -Port và -WalletPort nếu cổng mặc định đang bận.

4. Trên laptop, vào http://localhost:8082, đăng nhập, chọn ghế/bắp nước, đến thanh toán và chọn **MoMo — Giả lập Nhóm 8**.
5. Trên điện thoại, dùng camera quét QR rồi mở liên kết. Có thể dán đầy đủ liên kết thanh toán vào trang /demo-wallet của URL HTTPS được in ra.
6. Kiểm tra phim, phòng, ghế, suất chiếu và tổng tiền; đánh dấu xác nhận giả lập rồi bấm thanh toán. Laptop tự cập nhật và mở vé sau khi backend xác nhận.
7. Tắt bản demo:

~~~powershell
.\scripts\stop-demo-wallet.ps1
~~~

Điện thoại có thể dùng Wi-Fi khác hoặc 4G/5G. Máy chạy backend cần bật và có Internet. URL HTTPS thay đổi khi mở tunnel mới, hết hiệu lực khi tunnel dừng. Khi bật lại, hãy tạo QR mới. QR phải mở bằng camera/trình duyệt; không phải mã thanh toán của app MoMo thật.

HTTPS dùng [Cloudflare Quick Tunnel](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/), không cần tài khoản Cloudflare hay tên miền. [Tài liệu Cloudflare](https://developers.cloudflare.com/pages/how-to/preview-with-cloudflare-tunnel/) mô tả đây là dịch vụ tunnel miễn phí. Script không mua tên miền, tạo dịch vụ trả phí hoặc cài service hệ thống.

## Luồng xác nhận

~~~mermaid
sequenceDiagram
    participant L as Laptop — web rạp local
    participant B as Backend
    participant D as SQL Server cloud nhóm
    participant W as Điện thoại — MoMo giả lập HTTPS
    L->>B: Tạo giao dịch cho đúng lượt giữ
    B->>D: Lưu PENDING, tiền, vé, hạn và băm khóa
    B-->>L: QR chứa liên kết ví
    W->>B: Qua cổng ví: khóa QR, số tiền, CSRF và đồng ý giả lập
    B->>D: Khóa suất và giao dịch; kiểm tra giữ ghế/đơn hàng
    B->>D: Trong cùng transaction: vé PAID, hóa đơn PAID, giao dịch SUCCESS
    B-->>W: Kết quả giao dịch
    L->>B: Kiểm tra trạng thái mỗi 3 giây
    B-->>L: SUCCESS rồi mở vé
~~~

Điện thoại gọi backend qua HTTPS; không kết nối SQL Server trực tiếp. Backend quyết định trạng thái và số tiền. Không có endpoint đổi một cờ true/false tùy ý.

Cổng mặc định 8084 chỉ lắng nghe 127.0.0.1 và chỉ chuyển tiếp:
- GET /demo-wallet, /demo-wallet/pay/{UUID} và /demo-wallet/health (chỉ trả trạng thái ví/kết nối database).
- POST /demo-wallet/api/{UUID}/status, /confirm, /cancel.
- Ba tài nguyên CSS, JavaScript và favicon của ví.

Các đường dẫn web rạp, tài khoản, vé, quản trị, URL có query, path bị mã hóa khác quy tắc và phương thức khác đều bị chặn. Tunnel chỉ trỏ vào cổng này. Web rạp 8082 chỉ lắng nghe trên máy local khi dùng script. Cookie phiên ví có Secure, HttpOnly và SameSite=Lax. Script tắt chỉ dừng các PID khớp đường dẫn và thời điểm tiến trình do chính nó tạo.

## Validation và trạng thái

| Trạng thái | Ý nghĩa |
|---|---|
| PENDING | Đúng lượt giữ, chưa xác nhận, còn hạn |
| SUCCESS | Backend đã xác nhận và xuất vé trong cùng giao dịch |
| CANCELLED | Người dùng hủy QR; giữ ghế theo hạn ban đầu |
| EXPIRED | Đã đến hạn giữ 5 phút hoặc giờ chiếu |
| INVALIDATED | Ghế/đơn hàng/phim thay đổi, QR được thay thế hoặc vé đã trả bằng cách khác |

- QR gắn với chủ đơn, suất, toàn bộ danh sách ID vé và tổng tiền vé + bắp nước.
- Số tiền là số đồng nguyên dương, tối đa 1 tỷ; số tiền điện thoại gửi phải khớp database. JSON không ép chuỗi/số thập phân thành số nguyên hoặc ép chuỗi thành đồng ý.
- Khóa QR ngẫu nhiên 256 bit; chỉ băm SHA-256 được lưu trong database. Khóa nằm trong fragment của liên kết, không gửi trong URL truy cập server. Trình duyệt đọc rồi xóa fragment khỏi thanh địa chỉ, giữ khóa trong sessionStorage của tab để tải lại trang.
- Điện thoại có session/CSRF riêng, không cần đăng nhập tài khoản của laptop. Thiếu khóa, sai khóa, sai CSRF, thiếu xác nhận giả lập hoặc sai tiền đều bị từ chối.
- Khóa suất trước rồi khóa giao dịch để đồng bộ với giữ/hủy ghế và các phương thức thanh toán khác. Xác nhận trùng trả lại cùng kết quả; hai thiết bị xác nhận cùng lúc chỉ tạo một hóa đơn.
- Tạo lại cùng QR trong cùng session và cùng lượt giữ không gia hạn. QR mới thay thế QR cũ. Giới hạn 10 QR mới/tài khoản/phút.
- Không xuất vé nếu phim ngừng hoạt động, ghế hết hạn, hủy/đổi lượt giữ, đơn hàng đổi tiền, hoặc thanh toán tại quầy đã hoàn tất.
- Mất phản hồi khi xác nhận: trình duyệt kiểm tra lại backend, không tự gửi thêm xác nhận. Mất kết nối: nút xác nhận bị khóa đến khi xác minh lại. Đếm ngược dựa vào giờ server và thời gian trôi của trình duyệt.
- Thông tin QR chỉ được laptop đã đăng nhập đúng chủ đơn đọc. GET /finish chỉ đọc vé đã thanh toán; không tạo thanh toán.

## Database và tương thích

Bảng demo_payments lưu giao dịch, các bản chụp thông tin thanh toán, khóa đã băm, trạng thái và mốc thời gian. Các CHECK bảo vệ số tiền, hạn và paid_at. Migration mở rộng đúng giá trị payment_method của tickets, booking_orders, ticket_refunds thành COUNTER/MOMO/MOMO_DEMO. Dữ liệu vé/hóa đơn cũ không bị đổi.

Vé và hóa đơn giả lập dùng MOMO_DEMO, tham chiếu DEMO-{UUID}. Hoàn vé giả lập xử lý nội bộ, không gọi API hoàn tiền MoMo thật. Luồng tạo QR thật bị ẩn khi bật giả lập; callback/query cho các giao dịch MoMo cũ vẫn được giữ.

**Các thành viên nhóm phải dùng bản code có enum MOMO_DEMO trước khi đọc vé giả lập trên cloud chung.** Các bài kiểm thử xóa dữ liệu chỉ chạy trên database riêng có hậu tố _test, tuyệt đối không dùng cloud nhóm.

Profile cloud bật giả lập mặc định và yêu cầu địa chỉ ví HTTPS công khai trước khi tạo QR. Chạy thông thường có thể đặt DEMO_WALLET_ENABLED=false để quay về MoMo Sandbox với cấu hình MoMo hiện có. Script demo luôn bật giả lập và tắt gửi email trong phiên demo.

## Kiểm chứng

- Bộ Java toàn dự án: 357 bài, không lỗi. Cổng ví được kiểm thử lại sau khi thêm bảo vệ cookie.
- JavaScript chọn ghế + ví: 20 bài, không lỗi.
- Chrome: hai context độc lập đại diện laptop/điện thoại, thanh toán qua cổng ví riêng, tải lại trang từ sessionStorage, kiểm tra đồng ý, tự nhận vé, màn hình 375px, sáng/tối và không có lỗi JavaScript.
- HTTPS miễn phí thật: kiểm tra chứng chỉ, giao diện ví, cookie bảo vệ và chặn các trang nội bộ.
- Migration cloud đã kiểm tra trước/sau: 11 vé, 4 hóa đơn cũ giữ nguyên; không tạo giao dịch thử trên cloud trong quá trình kiểm thử.

Có thể chạy kiểm thử Java bằng profile test với cấu hình database _test hợp lệ theo docs/KE_HOACH_KIEM_THU.md; JavaScript:

~~~powershell
node --test src/test/js/seat-booking.test.cjs src/test/js/demo-wallet.test.cjs
~~~
