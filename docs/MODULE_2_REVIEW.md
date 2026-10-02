# Module 2 — Kết quả sửa và kiểm tra ngày 01/10/2026

Mã được sửa trực tiếp tại E:\Cong_Nghe_Phan_Mem\Project_Cuoi_Ky, nhánh Hữu_Thắng, dựa trên develop 486d75a. Chưa commit hoặc push.

## Đã xử lý

| Nội dung | Kết quả |
| --- | --- |
| Huỷ/dọn hết hạn cạnh tranh thanh toán | Cùng khoá suất chiếu; DELETE có điều kiện HELD trong SQL |
| Hai khách chọn khác ghế nhưng tạo chỗ lẻ | Kiểm tra sơ đồ sau khoá suất chiếu |
| Ghế COUPLE bị tính nhầm là một chỗ lẻ | Cặp còn nguyên được tính hai chỗ; không chặn sai |
| JSON mã ghế bị làm tròn hoặc ép kiểu | Chỉ nhận token số nguyên trong phạm vi Long |
| Yêu cầu quá nhiều mã ghế | Chặn trước khi truy vấn từng ghế; tối đa tám chỗ kể cả ghế đôi |
| Tab cũ huỷ hoặc thanh toán lượt mới | Kiểm tra đúng danh sách mã vé; MoMo gắn mã vé đầu lượt |
| Đúng mốc hết hạn | Hết hiệu lực tại heldAt + 5 phút, nhất quán giữa giữ/đơn/thanh toán |
| Đồng hồ sai múi giờ hoặc giờ máy khách | Epoch + giờ máy chủ + performance.now; nghiệp vụ dùng giờ Việt Nam |
| Giao diện mở ghế khi hết giờ hoặc mất phản hồi | Chờ máy chủ xác nhận; tự đọc lại trạng thái |
| Tải lại/tab khác | Khôi phục lượt giữ, tự cập nhật tám giây, focus/visibility/BroadcastChannel |
| Đổi ghế | Cùng giao dịch, rollback giữ nguyên ghế cũ, không kéo dài thời hạn |
| Gửi lại cùng lựa chọn | Trả lượt giữ hiện có, không tạo vé hoặc gia hạn |
| Gợi ý ghế | Theo số người, loại, ngân sách; nhóm liền nhau, ưu tiên trung tâm |
| Độ tuổi/quy định | Hiển thị P/K/T13/T16/T18; kiểm tra xác nhận tại máy chủ |

Checkbox xác nhận điều kiện tuổi không chứng minh tuổi thật; giao diện nhắc mang giấy tờ kiểm tra tại rạp. Giới hạn tám chỗ và chính sách ghế lẻ là quy định của dự án.

## Database cloud

- Ứng dụng tiếp tục dùng profile cloud và tệp cấu hình riêng bị Git bỏ qua.
- Kết nối SQL Server cloud thành công; Hibernate validate kiểm tra schema thành công.
- Khởi động ứng dụng thành công, trang chủ HTTP 200. Trong lần kiểm tra này đã tắt dọn ghế nền và gửi email.
- Không thay đổi bảng/cột hoặc dữ liệu phim/suất/phòng trên cloud. UNIQUE(showtime_id, seat_id) được giữ nguyên. API trạng thái vẫn dọn HELD hết hạn theo nghiệp vụ hiện có.
- Mã vé hiện có đủ nhận diện lượt giữ nên không cần migration mới.

## Kiểm chứng

| Kiểm tra | Kết quả |
| --- | --- |
| Toàn bộ Maven test, JDK 21 | 293 chạy, 0 lỗi, 0 thất bại, 0 bỏ qua |
| Kiểm thử JavaScript | 11/11 thành công |
| SQL Server thực | SQL Server 2025 LocalDB, database riêng cinema_booking_module2_test |
| Tranh chấp/rollback mới | BookingSafetyIntegrationTest: 4/4 thành công |
| API trạng thái từ phiên JPA mới, thêm phim qua quản trị | BookingStateIntegrationTest: 24/24 thành công |
| Ngăn phim thiếu/sai phân loại tuổi | Thêm 5 trường hợp qua HTTP quản trị; không lưu phim không hợp lệ |
| Luồng MoMo | API giả trong test; kiểm chữ ký và lưu dữ liệu/giao dịch qua service thật |
| Biên dịch tất cả mã/test | Thành công |
| Kiểm tra diff khoảng trắng | Thành công |

LocalDB là SQL Server thực. Do driver JDBC không hỗ trợ named pipe, lần kiểm tra dùng cầu nối tạm chỉ lắng nghe 127.0.0.1:11433; cấu hình database/login chỉ được truyền vào tiến trình test. Cloud của nhóm không được dùng để chạy các bài test có xoá dữ liệu. Môi trường tạm được dừng sau kiểm tra.

