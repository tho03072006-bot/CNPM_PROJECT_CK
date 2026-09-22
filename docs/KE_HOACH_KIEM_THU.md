# Kế hoạch kiểm thử — UTE Cinema

Tài liệu này mô tả nhóm kiểm thử phần mềm thế nào, kiểm thử những gì, và vì sao chọn cách đó.
Số liệu trong đây lấy từ lần chạy `mvn test` thật, không phải ước lượng.

**Hiện trạng: 125 test, 125 đạt, 0 hỏng.**

---

## 1. Nguyên tắc nhóm thống nhất

**Kiểm thử trên SQL Server thật, không dùng database giả lập trong bộ nhớ.** Lý do nằm ở chính
chỗ khó nhất của đề tài: chống hai người đặt trùng một ghế. Cơ chế chặn là ràng buộc
`UNIQUE (showtime_id, seat_id)` cùng cơ chế khoá dòng của SQL Server. Nếu thay bằng H2 hay
database trong bộ nhớ thì test vẫn báo xanh, nhưng nó không còn chứng minh được điều cần chứng
minh — mà đó lại là phần quan trọng nhất của cả bài.

**Test không được làm bẩn dữ liệu của nhau.** Mọi test đụng database đều kế thừa
`IntegrationTestBase`. Lớp cha này lo ba việc: bật profile `test`, kiểm tra tên database trước
mỗi lần chạy, và xoá sạch dữ liệu trước từng test case.

**Chốt an toàn chống chạy nhầm database.** `IntegrationTestBase` kiểm tra tên database phải kết
thúc bằng `_test` trước khi làm bất cứ việc gì. Nếu ai đó sửa nhầm cấu hình làm test trỏ vào
`cinema_booking` thì test dừng lại ngay thay vì xoá mất dữ liệu của cả nhóm.

**Tên test nói rõ đang kiểm tra điều gì.** Quy ước `should<KếtQuảMongĐợi>_when<TìnhHuống>()`.
Đọc tên test là biết nó bảo vệ quy tắc nghiệp vụ nào, không cần mở code ra xem.

---

## 2. Ba tầng kiểm thử

| Tầng | Dùng khi | Cần database? | Số test |
|---|---|---|---|
| Unit test | Kiểm tra logic tính toán và kiểm tra dữ liệu đầu vào | Không | 40 |
| Test MVC | Kiểm tra Controller, template và mã HTTP trả về | Không | 7 |
| Test tích hợp | Kiểm tra nghiệp vụ chạy thật trên database | Có | 78 |

Unit test chạy trong mili giây nên viết được nhiều và chạy liên tục lúc code. Test tích hợp chậm
hơn nhưng là thứ duy nhất chứng minh được ràng buộc database có hoạt động thật hay không.

---

## 3. Bảng kiểm thử theo module

### Module 1 — Phim, phòng chiếu, suất chiếu (26 test)

| Lớp test | Số test | Kiểm tra điều gì |
|---|---|---|
| `MoviePagesIntegrationTest` | 9 | Trang phim công khai và trang quản trị hiển thị đúng dữ liệu |
| `ShowtimeServiceIntegrationTest` | 7 | **Chặn xếp hai suất trùng giờ trong cùng một phòng** |
| `MovieServiceIntegrationTest` | 6 | Thêm, sửa, ngừng chiếu phim |
| `RoomServiceIntegrationTest` | 4 | Tạo phòng và tự sinh ghế theo số hàng × cột |

Phần chặn trùng giờ chiếu được phủ đủ bốn kiểu chồng lấn: suất mới bắt đầu giữa suất cũ, kết thúc
giữa suất cũ, bao trọn suất cũ, và nằm gọn trong suất cũ. Cộng thêm ca sát nút (bắt đầu đúng sau
15 phút dọn phòng thì phải cho qua) và ca hai phòng khác nhau chiếu cùng giờ thì không được chặn.

### Module 2 — Ghế và vé (60 test)

