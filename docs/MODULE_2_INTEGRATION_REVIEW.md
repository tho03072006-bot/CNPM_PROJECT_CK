# Rà soát tích hợp UTE Cinema — 03/10/2026

## Các nhánh đã đối chiếu

| Nguồn | Commit | Kết quả trong bản tích hợp |
| --- | --- | --- |
| Hữu_Thắng | `60740b7` | Giữ ghế, giá vé, thanh toán, hóa đơn A4, mã vé 8 số, QR chung, camera/ảnh và soát vé |
| develop | `c85b882` | Lấy thay đổi mới của nhóm; gồm Hữu_Tài và Minh_Thọ |
| Hữu_Tài | `3ad1e66` | Lịch sử/chi tiết theo giao dịch, snapshot ghế, ưu đãi đã lưu, biên nhận hoàn tiền |
| Tuấn_Thanh | `0660e5d` | Voucher từ database, menu tài khoản, định dạng tiền |

Các nhánh combo, tồn kho, dọn hóa đơn, bổ sung chức năng và giữ ghế đã nằm trong lịch sử các nguồn trên. `main` còn một merge commit riêng, nhưng code tài khoản/OTP của thay đổi đó đã có trong bản này. `dev_final` là nhánh khởi tạo từ snapshot, thiếu các thay đổi mã vé/QR của Thắng; không dùng snapshot đó để thay thế code đã tích hợp.

62 file kiểm thử trên nhánh Hữu_Thắng vẫn được giữ, hiện có tổng cộng 66 file kiểm thử Java/JavaScript. Không xóa file chức năng hoặc file kiểm thử của nhánh gốc trong lần tích hợp này.

## Chức năng và validation đã kiểm tra

| Luồng | Các điều kiện chính |
| --- | --- |
| Phim/phòng/suất chiếu | Phân quyền quản trị; ID, giá, ngày giờ, thời lượng, lịch trùng; bảo toàn suất có vé; tạo suất mới rồi đặt vé |
| Chọn và giữ ghế | Đúng phòng/suất, ID dương/không trùng, ghế trống, tối đa số chỗ, ghế đôi, tránh chỗ lẻ, độ tuổi/đồng ý; hết hạn chính xác 5 phút |
| Đổi/hủy giữ ghế | Khớp đúng lượt giữ; không gia hạn khi đổi; tab cũ bị chặn; thất bại rollback; snapshot giữ lại trước xóa |
| Giá vé | Giá đồng nguyên trong giới hạn cột tiền; hệ số VIP/COUPLE; làm tròn từng ghế; giá vé đã giữ/đã trả được giữ |
| Bắp nước/tồn kho | Món đang bán, số lượng mỗi món/toàn đơn, tồn kho đủ; trừ kho khi thanh toán; thiếu kho rollback |
| Voucher | Chuẩn hóa mã; tồn tại/active/ngày hiệu lực/tiền vé tối thiểu/mức trần; không giảm bắp nước; CSRF và đúng tài khoản/lượt giữ |
| Thay đổi đơn | Voucher tính lại trước checkout; OTP/QR cũ không thanh toán khi tiền hoặc lượt giữ đổi; đơn PAID/CANCELLED giữ số tiền đã chốt |
| Thanh toán | Số tiền do server tính; đúng lượt giữ/hạn/phim/suất; chống xác nhận trùng; ví điện thoại dùng session riêng, token và CSRF |
| Hóa đơn | Thành công chuyển thẳng đến hóa đơn; giá gốc + ưu đãi + combo khớp tổng đã trả; một QR chung cho nhiều ghế; in A4 |
| Mã vé/QR | Mã công khai 8 số duy nhất, không tái sử dụng; QR V2; giữ mã khi hủy; vé hủy/HELD không vào phòng |
| Soát vé | Nhân viên/quản trị; lookup không check-in; phải xác nhận POST với CSRF; đúng suất/ngày/giờ; QR chung kiểm tra cả nhóm và check-in nguyên tử |
| Hủy/hoàn tiền | Chủ vé; vé PAID chưa sử dụng; mốc 24 giờ/2 giờ; tiền thực trả sau giảm; giữ chứng từ/mã và giải phóng ghế; cạnh tranh check-in/hủy chỉ một bên thắng |
| Lịch sử/chi tiết | Chỉ chủ tài khoản; phân trang/lọc; đơn đã hủy/hết hạn giữ snapshot; không gộp nhầm hoàn tiền của hai đơn gần nhau; QR tải đúng chủ/đúng vé |
| Tài khoản/OTP | Đăng ký/đăng nhập/khôi phục/hồ sơ, định dạng dữ liệu, vai trò, hạn và số lần OTP; email gửi qua mock trong test |
| JavaScript/tài nguyên | Ví điện thoại nạp money.js trước hành vi thanh toán; chọn ghế, deadline, camera/ảnh, QR chung và tài nguyên phiên bản mới |

