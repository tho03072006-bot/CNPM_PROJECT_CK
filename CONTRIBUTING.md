# Quy ước làm việc nhóm — NHÓM 8 (Đồ án CNPM: Hệ thống đặt vé xem phim)

Tài liệu này áp dụng cho cả 4 thành viên (Thọ, Tài, Thắng, Thanh). Mục tiêu: code của 4 người
nhìn vào giống như một người viết, tránh xung đột khi merge, và dễ chấm điểm theo đúng tính chất
môn Công nghệ phần mềm (đặt tên rõ ràng, cấu trúc mô-đun hoá, dễ bảo trì).

---

## 1. Quy ước đặt tên

### 1.1 Java

| Đối tượng | Quy tắc | Ví dụ |
|---|---|---|
| Class / Interface / Enum | PascalCase, danh từ | `MovieService`, `TicketRepository`, `TicketStatus` |
| Method | camelCase, động từ + bổ ngữ, rõ hành động | `findAvailableSeats()`, `holdSeat()`, `calculateTotalPrice()` |
| Biến / tham số | camelCase, danh từ rõ nghĩa, KHÔNG viết tắt khó hiểu | `showtimeId` (không dùng `stId`), `customerEmail` (không dùng `e`) |
| Hằng số | UPPER_SNAKE_CASE | `SEAT_HOLD_MINUTES`, `ROLE_ADMIN` |
| Package | chữ thường, không dấu, số nhiều cho tầng chứa nhiều class cùng loại | `entity`, `repository`, `service`, `controller`, `constants` |
| Boolean | tiền tố `is` / `has` / `can` | `isActive`, `hasDiscount`, `canCancel()` |

**Không dùng Lombok** — viết tay getter/setter/constructor, đúng cách nhóm đã quen ở môn
Lập trình Web.

**Viết tiếng Việt CÓ DẤU** ở mọi chỗ người đọc: chữ trên giao diện, thông báo lỗi, chú thích
code, tài liệu. Riêng định danh trong code (tên class, biến, method) vẫn dùng tiếng Anh.
File phải lưu UTF-8 — Maven đã cấu hình sẵn, chỉ cần IDE đừng lưu sang bảng mã khác.

### 1.2 Entity & Database

- Tên bảng: số nhiều, snake_case — `movies`, `showtimes`, `tickets`.
- Tên cột khoá ngoại: `<tên_bảng_số_ít>_id` — `movie_id`, `room_id`, `seat_id`.
- Cột trạng thái: dùng enum String (`status NVARCHAR(20)`), giá trị VIẾT HOA — `HELD`, `PAID`.
- Text tiếng Việt: **luôn `NVARCHAR`, không dùng `VARCHAR`**. Dùng `VARCHAR` là tiếng Việt
  biến thành dấu hỏi, và lỗi này chỉ lộ ra khi đã có dữ liệu thật.

### 1.3 Thymeleaf / Giao diện

- Tên file template: kebab-case, trùng với chức năng trang — `movie-list.html`, `seat-map.html`,
  `payment-confirm.html`.
- Class CSS: kebab-case. **Bộ giao diện dùng chung đã có sẵn** trong `static/css/style.css` —
  chạy ứng dụng rồi mở `http://localhost:8082/ui-kit` để xem tất cả thành phần đã có (nút, nhãn,
  thẻ phim, vé, form, bảng, sơ đồ ghế, thông báo). **Cần cái nào thì chép class ở đó về dùng,
  đừng tự viết CSS riêng cho module mình.**
- Thêm class MỚI thì nối vào **cuối** `style.css`, không sửa class người khác đang dùng.
- Chỉ dùng biến CSS trong `:root`, không gõ thẳng mã màu — nhờ vậy chế độ sáng/tối mới chạy được.
- Biến Thymeleaf trong model: camelCase, trùng tên với field của entity/DTO tương ứng.

### 1.4 Git — nhánh & commit

**Mô hình nhánh của nhóm (đã chốt):** mỗi người có một nhánh cá nhân mang tên mình, code và
commit trên đó; xong việc thì gộp vào `develop`; `main` chỉ chứa code ổn định.

| Nhánh | Ai dùng | Dùng để làm gì |
|---|---|---|
| `main` | cả nhóm | Code ổn định, nhận code từ `develop` khi đã chạy được |
| `develop` | cả nhóm | Nhánh gộp chung của 4 module |
| `Minh_Thọ` | Thọ | Module 4 — kiến trúc dùng chung, testing, tài liệu |
| `Hữu_Tài` | Tài | Module 1 — phim / phòng chiếu / suất chiếu |
| `Hữu_Thắng` | Thắng | Module 2 — ghế & vé, seat-map, giữ ghế |
| `Tuấn_Thanh` | Thanh | Module 3 — user/auth, thanh toán, email, thống kê |

Trước khi bắt đầu việc mới, nhớ kéo `develop` về nhánh của mình cho khỏi lệch:

```bash
git switch <nhánh-của-bạn> && git merge origin/develop
```