| Lớp test | Số test | Kiểm tra điều gì |
|---|---|---|
| `SeatBookingServiceTest` | 12 | Kiểm tra dữ liệu đầu vào khi giữ ghế |
| `SeatServiceTest` | 11 | Dựng sơ đồ ghế kèm trạng thái từng ghế |
| `SeatPricingServiceTest` | 11 | Tính giá theo loại ghế |
| `SeatSelectionPolicyTest` | 6 | Quy tắc chọn ghế: tối đa 8 chỗ, cùng hàng, liền nhau, không để ghế lẻ |
| `BookingControllerTest` | 7 | API giữ ghế: chỉ nhận JSON, lấy danh tính từ session |
| `SeatHoldServiceIntegrationTest` | 6 | **Trả ghế về trạng thái trống** khi hết hạn hoặc khách huỷ |
| `SeatBookingConcurrencyIntegrationTest` | 4 | **Chống đặt trùng ghế — ADR-1** |
| `SeatBookingServiceIntegrationTest` | 3 | Giữ nhiều ghế trong một giao dịch |

**Hai test quan trọng nhất của cả đồ án nằm ở đây.**

`SeatBookingConcurrencyIntegrationTest` cho 10 luồng cùng lúc tranh nhau một ghế. Đúng một luồng
được phép ghi, chín luồng còn lại phải nhận `SeatAlreadyTakenException` chứ không phải lỗi 500.
Test này chứng minh ràng buộc `UNIQUE` thật sự chặn được tranh chấp, không phải chỉ nhờ may mắn
về thứ tự chạy.

`SeatHoldServiceIntegrationTest` chứng minh ADR-2 làm đúng: sau khi vé quá hạn bị dọn, **người
khác đặt lại đúng ghế đó phải thành công**. Nếu ai đó sửa code thành "chỉ đổi trạng thái sang
`EXPIRED`" thay vì xoá dòng thì test này đỏ ngay, vì ràng buộc `UNIQUE` không nhìn cột `status`.

### Module 3 — Người dùng, thanh toán, thống kê (21 test)

| Lớp test | Số test | Kiểm tra điều gì |
|---|---|---|
| `AuthServiceIntegrationTest` | 7 | Đăng ký và đăng nhập, **mật khẩu phải được băm** |
| `AdminAccessIntegrationTest` | 6 | **Phân quyền khu vực quản trị** |
| `PaymentServiceIntegrationTest` | 5 | Xác nhận thanh toán, chặn vé quá hạn |
| `StatsServiceIntegrationTest` | 3 | Thống kê doanh thu và phim bán chạy |

Phần mật khẩu kiểm tra ba điều: cột `password_hash` không chứa mật khẩu thô, hai người dùng cùng
mật khẩu vẫn ra hai chuỗi băm khác nhau (BCrypt tự sinh muối riêng), và sai mật khẩu với sai
email phải báo **cùng một câu** để người lạ không dò được email nào đã đăng ký.

Phần phân quyền gọi **thẳng địa chỉ** `/admin/**` bằng bốn tư cách: chưa đăng nhập, tài khoản
khách hàng, tài khoản nhân viên, tài khoản quản trị. Đây mới là cách kiểm tra đúng — ẩn nút trên
giao diện không phải là phân quyền.

### Module 4 — Nền tảng dùng chung (18 test)

| Lớp test | Số test | Kiểm tra điều gì |
|---|---|---|
| `TicketPricingServiceIntegrationTest` | 6 | Bảng giá vé khớp với giá thật trong lịch chiếu |
| `ScheduleServiceIntegrationTest` | 6 | Lịch chiếu theo ngày, gom theo loại phòng |
| `GlobalExceptionHandlerIntegrationTest` | 4 | Exception nghiệp vụ đổi đúng mã HTTP |
| `BookingFlowEndToEndTest` | 2 | **Luồng đặt vé từ đầu đến cuối** |

`BookingFlowEndToEndTest` là test chứng minh bốn module ghép lại chạy được, chứ không phải từng
module chạy riêng thì đúng. Nó đi hết đường mà một khách thật sẽ đi, mỗi bước gọi qua HTTP đúng
như trình duyệt gọi:

1. Tạo tài khoản (Module 3)
2. Mở trang lịch chiếu, thấy phim (Module 1)
3. Mở sơ đồ ghế rồi giữ 2 ghế (Module 2)
4. Vào trang thanh toán, xác nhận trả tiền (Module 3)
5. Kiểm tra vé đã chuyển sang `PAID` và có ghi thời điểm thanh toán
6. Mở trang vé của tôi, thấy vé
7. Mở lại sơ đồ ghế, hai ghế đó đã chuyển sang trạng thái đã bán