Kiểm thử JavaScript dùng DOM giả để kiểm tra hành vi. Chưa thực hiện đầy đủ kiểm tra trực quan bằng trình duyệt ở mọi kích thước/chế độ sáng tối và chưa thực hiện thanh toán MoMo thật trong lần này.


## Sửa lỗi đồng bộ ghế HTTP 500 trong ảnh ngày 01/10

Nguyên nhân: dọn vé hết hạn dùng bulk DELETE với clearAutomatically, làm Showtime đang giữ trong biến bị tách khỏi phiên JPA. Request API riêng truy cập movie/room LAZY sau đó gây LazyInitializationException. Trước sửa đã tái hiện 11 ca thất bại; sau sửa phải đọc lại Showtime sau toàn bộ bước dọn, giữ khoá suất trong cùng giao dịch. Không sửa theo mã phim hoặc mã suất cố định.

JavaScript phân biệt lỗi nghiệp vụ/máy chủ và mất kết nối, hiển thị đúng lý do; khi kết nối phục hồi, thông báo lỗi cũ được thay thế. Form tạo/sửa phim yêu cầu P/K/T13/T16/T18, chuẩn hoá khoảng trắng/chữ thường và mã cũ C13/C16/C18. Thêm phim qua quản trị rồi tạo suất mới được kiểm tra bằng HTTP cho từng phân loại.

Ứng dụng vật lý được khởi động lại ở localhost:8082 với đúng tham số ban đầu, profile cloud. Suất 577 trả HTTP 200, phim Bát Tiên Truy Tìm Lưu Ly Đăng, 140 ghế, phân loại K, mở đặt. Tệp JavaScript được nạp từ /js/seat-booking.js.

Rà toàn bộ 17 phim, 826 suất và cấu hình ghế trên cloud: không có tên/thời lượng/phân loại sai, giá không dương, giờ kết thúc sai, phòng không có ghế, cột ghế ngoài phòng hoặc loại ghế không hỗ trợ trong các điều kiện kiểm tra. Gọi trang ghế, API state và suggestions tại một suất tương lai của từng phim đang chiếu:

| Phim | Suất kiểm tra | Ghế | Trang / state / gợi ý |
| --- | --- | --- | --- |
| Út Lan 2 | 770 | 140 | 200 / 200 / 200 |
| Lên Hương | 751 | 140 | 200 / 200 / 200 |
| Vùng Đất Quỷ Dữ 2026 | 844 | 60 | 200 / 200 / 200 |
| Bóng Ma Nhà Hát | 804 | 140 | 200 / 200 / 200 |
| Tế Nhi Cải Mệnh | 841 | 60 | 200 / 200 / 200 |
| Yêu Nhân Thần Thám: Kỳ Án Trường An | 879 | 32 | 200 / 200 / 200 |
| Marine Yêu Dấu | 806 | 140 | 200 / 200 / 200 |
| Nghỉ Hè Sợ Nghỉ Hưu | 767 | 140 | 200 / 200 / 200 |
| Hope Vùng Tử Địa | 880 | 32 | 200 / 200 / 200 |
| Quý Tử Vượt Giàu | 842 | 60 | 200 / 200 / 200 |
| Bùa Yêu: Bí Mật Gia Tộc | 881 | 32 | 200 / 200 / 200 |
| Chiikawa: Bí Mật Đảo Người Cá | 840 | 60 | 200 / 200 / 200 |
| Tàu Buôn Người | 859 | 32 | 200 / 200 / 200 |
| Bát Tiên Truy Tìm Lưu Ly Đăng | 782 | 140 | 200 / 200 / 200 |
| Hòn Đảo Quên Lãng | 750 | 140 | 200 / 200 / 200 |
| Trại Buôn Người | 865 | 32 | 200 / 200 / 200 |
| Minions & Quái Vật | Không có suất; đã ngừng chiếu | — | Trang chi tiết 404 đúng chính sách |

Kiểm tra trên cloud dùng request đọc sơ đồ/gợi ý, không tạo vé hoặc thanh toán thật. Công cụ điều khiển trình duyệt không khởi động được (thiếu kernel.js), nên chưa xác minh trực quan lại trang trong lần sửa này.

## Tài liệu liên quan

- [API và cách chạy kiểm thử Module 2](../src/test/java/edu/hcmute/cnpm/cinema/booking/README.md)
- [Quyết định database ADR-1, ADR-2, ADR-3](DATABASE.md)
- [CGV FAQ](https://www.cgv.vn/default/faq/)
- [Lotte Cinema](https://www.lottecinemavn.com/LCHS/Contents/etc/terms-of-use.aspx)
- [Galaxy FAQ](https://www.galaxycine.vn/hoi-dap/)
- [MoMo — Idempotency](https://developers.momo.vn/v3/docs/payment/api/result-handling/idempotency/)