Nếu một tính năng làm dài ngày (ví dụ sửa lại cả luồng đặt vé) thì có thể tách thêm nhánh phụ
từ nhánh cá nhân, đặt tên `feature/<module>-<mô-tả-ngắn>` (ví dụ `feature/module2-seat-map`).
Không đặt tên chung chung như `feature/fix` hay `test1`.

**Commit message:** `<loại>(<module>): <mô tả ngắn>`

| Loại | Dùng khi |
|---|---|
| `feat` | thêm tính năng mới |
| `fix` | sửa lỗi |
| `refactor` | sửa cấu trúc code, không đổi hành vi |
| `docs` | tài liệu (README, CONTRIBUTING, chú thích) |
| `test` | thêm/sửa test |
| `chore` | việc lặt vặt (cấu hình, dependency, gitignore...) |

Ví dụ: `feat(module2): thêm API giữ ghế theo suất chiếu`,
`fix(module1): sửa lỗi validate trùng giờ chiếu`.

**Mỗi commit chỉ làm MỘT việc** — đừng gom "sửa 3 bug + thêm 2 tính năng" vào một commit, sẽ
rất khó review và không revert được khi cần.

**Commit sớm, commit thường xuyên.** Trước khi đổi nhánh luôn chạy `git status` xem còn gì chưa
commit — ngày 17/09 nhóm đã mất một đợt công việc vì đổi nhánh lúc còn file chưa commit.

---

## 2. Cấu trúc thư mục

```
src/main/java/edu/hcmute/cnpm/cinema/
  entity/       — các lớp @Entity ánh xạ bảng database (KHÔNG chứa logic nghiệp vụ)
  repository/   — interface extends JpaRepository (chỉ khai báo truy vấn)
  service/      — logic nghiệp vụ (tính tiền, giữ ghế, gửi email...)
                   MỖI THÀNH VIÊN TỰ TẠO service riêng cho module mình:
                   MovieService, SeatBookingService, PaymentService, AuthService
  controller/   — @Controller Spring MVC, chỉ nhận request và trả view
  exception/    — exception nghiệp vụ + GlobalExceptionHandler dùng chung
  constants/    — hằng số dùng chung
src/main/resources/
  templates/
    layout/     — layout dùng chung
    fragments/  — header / footer / alert dùng chung
    error.html  — trang báo lỗi chung
    <module>/   — MỖI MODULE tạo một thư mục con riêng:
                   templates/movie/, templates/booking/, templates/account/
  static/css/style.css — design system dùng chung, thêm class mới vào cuối file
  static/js/theme.js   — chuyển chế độ sáng/tối, dùng chung
src/test/java/edu/hcmute/cnpm/cinema/
  support/      — IntegrationTestBase + TestDataFactory dùng chung
  <module>/     — test của từng module
database/       — script SQL (Thọ gộp lại từ đề xuất của từng người)
docs/           — kế hoạch, phân công, ADR
```

**Nguyên tắc quan trọng:** Controller gọi Service, Service gọi Repository. **Không được** để
Controller gọi thẳng Repository hay chứa logic tính toán / điều kiện nghiệp vụ — đúng kiến trúc
phân lớp đã học ở Chương 2 và đã áp dụng ở môn Lập trình Web.

---

## 3. Quy trình Pull Request

1. Code trên nhánh cá nhân của bạn (xem bảng ở Mục 1.4). Trước khi bắt đầu, kéo `develop` về
   nhánh mình để không làm trên bản cũ: `git merge origin/develop`.
2. Code xong, tự kiểm tra lại: đọc lại diff một lượt và chạy `mvn test` cho chắc.
3. Đưa code lên `develop`. Nhánh `develop` **không khoá**, nên chọn cách nào tiện nhất cho bạn:
   push thẳng từ nhánh cá nhân, hoặc mở Pull Request nếu muốn người khác xem qua trước.
4. **Không phải chờ ai duyệt.** Mở Pull Request thì tự bấm merge được ngay, không đợi Thọ hay
   bất kỳ ai. Nhờ review khi bạn thấy cần, đó không phải thủ tục bắt buộc.
5. Tự rà lại bảng kiểm này trước khi đưa code lên `develop`:
   - [ ] `mvn test` chạy xanh
   - [ ] Không có logic nghiệp vụ trong Controller
   - [ ] Có xử lý lỗi, không để lỗi 500 thô ra màn hình
   - [ ] Không hardcode chuỗi kết nối database / mật khẩu / API key
   - [ ] Đặt tên biến, hàm, class đúng quy ước ở Mục 1
   - [ ] Giao diện dùng class có sẵn trong `style.css`, không tự viết CSS riêng
   - [ ] Sửa file dùng chung hoặc file của module khác thì nhắn nhóm một câu cho mọi người biết
6. Nhánh cá nhân thì **giữ lại** (dùng suốt kỳ đồ án). Chỉ xoá các nhánh phụ `feature/...` sau
   khi đã merge xong, để repo khỏi rối.
7. Cuối tuần 3 / đầu tuần 4: merge `develop` vào `main` sau khi cả 4 module đã tích hợp và chạy được.