Test thứ hai kiểm tra chiều ngược lại: chưa đăng nhập thì không giữ ghế được, và **không được
tạo ra dòng vé nào** trong database.

---

## 4. Ma trận: quy tắc nghiệp vụ ↔ test bảo vệ nó

| Quy tắc nghiệp vụ | Test bảo vệ |
|---|---|
| Hai người không thể đặt trùng một ghế | `SeatBookingConcurrencyIntegrationTest` |
| Hết giờ giữ ghế thì ghế phải trống lại cho người khác đặt | `SeatHoldServiceIntegrationTest` |
| Một phòng không thể chiếu hai phim cùng lúc | `ShowtimeServiceIntegrationTest` |
| Mật khẩu không bao giờ lưu dạng thô | `AuthServiceIntegrationTest` |
| Khách hàng không vào được khu vực quản trị | `AdminAccessIntegrationTest` |
| Vé quá hạn giữ thì không thanh toán được | `PaymentServiceIntegrationTest` |
| Không trả tiền hộ vé của người khác | `PaymentServiceIntegrationTest` |
| Chưa đăng nhập thì không giữ ghế được | `BookingFlowEndToEndTest` |
| Giá ghế VIP và ghế đôi tính đúng hệ số | `SeatPricingServiceTest` |
| Bảng giá công bố khớp giá thật trong lịch chiếu | `TicketPricingServiceIntegrationTest` |
| Lỗi nghiệp vụ trả đúng mã HTTP, không lòi lỗi 500 | `GlobalExceptionHandlerIntegrationTest` |

---

## 5. Chạy test thế nào

Chuẩn bị một lần trên máy mỗi người:

```bash
sqlcmd -S localhost,1433 -U sa -C -f 65001 -i database/create-test-database.sql
```

Chạy toàn bộ:

```bash
mvn test
```

Chạy riêng phần không cần database, nhanh hơn nhiều lúc đang code:

```bash
mvn -Dtest=SeatPricingServiceTest,SeatServiceTest,SeatBookingServiceTest,BookingControllerTest test
```

**Nhớ đặt `JAVA_HOME` trỏ vào JDK 21 trước khi chạy Maven**, vì máy của nhóm đang để mặc định
Java 8 và Spring Boot 3.5 không build được bằng Java 8.

Nếu test báo lỗi `Connection refused` ở cổng 1433 thì SQL Server chưa bật. Mở PowerShell bằng
quyền quản trị rồi chạy `net start MSSQL$SQLEXPRESS`.

---

## 6. Kiểm thử tự động trên GitHub

Mỗi lần push hoặc mở Pull Request, GitHub Actions dựng một container SQL Server 2022 thật, tạo
database rỗng rồi chạy toàn bộ `mvn test`. Test đỏ thì hiện ngay trên Pull Request, kèm file báo
cáo chi tiết tải về được.

Có một loại lỗi mà **CI không bắt được**, cần biết để không mất thời gian: CI luôn tạo database
rỗng nên Hibernate sinh bảng đúng kiểu ngay từ đầu. Còn trên máy cá nhân, database test đã tồn
tại từ trước, mà `ddl-auto=update` thì không bao giờ đổi kiểu của cột đã có. Nên khi ai đó đổi
kiểu dữ liệu trong `@Entity`, CI vẫn xanh mà máy cá nhân đỏ hàng loạt. Cách xử lý ghi ở mục
"Năm thứ dễ sai" trong [`DATABASE.md`](DATABASE.md).

---

## 7. Những gì chưa kiểm thử tự động

Nói thẳng để không ai tưởng bộ test phủ hết mọi thứ:

- **Giao diện trên trình duyệt thật.** Nhóm kiểm tra bằng mắt: mở trang, thu nhỏ cửa sổ xuống khổ
  điện thoại, xem cả chế độ sáng lẫn tối, đi bằng phím Tab kiểm tra viền focus. Chưa có test tự
  động chụp màn hình so sánh.
- **Gửi email thật.** `TicketMailService` được viết để không gửi gì khi chưa cấu hình email, và
  lỗi SMTP không làm hỏng giao dịch đã thanh toán. Việc thư có tới hộp thư hay không thì phải thử
  tay.
