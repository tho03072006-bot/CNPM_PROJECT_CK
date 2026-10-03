# Module 2 — Ghế và vé (Hữu Thắng)

## Chức năng

- Sơ đồ ghế, giá từ máy chủ: NORMAL ×1, VIP ×1,5, COUPLE ×2 và tính hai chỗ.
- Tối đa tám chỗ mỗi lượt; cho phép chọn khác hàng hoặc tách nhóm. Không tạo thêm một chỗ lẻ; một ghế COUPLE còn nguyên không phải chỗ lẻ.
- Giữ ghế năm phút, khôi phục khi tải lại; gửi lại cùng lựa chọn trả lượt giữ hiện có, không gia hạn.
- Đổi ghế trong một giao dịch: thất bại giữ nguyên ghế cũ, thành công giữ nguyên hạn cũ.
- Gợi ý nhóm ghế liền nhau theo số người, loại ghế và ngân sách; ưu tiên gần trung tâm.
- Hiển thị phân loại P/K/T13/T16/T18, hỗ trợ mã cũ C13/C16/C18. Máy chủ yêu cầu đồng ý quy định và xác nhận tuổi/giám hộ cho phim có giới hạn. Checkbox không thay giấy tờ tại rạp.
- Tự cập nhật mỗi tám giây, khi trở lại tab và qua BroadcastChannel; đọc lại trạng thái khi phản hồi thao tác bị mất.
- Đồng hồ dùng thời điểm dạng epoch của máy chủ và thời gian đơn điệu của trình duyệt. Hết đồng hồ không tự biến ghế thành trống.

## API

Danh tính lấy từ session, giá lấy từ máy chủ. Mã ghế/mã vé trong JSON phải là số nguyên dương; không chấp nhận chuỗi số, boolean, số thập phân, mã trùng hoặc vượt Long.

| API | Nội dung |
| --- | --- |
| GET /booking/showtime/{id} | Trang chọn ghế, khôi phục lượt giữ |
| GET /booking/showtime/{id}/state | Sơ đồ, lượt giữ của chính khách, serverTimeMillis, expiresAtMillis, trạng thái mở bán/thanh toán; no-store |
| GET /booking/showtime/{id}/suggestions?admissions=2&seatType=ANY&maxBudget=300000 | Gợi ý ghế; loại ANY/NORMAL/VIP/COUPLE, ngân sách tùy chọn |
| POST /booking/showtime/{id}/hold | JSON seatIds, ageConfirmed, termsAccepted; thêm expectedTicketIds khi đổi ghế |
| POST /booking/showtime/{id}/cancel | JSON ticketIds của đúng lượt giữ cần huỷ |

Ví dụ giữ ghế:

```json
{"seatIds":[1,2],"ageConfirmed":true,"termsAccepted":true}
```

Đổi ghế:

```json
{"seatIds":[3,4],"expectedTicketIds":[10,11],"ageConfirmed":true,"termsAccepted":true}
```

Huỷ:

```json
{"ticketIds":[10,11]}
```

AJAX gửi Accept: application/json hoặc X-Requested-With: XMLHttpRequest. Lỗi nghiệp vụ dùng handler chung, tranh chấp ghế trả 409, nội dung không hợp lệ trả 400.

## Giao dịch và thời gian

Mọi thao tác ghi giữ/đổi/huỷ/thanh toán/đơn hàng dùng khoá PESSIMISTIC_WRITE trên suất chiếu; giữ ghế khóa tiếp tài khoản theo cùng thứ tự. Kiểm tra ghế lẻ chạy sau khi có khóa, tránh hai khách chọn khác ghế nhưng cùng tạo một chỗ lẻ.

Vé HELD hết hạn tại đúng mốc heldAt + 5 phút được DELETE có điều kiện status/user/showtime/mã vé phù hợp. Vé PAID bị loại khỏi truy vấn dọn/huỷ giữ ngay trong SQL. UNIQUE(showtime_id, seat_id) được giữ nguyên theo ADR-1/ADR-2.

Đặt mới và đổi ghế đóng trước giờ chiếu năm phút; lượt giữ hiện có được thanh toán đến hạn giữ hoặc giờ bắt đầu, tùy mốc nào đến trước. BookingClock dùng Asia/Ho_Chi_Minh độc lập múi giờ máy chạy.

Biểu mẫu thanh toán gửi mã vé; đơn MoMo chứa mã vé nhỏ nhất của lượt giữ. Khi callback về, máy chủ kiểm tra lại mã lượt giữ và tổng tiền trong giao dịch. QR cũ không trả cho ghế mới dù cùng giá; giao dịch thành công nhưng không xuất được vé đi qua luồng hoàn tiền. Yêu cầu tự hoàn dùng requestId cố định theo transId để thử lại an toàn.

API state đọc lại Showtime sau dọn HELD, vì bulk DELETE xoá persistence context. BookingStateIntegrationTest kiểm tra request đầu tiên của phim mới, cả 5 phân loại tuổi, mã cũ, lỗi phân loại, phim ngừng chiếu và suất đã bắt đầu. Form quản trị phim bắt buộc phân loại hợp lệ.

## Database cloud

Ứng dụng giữ profile cloud và thông tin kết nối trong tệp secrets bị Git bỏ qua. Không thêm cột hoặc bảng: mã vé hiện có đủ nhận diện từng lượt giữ. Đã kiểm tra kết nối và schema với Hibernate validate. Không đưa mật khẩu vào mã nguồn, tài liệu hoặc log.

## Kiểm thử

Dùng JDK 21:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn "-Dtest=SeatPricingServiceTest,SeatSelectionPolicyTest,SeatServiceTest,SeatBookingServiceTest,SeatHoldServiceTest,BookingControllerTest,SeatMapViewTest,BookingSafetyTest,MomoHoldIdentityTest" test
node --test src/test/js/seat-booking.test.cjs
```

Mọi integration test kế thừa IntegrationTestBase và dùng TestDataFactory. Database phải kết thúc bằng _test; lớp cha xoá dữ liệu trước mỗi test. Không chạy mvn test với URL cloud của nhóm.

BookingSafetyIntegrationTest kiểm tra trên SQL Server thực: hai nhóm khác ghế không cùng tạo chỗ lẻ, huỷ cạnh tranh thanh toán, rollback khi INSERT đổi ghế lỗi và tab cũ không huỷ ghế mới. MoMo API trong test là bản giả; không gửi email hoặc giao dịch thật.

Ngày 01/10/2026: đã chạy các test tranh chấp trên SQL Server 2025 LocalDB, database riêng cinema_booking_module2_test; kết nối JDBC qua cầu nối loopback tạm. Cấu hình này chỉ dùng trong tiến trình kiểm thử, không đổi cấu hình cloud của ứng dụng. Kết quả đầy đủ ghi trong docs/MODULE_2_REVIEW.md.

## Nguồn tham khảo

Quy định tối đa tám chỗ, thời gian giữ/đóng bán và ghế lẻ trong dự án được ghi rõ theo lựa chọn của nhóm; không khẳng định mọi rạp đều áp dụng cùng một chính sách.

- [CGV — Câu hỏi thường gặp](https://www.cgv.vn/default/faq/): luồng đặt vé, hạn giữ và phân loại độ tuổi.
- [Lotte Cinema — Điều khoản sử dụng](https://www.lottecinemavn.com/LCHS/Contents/etc/terms-of-use.aspx).
- [Galaxy — Hỏi đáp](https://www.galaxycine.vn/hoi-dap/).
- [MoMo — Idempotency](https://developers.momo.vn/v3/docs/payment/api/result-handling/idempotency/).
