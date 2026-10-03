# Hữu Tài — Lịch sử đặt vé và chi tiết đặt vé

Triển khai mục 4 của `phan-cong-bo-sung-chuc-nang.txt`, trên nền Tuấn Thanh `27576de`, nhánh local `codex/huu-tai-booking-history`.

## Code của nhóm

Đã fetch ngày 02/10/2026: `origin/Tuấn_Thanh` vẫn ở `27576de` (17:04 +0700), `origin/Hữu_Thắng` vẫn ở `6e6a0e7` (16:22 +0700). Cả hai đã nằm trong lịch sử HEAD đang dùng; không có commit mới để pull. Chưa có đầy đủ voucher/menu tài khoản và QR/camera theo phân công của hai bạn trên các remote này.

## Một nơi xem lại vé

- `/lich-su-dat-ve`: mỗi thẻ poster là một giao dịch thuộc tài khoản hiện tại; mã/ngày đặt, phim, suất, tổng tiền và trạng thái. Mới nhất trước, 12 đơn/trang, lọc trạng thái.
- `/lich-su-dat-ve/{receiptCode}`: **Chi tiết đặt vé**, gồm phim/phòng/suất/ghế, mã vé và QR hợp lệ của từng vé, vé/combo, ưu đãi đã lưu, thanh toán và hoàn tiền.
- Header chỉ còn mục **Lịch sử đặt vé**; bỏ hai tab trùng nhau. `/ve-cua-toi` chuyển tới lịch sử sau đăng nhập để các liên kết cũ vẫn hoạt động.
- Tải ảnh QR và hủy từng vé nằm trong chi tiết đơn, theo chính sách hủy hiện có. Giữ các route thanh toán/hủy cũ.
- Kiểm tra chủ sở hữu ở danh sách, chi tiết, hóa đơn và tải QR; nhân viên/quản trị cũng không được xem đơn người khác qua các trang cá nhân này. Vé hủy/chưa thanh toán không có QR vào phòng.

## Tích hợp Hữu Thắng

`BookingTicketDataService` dựng dữ liệu vé chung cho `ReceiptService` và `BookingDetailService`. Fragment `booking/ticket-passes.html` dùng cùng mã số/QR trên chi tiết và hóa đơn. QR dùng `QrCodeService` hiện có, chứa ID vé dạng số tương thích bộ soát vé. Route tải SVG kiểm tra chủ đơn/trạng thái, hóa đơn có QR từng vé để in.

Giữ nguyên `ticket-fragment.html`, controller thanh toán và soát vé. Các điểm chung cần sửa để tích hợp gồm `ReceiptService`, `ReceiptView`, template hóa đơn, dịch vụ giữ/hủy vé. Khi gộp bản mới của Thắng cần giữ một nguồn dữ liệu vé/QR.

Snapshot vé được lưu ngay khi giữ ghế: ID, ghế, loại ghế, giá và thời điểm giữ. Đơn hết hạn/hủy giữ ghế được đóng trước khi xóa vé tạm; ghế được giải phóng, dữ liệu lịch sử đã lưu được giữ. Lượt đặt mới sau hết hạn có mã đơn mới. Biên nhận hủy đối chiếu chủ vé, suất, thời điểm thanh toán chính xác microsecond và mã thanh toán để không gộp hai giao dịch gần nhau. Hoàn tiền dùng giá thực trả sau phân bổ ưu đãi; hóa đơn giữ giá gốc và khoản giảm riêng.

## Tích hợp Tuấn Thanh

`BookingOrder` lưu `discountAmount`, `appliedVoucherCode`, `appliedVoucherName`, `ticketSnapshot`. Lịch sử và hóa đơn đọc dữ liệu đã lưu.

Thanh gọi `BookingOrderService.applyValidatedOffer(userId, showtimeId, code, name, amount)` **sau khi kiểm tra điều kiện voucher**. Dịch vụ kiểm tra số tiền và lưu ưu đãi vào đúng đơn; không có endpoint nhận mức giảm tùy ý từ trình duyệt. Đổi vé/combo sẽ xóa ưu đãi cũ để cần kiểm tra lại, kể cả đổi hai sản phẩm cùng giá.

Phần lưu/hiển thị ưu đãi của Tài đã có và được test. Giao diện nhập mã và quy tắc voucher thuộc Thanh chưa có trên remote đã fetch, nên chưa thể kiểm tra toàn bộ luồng voucher qua giao diện. Menu tài khoản mới dùng `/lich-su-dat-ve`; liên kết `/ve-cua-toi` cũ cũng tới cùng trang, tránh hai mục trùng nội dung.

## Database và chạy ứng dụng

Migration: `database/migrations/20261002-booking-history-snapshots.sql`; hai schema khởi tạo cũng cập nhật. Đã chạy migration hai lần trên database test, rồi thành công trên database cloud đang dùng. Sao lưu mã/trạng thái 22 đơn trước migration tại `.local-backup` (Git bỏ qua), không đổi secrets kết nối.

Đơn cũ còn vé được bổ sung snapshot; đơn nháp không còn ghế giữ được đóng. Ghế/ID của vé tạm đã bị xóa trước thay đổi này không thể khôi phục từ dữ liệu hiện có. Không tạo dữ liệu giả; các lượt đặt mới giữ đủ snapshot.

Java 21: `C:\Users\ADMIN\.jdks\ms-21.0.8`. `scripts/mvn21.ps1` hỗ trợ tìm JDK 21 trong `.jdks`. Từ thư mục dự án chạy `powershell -ExecutionPolicy Bypass -File scripts/run-cloud.ps1` để chạy profile cloud trên cổng mặc định 8082; chỉ chạy một tiến trình ứng dụng trên cổng này. Bản JAR kiểm tra hiện chạy tại `http://localhost:8082/lich-su-dat-ve`.

## Kiểm tra

- `mvn -q test`: **479/479**, không lỗi/bỏ qua.
- `node --test src/test/js/*.test.cjs`: **21/21**.
- `mvn -q -DskipTests package`: thành công bằng Java 21.
- Test tích hợp dùng dịch vụ/database thật trên database test: đăng nhập/chủ đơn, phân trang/lọc, lịch sử khớp hóa đơn, QR/tải ảnh, hủy/hoàn tiền, hết hạn vẫn giữ dữ liệu vé, đặt lại không ghi đè lịch sử, ưu đãi đã lưu và không hoàn quá tiền thực trả, đổi combo cùng giá xóa ưu đãi.
- Test tương thích cập nhật kỳ vọng `/ve-cua-toi` chuyển sang lịch sử và hóa đơn có QR từng vé theo phân công.
- Kiểm tra bản JAR thực trên 8082: đăng nhập tài khoản demo, liên kết cũ chuyển đúng trang, chi tiết/hóa đơn khớp 3 mã vé và tổng tiền, tải thành công SVG vé 30, nút hủy hiển thị đúng mức hoàn theo chính sách. Đã kiểm tra màn hình 390/1280 px và giao diện sáng/tối; ba trang không tràn ngang. Ảnh giao diện lưu tại `.local-backup/booking-history-integrated-final.png`.

Chỉ lưu thay đổi tại máy để Tài kiểm tra; không commit hoặc push.
