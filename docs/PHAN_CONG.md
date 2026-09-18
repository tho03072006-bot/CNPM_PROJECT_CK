# Bảng phân công chi tiết — Nhóm 05

Mỗi dòng trong bảng dưới đây đã được tạo thành một **Issue trên GitHub**. Vào
[tab Issues](https://github.com/tho03072006-bot/CNPM_PROJECT_CK_NHOM5/issues) lọc theo nhãn
`module-1` / `module-2` / `module-3` / `module-4` để thấy việc của mình, trong đó có mô tả đầy
đủ và tiêu chí "xong là khi nào".

**Làm mấy việc gắn nhãn `uu-tien-cao` trước** — chúng đang chặn việc của người khác.

Ước lượng tính bằng giờ làm thật, không tính thời gian ngồi nghĩ. Cột **Cần có trước** ghi
mã công việc phải xong trước thì mới làm được việc này. Thấy mô tả chưa đúng hoặc ước lượng
lệch thì báo Thọ, sửa sớm đỡ tốn công.

---

## Module 1 — Phim / Phòng chiếu / Suất chiếu (Tài)

| Mã | Công việc | Cần có trước | Ước lượng | Tuần |
|---|---|---|---|---|
| M1.1 | `MovieService`: `findActiveMovies()`, `findById()`, `createMovie()`, `updateMovie()`, `deactivateMovie()` | M4.2 | 3h | 1 |
| M1.2 | Trang danh sách phim công khai `/movies` — dùng `.movie-card` có sẵn | M1.1 | 2h | 1 |
| M1.3 | Trang chi tiết phim `/movies/{id}` kèm danh sách suất chiếu sắp tới | M1.1, M1.8 | 3h | 2 |
| M1.4 | Trang quản trị danh sách phim `/admin/movies` — dùng `.admin-table` có sẵn | M1.1, M3.3 | 2h | 2 |
| M1.5 | Form thêm/sửa phim + kiểm tra dữ liệu nhập (tên không rỗng, thời lượng > 0) | M1.4 | 3h | 2 |
| M1.6 | `RoomService` + quản trị phòng chiếu | M4.2 | 3h | 2 |
| M1.7 | Tự sinh ghế khi tạo phòng (theo số hàng × số cột, 2 hàng cuối là VIP) | M1.6 | 2h | 2 |
| M1.8 | `ShowtimeService` + quản trị suất chiếu | M1.1, M1.6 | 3h | 2 |
| M1.9 | **Chặn xếp 2 suất chiếu trùng giờ trong cùng một phòng** (tính cả thời gian dọn phòng) | M1.8 | 2h | 2 |
| M1.10 | Unit test cho M1.9: các trường hợp trùng đầu, trùng cuối, lồng nhau, sát nút | M1.9 | 2h | 3 |

## Module 2 — Ghế & Vé, lõi đặt vé (Thắng)

| Mã | Công việc | Cần có trước | Ước lượng | Tuần |
|---|---|---|---|---|
| M2.1 | `SeatService`: lấy sơ đồ ghế của một suất chiếu kèm trạng thái từng ghế (trống / đang giữ / đã bán) | M1.7, M1.8 | 3h | 2 |
| M2.2 | Trang chọn ghế `/booking/showtime/{id}` — dùng `.seat-map` và `.screen` có sẵn | M2.1 | 3h | 2 |
| M2.3 | **`SeatBookingService.holdSeats()` — mấu chốt của ADR-1.** Bắt `DataIntegrityViolationException` và ném `SeatAlreadyTakenException` | M2.1, M4.5 | 4h | 2 |
| M2.4 | API giữ ghế gọi bằng AJAX, trả JSON `{success, message}` | M2.3 | 2h | 3 |
| M2.5 | Đồng hồ đếm ngược thời gian giữ ghế trên giao diện (5 phút) | M2.4 | 2h | 3 |
| M2.6 | Xử lý vé quá hạn giữ: `HELD` quá `SEAT_HOLD_MINUTES` phút thì chuyển `EXPIRED` và trả ghế về trạng thái trống | M2.3 | 3h | 3 |
| M2.7 | Cho người dùng tự huỷ giữ ghế trước khi thanh toán | M2.3 | 2h | 3 |
| M2.8 | Tính tiền theo loại ghế: `NORMAL` giá gốc, `VIP` +50%, `COUPLE` ×2 | M2.1 | 2h | 3 |
| M2.9 | Test tranh chấp ở tầng Service: 2 request cùng gọi `holdSeats()` thì đúng 1 thành công, request kia nhận `SeatAlreadyTakenException` chứ không phải lỗi 500 | M2.3 | 2h | 3 |

## Module 3 — Người dùng, thanh toán, email, thống kê (Thanh)

| Mã | Công việc | Cần có trước | Ước lượng | Tuần |
|---|---|---|---|---|
| M3.1 | `AuthService`: đăng ký tài khoản, **mật khẩu hash bằng BCrypt** (dữ liệu mẫu hiện đang để mật khẩu thô, phải thay) | M4.2 | 3h | 1 |
| M3.2 | Đăng nhập / đăng xuất bằng session, dùng hằng số `Constants.SESSION_USER` | M3.1 | 3h | 1 |
| M3.3 | Chặn truy cập: tài khoản `CUSTOMER` không vào được trang `/admin/**` | M3.2 | 2h | 2 |
| M3.4 | Trang hồ sơ cá nhân + lịch sử vé đã đặt | M3.2, M2.3 | 3h | 3 |
| M3.5 | `PaymentService`: xác nhận thanh toán, chuyển vé `HELD` sang `PAID` và ghi `paid_at` | M2.3 | 3h | 3 |
| M3.6 | Trang xác nhận thanh toán — dùng `.ticket` có sẵn | M3.5 | 3h | 3 |
| M3.7 | Gửi email xác nhận vé sau khi thanh toán thành công (Gmail App Password) | M3.5 | 3h | 3 |
| M3.8 | Trang thống kê cho quản trị: doanh thu theo ngày, phim bán chạy | M3.5, M3.3 | 4h | 4 |
| M3.9 | Test phân quyền: khách hàng gọi thẳng URL trang admin phải bị chặn | M3.3 | 2h | 3 |

## Module 4 — Kiến trúc dùng chung, kiểm thử, quản lý (Thọ)

Tóm tắt các việc mà 3 bạn kia **phải chờ**, nên Thọ làm sớm:

| Mã | Công việc | Trạng thái |
|---|---|---|
| M4.1 | Nền móng dự án, schema, entity, repository | Xong |
| M4.2 | Hạ tầng test dùng chung (`IntegrationTestBase`, `TestDataFactory`) + database test riêng | Xong |
| M4.3 | Test chống đặt trùng ghế (ADR-1) | Xong |
| M4.4 | Design system + hai chế độ sáng/tối + trang `/ui-kit` | Xong |
| M4.5 | Bộ exception nghiệp vụ + `GlobalExceptionHandler` | Xong |
| M4.6 | Dữ liệu mẫu dùng chung `seed-data.sql` | Xong |
| M4.7 | CI tự động chạy test trên mỗi Pull Request | Xong, đã chạy xanh trên PR #1 |
| M4.8 | Database dùng chung trên cloud (MonsterASP.NET) | Xong, đã nạp schema + dữ liệu mẫu |
| M4.9 | Test luồng đặt vé end-to-end | Chờ M1, M2, M3 |
| M4.10 | Tài liệu kiểm thử + báo cáo tổng hợp | Tuần 4 |

---

## Trước khi bắt đầu code — làm đủ 4 bước này

1. **Đọc `README.md`**, mục *Môi trường phát triển*. Cài đúng JDK 21, Maven 3.9, SQL Server 2022
   trở lên. Chạy 3 lệnh kiểm tra ở cuối mục đó, xanh hết mới bắt đầu.
2. **Đọc `CONTRIBUTING.md`**, đặc biệt **Mục 5 (xử lý lỗi)** và **Mục 6 (viết test)** — hai mục
   này quyết định code của bạn có merge được hay không.
3. **Chạy ứng dụng rồi mở `http://localhost:8082/ui-kit`** xem sẵn bộ giao diện dùng chung.
   Cần nút, bảng, form, sơ đồ ghế thì chép class ở đó về, đừng tự viết CSS riêng.
4. **Xin Thọ thông tin kết nối database dùng chung** trong nhóm chat.

### Về database dùng chung trên cloud

Thông tin kết nối (server, tên database, user, mật khẩu) **cố ý KHÔNG nằm trong repo** — repo
này để public, đưa mật khẩu lên là ai cũng đọc được. Thọ gửi riêng trong nhóm chat, mỗi người
tự điền vào file `application-secrets-cloud.properties` trên máy mình (file này đã nằm trong
`.gitignore`).

Nhớ hai điều:

- **Cloud chỉ dùng để tích hợp và demo.** Code hằng ngày vẫn chạy SQL Server trên máy mình cho
  nhanh — datacenter cloud đặt ở châu Âu nên chậm hơn đáng kể.
- **Không tự sửa schema trên cloud.** Profile `cloud` đặt `ddl-auto=validate` nên Hibernate
  không được tự đổi bảng. Cần thêm/sửa bảng thì báo Thọ.

---

## Timeline 4 tuần

| Tuần | Mục tiêu | Mốc kiểm tra |
|---|---|---|
| 1 | Nền móng + đăng nhập + CRUD phim cơ bản | Chạy được ứng dụng, đăng nhập được, xem được danh sách phim |
| 2 | Quản trị đầy đủ + chọn ghế + giữ ghế | **Đặt được một vé từ đầu đến cuối** (dù chưa thanh toán) |
| 3 | Thanh toán + email + hết hạn giữ ghế + test | Luồng đặt vé hoàn chỉnh, test tự động chạy xanh |
| 4 | Thống kê + tích hợp + tài liệu + tập demo | Merge `develop` vào `main`, chạy thử trên database chung |

## Việc ai cũng phải làm, không chia cho riêng ai

- Review Pull Request của người khác (mỗi PR cần ít nhất 1 người duyệt).
- Viết mô tả PR tử tế, có ảnh chụp màn hình nếu đụng tới giao diện.
- Tự chạy `mvn test` trước khi mở PR, đừng để CI báo đỏ rồi mới sửa.
- Báo ngay trong nhóm chat khi cần sửa file thuộc module người khác.
