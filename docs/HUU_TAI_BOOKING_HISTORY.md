# Hữu Tài — Lịch sử đặt vé và chi tiết đặt vé

Triển khai mục 4 của `phan-cong-bo-sung-chuc-nang.txt`, trên nền Tuấn Thanh `27576de`, nhánh local `codex/huu-tai-booking-history`.

## Code của nhóm

Bản tích hợp ngày 03/10/2026 dùng Hữu_Thắng `60740b7`, develop `c85b882` (đã gồm Hữu_Tài `3ad1e66` và Minh_Thọ `23ac661`) và Tuấn_Thanh `0660e5d`. Đã kết hợp voucher/menu, lịch sử/chi tiết, mã vé 8 số, QR chung và camera soát vé. Xem [báo cáo rà soát](MODULE_2_INTEGRATION_REVIEW.md).

## Một nơi xem lại vé

- `/lich-su-dat-ve`: mỗi thẻ poster là một giao dịch thuộc tài khoản hiện tại; mã/ngày đặt, phim, suất, tổng tiền và trạng thái. Mới nhất trước, 12 đơn/trang, lọc trạng thái.
- `/lich-su-dat-ve/{receiptCode}`: **Chi tiết đặt vé**, gồm phim/phòng/suất/ghế, mã vé và QR hợp lệ của từng vé, vé/combo, ưu đãi đã lưu, thanh toán và hoàn tiền.
- Header chỉ còn mục **Lịch sử đặt vé**; bỏ hai tab trùng nhau. `/ve-cua-toi` chuyển tới lịch sử sau đăng nhập để các liên kết cũ vẫn hoạt động.
- Tải ảnh QR và hủy từng vé nằm trong chi tiết đơn, theo chính sách hủy hiện có. Giữ các route thanh toán/hủy cũ.
- Kiểm tra chủ sở hữu ở danh sách, chi tiết, hóa đơn và tải QR; nhân viên/quản trị cũng không được xem đơn người khác qua các trang cá nhân này. Vé hủy/chưa thanh toán không có QR vào phòng.

## Tích hợp Hữu Thắng

`BookingTicketDataService` dựng dữ liệu vé chung cho `ReceiptService` và `BookingDetailService`. Sau khi tích hợp Module 2, mã vé công khai có 8 số; QR riêng chứa `UTE-CINEMA:TICKET:V2:<mã>`. Trang chi tiết giữ QR từng vé và kiểm tra chủ đơn khi tải SVG. Hóa đơn nhiều ghế dùng một QR chung `UTE-CINEMA:BOOKING:V2:<receiptCode>` cùng bảng mã ghế để in A4. Vé hủy giữ nguyên mã và không còn QR vào phòng.

Giữ nguyên `ticket-fragment.html`, controller thanh toán và soát vé. Các điểm chung cần sửa để tích hợp gồm `ReceiptService`, `ReceiptView`, template hóa đơn, dịch vụ giữ/hủy vé. Khi gộp bản mới của Thắng cần giữ một nguồn dữ liệu vé/QR.

Snapshot vé được lưu ngay khi giữ ghế: ID, ghế, loại ghế, giá và thời điểm giữ. Đơn hết hạn/hủy giữ ghế được đóng trước khi xóa vé tạm; ghế được giải phóng, dữ liệu lịch sử đã lưu được giữ. Lượt đặt mới sau hết hạn có mã đơn mới. Biên nhận hủy đối chiếu chủ vé, suất, thời điểm thanh toán chính xác microsecond và mã thanh toán để không gộp hai giao dịch gần nhau. Hoàn tiền dùng giá thực trả sau phân bổ ưu đãi; hóa đơn giữ giá gốc và khoản giảm riêng.

## Tích hợp Tuấn Thanh

`BookingOrder` lưu `discountAmount`, `appliedVoucherCode`, `appliedVoucherName`, `ticketSnapshot`. Lịch sử và hóa đơn đọc dữ liệu đã lưu.