## Lỗi đã sửa trong lần rà soát

1. Voucher có tên 151–200 ký tự làm lỗi lưu đơn: mở rộng `applied_voucher_name` lên NVARCHAR(200), thêm validation độ dài tên voucher và kiểm thử SQL Server thực.
2. Backfill snapshot có thể dùng giá đã giảm rồi hóa đơn trừ ưu đãi lần nữa: migration lấy `original_price` nếu có, vẫn giữ dữ liệu snapshot đã tồn tại.
3. Một đơn nháp cũ lưu tổng chưa trừ ưu đãi: migration chỉ đồng bộ tổng DRAFT hợp lệ; kiểm thử chạy hai lần và bảo toàn PAID/CANCELLED.
4. Bảo vệ số tiền đơn đã đóng khi gọi tính lại ưu đãi; bổ sung test QR cũ khi áp dụng/ngừng voucher, thiếu/trùng mã vé và giữ ghế hết hạn.
5. Health ví có `checkoutVersion=2` để phân biệt bản hỗ trợ tích hợp voucher; `status=UP` riêng chỉ chứng minh kết nối database.
6. Mã voucher sai định dạng/quá 40 ký tự bị chặn trước truy vấn database; vẫn nhận chữ thường và khoảng trắng đầu/cuối hợp lệ.

## Bố cục sau tích hợp

- Menu tài khoản có Hồ sơ, Ưu đãi, Lịch sử đặt vé và Đăng xuất. Bỏ hai mục trùng đường dẫn; trang hiện tại được đánh dấu.
- Trang ưu đãi tách rõ mức giảm, mã, điều kiện và nút chọn suất. Dùng cùng màu/chữ/khoảng cách của website.
- Thanh toán hai cột trên desktop, một cột trên mobile; vé giữ gọn, tổng tiền và voucher theo một thứ tự rõ ràng.
- Tablet/mobile có logo/tài khoản phía trên, điều hướng ở hàng dưới. Menu không bị cắt; liên kết vai trò dài được cuộn trong thanh điều hướng.
- Chi tiết đặt vé ưu tiên ghế/QR trên điện thoại, sau đó là vé/combo, thanh toán và thông tin giao dịch. Hóa đơn giữ bố cục A4 và QR chung.

## Kiểm chứng

- Toàn bộ **556 kiểm thử Java đạt**, 0 lỗi, 0 bỏ qua, trên SQL Server riêng `cinema_booking_module2_review_test`.
- Toàn bộ **37 kiểm thử JavaScript đạt**. Kiểm thử health phiên bản mới đạt sau lần thay đổi cuối.
- Đóng gói JDK 21 thành công trong thư mục riêng để không ghi đè JAR đang chạy.
- Hibernate cloud dùng `ddl-auto=validate`; schema được xác nhận tương thích. Không chạy bộ test dọn dữ liệu lên cloud.
- Hai migration ngày 03/10 đã áp dụng trên cloud. Số bản ghi ở 13 bảng kiểm tra giữ nguyên; không còn đơn có tổng lệch công thức và vé PAID nào thiếu mã công khai.
- File secret, build output, dữ liệu thử nghiệm và `tmp/` không đưa vào commit.
- Render Chrome riêng 40 trường hợp: 5 trang (ưu đãi, thanh toán, lịch sử, chi tiết, hóa đơn), 4 chiều rộng 320/390/768/1280 và 2 giao diện sáng/tối. Không tràn chiều ngang toàn trang, không lỗi JavaScript; kiểm tra menu mobile và xem ảnh render.

Log cục bộ trong `target/module2-audit-final-java.log`, `target/module2-audit-health-version.log`, `target/module2-audit-cloud.log`. Ảnh/HTML và `layout-report.json` nằm trong `target/ui-review`. Đây là file bỏ qua bởi Git, chỉ dùng dữ liệu giả trên database test.

## Đồng bộ web local và Render

Render dùng nhánh `Hữu_Thắng` theo `render.yaml`. Bản trước triển khai mới trả `status=UP`, chưa có `checkoutVersion=2`, `/js/money.js` trả 404. Web local và ví Render cần triển khai cùng commit tích hợp để voucher không bị tính lại theo logic cũ.