- **Đồng hồ đếm ngược giữ ghế.** Phần JavaScript trên trang chọn ghế chưa có test tự động. Logic
  quan trọng nằm ở phía máy chủ và đã có test: vé quá hạn thì không thanh toán được.
- **Chịu tải.** Đồ án không đặt mục tiêu này. Test tranh chấp chỉ dùng 10 luồng, đủ để chứng minh
  tính đúng đắn chứ không phải đo hiệu năng.

---

## 8. Kết quả kiểm thử tay ngày 22/09

Mục 7 nói bộ test tự động không phủ được giao diện trên trình duyệt. Lần chạy tay này chứng
minh đó không phải lo xa: **ba lỗi lọt qua cả 125 test xanh lẫn CI xanh**, vì cả ba đều nằm ở
tầng cấu hình và CSS chứ không phải ở logic nghiệp vụ.

### Ba lỗi đã tìm ra và đã sửa

| Lỗi | Hậu quả nếu không sửa | Sửa ở đâu |
|---|---|---|
| Tomcat viết `;jsessionid=...` vào URL ở lần chuyển hướng đầu tiên, Spring Boot 3 không cắt path parameter nên không khớp route nào | **Đăng nhập xong ra thẳng trang 404** trên trình duyệt sạch — đúng tình huống buổi bảo vệ | `server.servlet.session.tracking-modes=cookie` |
| Luật `[hidden] { display: none }` của trình duyệt có độ ưu tiên (0,1,0), bị class `.seat-hold-timer { display: flex }` cùng độ ưu tiên đè lên | Mở trang chọn ghế là thấy ngay khung "Ghế đang được giữ cho bạn — 05:00" kèm nút Thanh toán trỏ `#`, trong khi chưa giữ ghế nào | Thêm `[hidden] { display: none !important; }` vào phần nền tảng của `style.css` |
| Chưa khai báo icon cho tab trình duyệt | Console báo lỗi 404 `/favicon.ico` trên mọi trang | Thêm `static/images/favicon.svg` và `<link rel="icon">` trong `layout/base.html` |

Bài học rút ra: **test xanh không thay được việc mở trình duyệt lên xem**. Hai lỗi đầu đều đủ
nghiêm trọng để hỏng buổi demo, mà không một test nào trong 125 test bắt được — test MVC chỉ
kiểm tra mã HTTP và tên template chứ không chạy CSS, còn test tích hợp thì gọi thẳng Service
chứ không đi qua vòng chuyển hướng của trình duyệt.

### Những gì đã kiểm tra tay

Chạy trên máy cá nhân (`localhost:8082`) và trên database dùng chung của nhóm
(`localhost:8083`, profile `cloud`). Cả hai đều đi hết luồng sau:

- Đăng nhập bằng `khachhang@utecinema.local` trên trình duyệt đã xoá cookie → vào thẳng trang chủ.
- Chọn hai ghế cách nhau một ghế → hiện cảnh báo sẽ để ghế ở giữa trống một mình, nút Giữ ghế bị khoá.
- Chọn hai ghế liền nhau → giữ ghế thành công, khung đếm ngược mới hiện ra và chạy thật, nút Thanh toán trỏ đúng suất chiếu.
- Thanh toán → vé chuyển sang ĐÃ THANH TOÁN kèm giờ thanh toán.
- Mở trang Vé của tôi → thấy đủ vé.
- Mở lại sơ đồ ghế → hai ghế vừa mua đã sang trạng thái đã bán.
- Gọi thẳng `/admin/**` khi chưa đăng nhập → trả 403, không lọt vào được.

Kiểm tra giao diện:

- Khổ điện thoại 375px: trang không tràn ngang, sơ đồ ghế cuộn ngang trong khung riêng, ghế 32×32px đúng mức vùng chạm tối thiểu.
- Chế độ sáng và chế độ tối: đều hiển thị đúng.
- Đi bằng phím Tab: viền focus rõ, `solid 2.86px #f0a828` kèm offset.
- Console trình duyệt: sạch, không còn lỗi nào.

### Vẫn chưa kiểm thử

- **Gửi email thật.** Chưa cấu hình SMTP nên `TicketMailService` không gửi gì. Muốn demo phần
  này thì phải điền `spring.mail.username` và App Password vào `application-secrets.properties`
  rồi thử gửi tay.
- **Chịu tải.** Vẫn nằm ngoài mục tiêu của đồ án.