Giao diện gọi `BookingOrderService.applyVoucher(userId, showtimeId, code, expectedIds)`. Server kiểm tra CSRF, đúng lượt giữ, ngày hiệu lực, mức tối thiểu và mức giảm. Voucher được tính lại khi đổi combo; bắp nước không được giảm. Giá vé/lượt giữ thay đổi cần áp dụng mã lại. `applyValidatedOffer` giữ làm API nội bộ cho ưu đãi đã xác thực; loại này bị xóa khi đổi vé/combo. Trình duyệt không gửi mức giảm tùy ý.

Đã kiểm thử giao diện nhập/bỏ mã, QR cũ khi tổng tiền đổi, thanh toán, hóa đơn và hoàn tiền sau giảm. Tên ưu đãi hỗ trợ 200 ký tự trên voucher và đơn. Menu tài khoản dùng `/lich-su-dat-ve`; `/ve-cua-toi` cũ chuyển tới cùng trang.

## Database và chạy ứng dụng

Migration: `database/migrations/20261002-booking-history-snapshots.sql`; hai schema khởi tạo cũng cập nhật. Đã chạy migration hai lần trên database test, rồi thành công trên database cloud đang dùng. Sao lưu mã/trạng thái 22 đơn trước migration tại `.local-backup` (Git bỏ qua), không đổi secrets kết nối.

Đơn cũ còn vé được bổ sung snapshot; đơn nháp không còn ghế giữ được đóng. Ghế/ID của vé tạm đã bị xóa trước thay đổi này không thể khôi phục từ dữ liệu hiện có. Không tạo dữ liệu giả; các lượt đặt mới giữ đủ snapshot.

Java 21: `C:\Users\ADMIN\.jdks\ms-21.0.8`. `scripts/mvn21.ps1` hỗ trợ tìm JDK 21 trong `.jdks`. Từ thư mục dự án chạy `powershell -ExecutionPolicy Bypass -File scripts/run-cloud.ps1` để chạy profile cloud trên cổng mặc định 8082; chỉ chạy một tiến trình ứng dụng trên cổng này. Bản JAR kiểm tra hiện chạy tại `http://localhost:8082/lich-su-dat-ve`.

## Kiểm tra

- Bộ kiểm thử tích hợp ngày 03/10/2026: **543/543 Java**, không lỗi/bỏ qua, trên SQL Server riêng.
- `node --test src/test/js/*.test.cjs`: **35/35**.
- `mvn -q -DskipTests package`: thành công bằng Java 21.
- Test tích hợp dùng dịch vụ/database thật trên database test: đăng nhập/chủ đơn, phân trang/lọc, lịch sử khớp hóa đơn, QR/tải ảnh, hủy/hoàn tiền, hết hạn vẫn giữ dữ liệu vé, đặt lại không ghi đè lịch sử, ưu đãi đã lưu và không hoàn quá tiền thực trả, đổi combo cùng giá xóa ưu đãi.
- Test tương thích giữ `/ve-cua-toi` chuyển sang lịch sử; chi tiết có QR từng vé, hóa đơn nhiều ghế có một QR chung và mã 8 số cho mỗi ghế.
- Kiểm tra bản JAR thực trên 8082: đăng nhập tài khoản demo, liên kết cũ chuyển đúng trang, chi tiết/hóa đơn khớp 3 mã vé và tổng tiền, tải thành công SVG vé 30, nút hủy hiển thị đúng mức hoàn theo chính sách. Đã kiểm tra màn hình 390/1280 px và giao diện sáng/tối; ba trang không tràn ngang. Ảnh giao diện lưu tại `.local-backup/booking-history-integrated-final.png`.

Thông tin kiểm tra giao diện thủ công phía trên là của bản Tài trước tích hợp. Kết quả hiện tại và việc đồng bộ GitHub/Render được ghi trong báo cáo rà soát.