CI trên GitHub Actions tự chạy toàn bộ test cho mỗi push và mỗi Pull Request. Đừng để CI báo đỏ
rồi mới sửa — chạy `mvn test` trên máy trước khi push.

---

## 4. Định nghĩa "Hoàn thành" cho một tính năng

Một tính năng được tính là xong khi:

- Chạy đúng như mô tả trong Issue tương ứng (mọi mục trong phần "Xong là khi nào" đều tích)
- Có ít nhất một unit test, hoặc test thủ công được ghi lại trong PR
- Không còn `System.out.println` debug sót lại
- Giao diện dùng đúng layout chung, không vỡ bố cục trên màn hình nhỏ, xem được ở **cả chế độ
  sáng và tối**
- Đã chạy thử thật trên máy, không chỉ đọc code thấy đúng

---

## 5. Xử lý lỗi — dùng chung, không tự chế

Cả nhóm dùng chung bộ exception trong `edu.hcmute.cnpm.cinema.exception` và bộ bắt lỗi tập trung
`GlobalExceptionHandler`. **Không tự viết try/catch rồi tự render trang lỗi riêng** cho module mình.

Cách dùng — đặt ở tầng **Service**, không phải Controller:

```java
// Không tìm thấy dữ liệu -> tự động thành trang 404
Movie movie = movieRepository.findById(movieId)
        .orElseThrow(() -> new ResourceNotFoundException("phim", movieId));

// Nghiệp vụ không cho phép -> tự động thành trang 400
if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
    throw new InvalidBookingException("Suất chiếu này đã bắt đầu, bạn không thể đặt vé nữa.");
}

// Ghế bị người khác giữ mất -> tự động thành trang 409
try {
    ticketRepository.saveAndFlush(ticket);
} catch (DataIntegrityViolationException ex) {
    throw new SeatAlreadyTakenException(showtimeId, seatId, ex);
}
```

| Exception | Mã HTTP | Dùng khi nào |
|---|---|---|
| `ResourceNotFoundException` | 404 | Không tìm thấy phim / phòng / suất chiếu / vé theo id |
| `InvalidBookingException` | 400 | Yêu cầu sai nghiệp vụ (suất đã chiếu, vé hết hạn giữ, quá số ghế...) |
| `SeatAlreadyTakenException` | 409 | Ghế đã có người khác giữ — **bắt buộc dùng sau khi catch `DataIntegrityViolationException`** |
| `BusinessException` | 400 | Các lỗi nghiệp vụ khác chưa có lớp riêng |

Message truyền vào exception sẽ hiện **thẳng ra màn hình cho người dùng đọc**, nên viết câu hoàn
chỉnh, dễ hiểu — đừng ghi kiểu "err code 3" hay tên class.

Nếu request là AJAX (header `X-Requested-With: XMLHttpRequest`) thì handler tự trả JSON
`{"success": false, "message": "..."}` thay vì trang HTML — tiện cho màn hình chọn ghế.

Muốn báo thành công / báo lỗi nhẹ trên trang (không phải exception) thì dùng hai hằng số
`Constants.MODEL_SUCCESS_MESSAGE` / `Constants.MODEL_ERROR_MESSAGE` — fragment
`fragments/alert.html` đã nhúng sẵn trong layout sẽ tự hiển thị.

---

## 6. Quy ước viết test

- File test đặt trong `src/test/java/edu/hcmute/cnpm/cinema/<module>/`, tên kết thúc bằng
  `Test` (test đơn lẻ) hoặc `IntegrationTest` (test có dùng database).
- **Mọi test có dùng database phải `extends IntegrationTestBase`** (ở package `support`).
  Lớp cha này tự bật profile `test`, tự chặn test chạy nhầm vào database thật, và tự xoá sạch
  dữ liệu trước mỗi test case.
- Tạo dữ liệu mẫu bằng `TestDataFactory` (cùng ở package `support`) thay vì tự `new` entity
  trong từng file test. Cần kiểu dữ liệu mẫu mới thì **thêm method mới** vào đó, không sửa
  method người khác đang dùng.
- Tên method test: `should<KếtQuảMongĐợi>_when<TìnhHuống>()`, kèm `@DisplayName` mô tả bằng
  tiếng Việt cho dễ đọc báo cáo.
- Trước khi chạy test lần đầu:
  ```bash
  sqlcmd -S localhost,1433 -U sa -C -f 65001 -i database/create-test-database.sql
  ```
  Sau đó chạy `mvn test`.

---

## 7. Liên hệ / thắc mắc

Nếu không chắc quy ước áp dụng thế nào cho trường hợp cụ thể, **hỏi trước trong nhóm chat** thay
vì tự quyết định rồi phải sửa lại sau — đồ án chỉ có 4 tuần, sửa đi sửa lại rất tốn thời gian.

Đặc biệt phải hỏi trước khi: sửa file thuộc module người khác, sửa entity, đổi phiên bản trong
`pom.xml`, hoặc sửa các file dùng chung mà Thọ đang quản lý (layout, fragments, `style.css`,
`Constants.java`, package `exception`, package `support` của test).