Theo yêu cầu cuối, chỉ sửa project trên máy; chưa commit/push hoặc triển khai Render. Sau khi chủ dự án tự commit/push, triển khai dịch vụ **momo-gia-lap-nhom8** bằng **Manual Deploy → Deploy latest commit**. Kiểm tra deploy đúng commit, health `UP`/`checkoutVersion=2`, `money.js` trả 200 và nội dung tài nguyên khớp bản local. Health không thay thế kiểm thử giao dịch.

Các test thanh toán dùng database riêng và API MoMo mock. Chưa xác nhận một giao dịch mới qua thiết bị thật trên bản Render mới trong báo cáo này; không phát sinh tiền thật hoặc dọn dữ liệu cloud để kiểm thử.

## Sửa lỗi QR bị vô hiệu ngay khi mở ví ngày 03/10/2026

Đối chiếu chỉ đọc giao dịch trong ảnh: QR 441.000đ; đơn có 270.000đ vé, 198.000đ bắp nước và voucher giảm 27.000đ. Backend Render cũ ghi lại tổng 468.000đ khi mở ví, còn voucher vẫn tồn tại, nên đối chiếu số tiền làm QR thành INVALIDATED. Health online thực tế chỉ trả `status=UP`, chưa có `checkoutVersion=2`.

Web local nay kiểm tra hợp đồng checkout tại health trước khi tạo/tái dùng QR, trước khi khóa suất hoặc sửa đơn. Ví cũ, mất mạng, chuyển hướng hoặc phản hồi sai đều không được tạo QR mới. Khách quay về trang thanh toán với thông báo rõ ràng và có thể chọn phương thức khác; giữ ghế không được gia hạn. Warmup cũng dùng cùng kiểm tra phiên bản, không báo sẵn sàng chỉ vì health UP.

Hai backend Spring độc lập đã chạy với SQL kiểm thử chung: tái hiện tổng bị ghi thành 468.000đ, backend ví mới tính đúng 441.000đ khi đọc lại, giữ giảm giá 27.000đ sau ba lần mở ví, từ chối xác nhận 468.000đ và chấp nhận 441.000đ. Xác nhận lặp chỉ phát hành hai mã vé, một hóa đơn và một lần trừ tồn kho; web local nhận SUCCESS và chuyển đến hóa đơn.

Frontend đối chiếu đúng publicId, số tiền nguyên dương tối đa một tỷ, danh sách ghế không rỗng/trùng, thời gian và thông tin hiển thị hợp lệ. QR/copy bị khóa khi chưa xác minh hoặc hết thời gian, thông báo kết thúc không hiển thị lặp. Các validation token, CSRF theo thiết bị, đồng ý mô phỏng, hết hạn, đổi ghế/voucher, thanh toán đồng thời và xác nhận lặp đã được kiểm thử lại. Log: `target/wallet-fix-targeted-java.log`, `target/wallet-fix-full-java.log`, `target/wallet-fix-package.log`.

Chưa commit/push/deploy theo yêu cầu chủ dự án. Sau khi đưa bản sửa lên nhánh Render đang theo dõi, cần Manual Deploy dịch vụ hiện có, kiểm tra health `UP`/`checkoutVersion=2` và tạo QR mới từ lượt giữ còn hiệu lực. QR đã INVALIDATED/EXPIRED không được tự kích hoạt lại.

## Kiểm chứng tải tài nguyên ví sau deploy

Chủ dự án đã commit bản tích hợp `7978769`. Bản online trả health UP/checkoutVersion=2, HTML nạp money.js có tham số phiên bản, nhưng gateway chặn query nên URL money.js có `v` trả 404, gây lỗi đọc `format` trên undefined.

Bản sửa tiếp theo cho phép duy nhất query phiên bản hợp lệ trên tài nguyên GET được liệt kê, giữ nguyên hạn chế cho trang/API. Ví có định dạng tiền dự phòng và phiên bản JS mới. Đã chạy 29 kiểm thử Java tập trung (gateway/health, không database), toàn bộ 39 kiểm thử JavaScript và bốn ca trình duyệt qua gateway thật. Đây là kiểm tra bổ sung sau mốc toàn bộ 556 Java/37 JavaScript ở trên; không chạy lại test dọn database cloud.

Trang mobile/desktop tải đúng script có query, hiển thị 441.000đ và xác nhận giao dịch giả thành công. Thiếu money.js vẫn định dạng đúng từ số tiền hợp lệ; phản hồi số tiền sai không được xác nhận. HTML là template thật, proxy là lớp DemoWalletGateway thật, browser riêng không dùng session người dùng. Ảnh/JSON/log cục bộ: `target/wallet-runtime-review/`, `target/wallet-assets-java.log`, `target/wallet-assets-package.log`.
