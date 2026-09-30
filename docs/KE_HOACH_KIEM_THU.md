# Kế hoạch kiểm thử — UTE Cinema

Tài liệu này mô tả nhóm kiểm thử phần mềm thế nào, kiểm thử những gì, và vì sao chọn cách đó.
Số liệu trong đây lấy từ lần chạy `mvn test` thật, không phải ước lượng.

**Hiện trạng: 211 test, 211 đạt, 0 hỏng.**

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
| Unit test | Kiểm tra logic tính toán và kiểm tra dữ liệu đầu vào | Không | 52 |
| Test MVC | Kiểm tra Controller, template và mã HTTP trả về | Không | 7 |
| Test tích hợp | Kiểm tra nghiệp vụ chạy thật trên database | Có | 152 |

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

### Module 2 — Ghế và vé (61 test)

| Lớp test | Số test | Kiểm tra điều gì |
|---|---|---|
| `SeatBookingServiceTest` | 12 | Kiểm tra dữ liệu đầu vào khi giữ ghế |
| `SeatServiceTest` | 11 | Dựng sơ đồ ghế kèm trạng thái từng ghế |
| `SeatPricingServiceTest` | 11 | Tính giá theo loại ghế |
| `SeatSelectionPolicyTest` | 7 | Quy tắc chọn ghế: tối đa 8 chỗ, không để ghế lẻ; **chọn khác hàng, cách nhau vẫn được** (từ 28/09) |
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

### Tính năng bổ sung ngày 27/09 — đợt một (35 test)

| Lớp test | Số test | Kiểm tra điều gì |
|---|---|---|
| `AccountSettingsIntegrationTest` | 9 | Sửa hồ sơ, đổi mật khẩu, **không in lại mật khẩu vừa gõ vào HTML** khi form báo lỗi |
| `TicketLookupServiceIntegrationTest` | 8 | Soát vé: cho vào, chưa thanh toán, sai ngày, đã hết suất, tra theo email |
| `MovieSearchIntegrationTest` | 6 | Tìm phim không dấu, lọc theo thể loại, mở bán lại phim đã ngừng chiếu |
| `StaffAccessIntegrationTest` | 6 | **Phân quyền khu vực nhân viên** và trang quản lý người dùng |
| `UserManagementIntegrationTest` | 6 | Cấp vai trò, **chặn tự đổi vai trò của chính mình** |

Test soát vé truyền "bây giờ" vào tay thay vì đọc đồng hồ máy, nên chạy lúc 23 giờ 59 hay lúc
nào cũng ra cùng một kết quả — kiểm tra "đúng ngày chiếu" mà dựa vào giờ thật thì sớm muộn cũng
có ngày test đỏ không rõ lý do.

### Tính năng bổ sung ngày 27/09 — đợt hai (38 test)

| Lớp test | Số test | Kiểm tra điều gì |
|---|---|---|
| `TicketRefundIntegrationTest` | 9 | Hoàn 100% / 50% / không huỷ theo giờ, **ghế trống lại cho người khác đặt**, MoMo từ chối thì vé còn nguyên, thống kê trừ tiền hoàn |
| `PasswordResetIntegrationTest` | 9 | Link đặt lại mật khẩu: hết hạn, **chỉ dùng một lần**, sửa link là hỏng, **không dò được email** |
| `MomoPaymentIntegrationTest` | 8 | **Chữ ký giả bị từ chối**, tải lại trang kết quả không xử lý trùng, ghế hết hạn giữ thì tự hoàn tiền |
| `CheckInIntegrationTest` | 5 | Cho vào phòng, **một vé không dùng được hai lần**, khách không tự bấm cho vào được |
| `MailDeliveryTest` | 4 | Công tắc tắt email, bỏ qua tên miền thử nghiệm, lỗi SMTP không làm hỏng giao dịch |
| `AdminFormErrorIntegrationTest` | 3 | Lỗi nghiệp vụ ở khu quản trị hiện ngay trên form, không đá sang trang lỗi |

### Bổ sung ngày 28/09 — loại ghế, loại phòng, thanh toán QR (12 test)

| Lớp test | Số test | Kiểm tra điều gì |
|---|---|---|
| `MomoPaymentIntegrationTest` (thêm) | 5 | Tạo giao dịch QR đúng tiền và hạn trả, MoMo báo đã trả thì xuất vé, chưa quét thì chờ, **không hỏi được giao dịch của người khác**, đi trọn luồng web tới trang hoàn tất |
| `SeatMapViewTest` | 4 | Gom ghế theo hàng, chú thích loại ghế kèm giá của suất, nhận ra loại phòng từ tên |
| `QrCodeServiceTest` | 3 | **Mã QR vẽ ra đọc ngược lại được đúng dữ liệu MoMo trả về**, SVG không ghi mã màu |

`QrCodeServiceTest` không chỉ kiểm tra "có ra thẻ `<svg>`": nó dựng lại ảnh từ SVG rồi cho bộ
đọc QR của ZXing đọc, nên mã vẽ sai một ô là test đỏ ngay.

**Không test nào gọi MoMo hay Gmail thật.** Lớp gọi HTTP của MoMo (`MomoApiClient`) được thay
bằng bản giả ngay trong `IntegrationTestBase`, và profile test đặt `app.mail.enabled=false`.
Chữ ký MoMo thì vẫn ký thật bằng khoá test, để chứng minh được chữ ký giả bị từ chối.

---

## 4. Ma trận: quy tắc nghiệp vụ ↔ test bảo vệ nó

| Quy tắc nghiệp vụ | Test bảo vệ |
|---|---|
| Hai người không thể đặt trùng một ghế | `SeatBookingConcurrencyIntegrationTest` |
| Hết giờ giữ ghế thì ghế phải trống lại cho người khác đặt | `SeatHoldServiceIntegrationTest` |
| Một phòng không thể chiếu hai phim cùng lúc | `ShowtimeServiceIntegrationTest` |
| Mật khẩu không bao giờ lưu dạng thô | `AuthServiceIntegrationTest` |
| Khách hàng không vào được khu vực quản trị | `AdminAccessIntegrationTest` |
| Chỉ nhân viên và quản trị viên soát được vé | `StaffAccessIntegrationTest` |
| Vé chưa thanh toán, sai ngày hoặc hết suất thì không cho vào | `TicketLookupServiceIntegrationTest` |
| Đổi mật khẩu phải nhập đúng mật khẩu hiện tại | `AccountSettingsIntegrationTest` |
| Quản trị viên không tự khoá mình ra khỏi hệ thống | `UserManagementIntegrationTest` |
| Kết quả thanh toán MoMo phải đúng chữ ký mới được ghi nhận | `MomoPaymentIntegrationTest` |
| Tiền đã trừ mà không xuất được vé thì phải tự hoàn | `MomoPaymentIntegrationTest` |
| Chỉ chủ giao dịch mới hỏi được trạng thái thanh toán QR | `MomoPaymentIntegrationTest` |
| Khách tự chọn hàng và vị trí, chỉ cấm để trống một ghế lẻ | `SeatSelectionPolicyTest` |
| Mã QR thanh toán mã hoá đúng dữ liệu MoMo trả về | `QrCodeServiceTest` |
| Hoàn tiền đúng chính sách theo số giờ còn lại tới suất chiếu | `TicketRefundIntegrationTest` |
| Vé đã vào phòng thì không huỷ được và không dùng lại được | `TicketRefundIntegrationTest`, `CheckInIntegrationTest` |
| Link đặt lại mật khẩu dùng một lần và có hạn | `PasswordResetIntegrationTest` |
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
- **Gửi email thật và gọi MoMo thật.** Bộ test tự động cố ý không chạm tới hai dịch vụ này.
  Đã thử tay ngày 27/09, kết quả ở mục 9.
- **MoMo gọi thẳng về máy chủ (IPN).** Chạy trên `localhost` thì MoMo không gọi tới được; trang
  kết quả (MoMo đưa khách quay về) lo việc xác nhận. Đường IPN đã viết và có chung logic kiểm chữ
  ký, nhưng chỉ chạy thật được khi đưa ứng dụng lên máy chủ có tên miền.
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

- **Chịu tải.** Vẫn nằm ngoài mục tiêu của đồ án.
- **Quét mã QR tới lúc trả tiền xong.** Cần điện thoại cài app MoMo Test (xem mục 10).

---

## 9. Kết quả thử tay với MoMo và Gmail ngày 27/09

Chạy trên máy cá nhân, dùng môi trường thử của MoMo (`test-payment.momo.vn`) và Gmail thật.

| Việc thử | Kết quả |
|---|---|
| Giữ 2 ghế, bấm "Thanh toán bằng ví MoMo" | Sang trang MoMo đúng mã đơn `UTE-<suất>-<khách>-…` và đúng số tiền |
| Bấm "Quay về", huỷ giao dịch bên MoMo | Quay lại app, **chữ ký thật của MoMo được chấp nhận**, báo lý do, ghế vẫn giữ |
| Trả bằng thẻ quốc tế thử | Vé chuyển sang "Đã thanh toán — Trả qua Ví MoMo", lưu mã giao dịch MoMo |
| Huỷ một vé còn hơn 24 giờ tới suất | Gọi API hoàn tiền thật của MoMo, nhận mã hoàn; ghế trống lại trên sơ đồ |
| Nhân viên soát vé, bấm "Cho vào phòng" | Soát lại cùng mã thì báo "ĐÃ VÀO PHÒNG", không có nút cho vào nữa |
| Quên mật khẩu với email thật | Gmail nhận thư "Đặt lại mật khẩu" |
| Thư tới tài khoản mẫu `@utecinema.local` | Bỏ qua đúng thiết kế, ghi log, không để Gmail trả thư lỗi |

**Thẻ thử của MoMo.** Tài liệu MoMo ghi thẻ quốc tế `4111 1111 1111 1111`, tên `NGUYEN VAN A`,
CVC `111`, hạn `05/26`. Hạn đó đã qua nên MoMo từ chối (mã 1002 — bị ngân hàng phát hành từ
chối). Nhập hạn còn hiệu lực, ví dụ `12/30`, thì thanh toán thành công. Thẻ ATM thử
`9704 0000 0000 0018` cũng bị từ chối với cùng mã lỗi trong lần thử này.

## 10. Kết quả thử tay ngày 28/09 — loại ghế, loại phòng, thanh toán QR

Chạy bản mới ở cổng riêng trên máy cá nhân, database `cinema_booking`, tài khoản khách mẫu, MoMo
môi trường thử. Suất thử: "Bóng Ma Nhà Hát", Cinema 3, và một suất Gold Class.

| Việc thử | Kết quả |
|---|---|
| Mở sơ đồ ghế phòng thường | Chữ cái hàng ở hai đầu; hàng H–I ghế VIP tím có dấu sao, hàng J ghế đôi hồng có dấu tim; ghế đã bán vẫn xám |
| Chú thích loại ghế | Ghi đúng giá của suất: thường 115.000 đ, VIP 172.500 đ, đôi 230.000 đ (2 người) |
| Chọn A5, A6, C7 và ghế đôi J8 | Hợp lệ, đếm 5/8 chỗ, tổng 575.000 đ |
| Chọn A5 và A7 (chừa đúng A6) | Bị chặn: "Lựa chọn này sẽ để ghế A6 trống một mình" |
| Giữ C7, E10, H3 (ba hàng khác nhau) | Máy chủ nhận, tổng 402.500 đ |
| Bấm "Quét mã QR bằng app MoMo" | Tạo giao dịch thật trên MoMo thử, trang của rạp hiện mã QR đen trên nền trắng (cả ở chế độ tối), đúng tiền, đếm ngược theo giờ giữ ghế |
| Để yên trang QR | Cứ 3 giây hỏi MoMo một lần, MoMo trả mã 1000 (đang chờ khách xác nhận) |
| Bấm "Tôi đã thanh toán" khi chưa trả | Báo "MoMo chưa báo nhận được tiền", vẫn ở trang QR |
| Phòng Gold Class | Nhãn vàng GOLD CLASS, chú thích chỉ có ghế VIP 300.000 đ |
| Lịch chiếu, chi tiết phim, bảng giá | Nhãn màu theo loại phòng; suất Gold/Premium có vạch màu bên trái |
| Khổ 375px | Không tràn ngang; sơ đồ cuộn trong khung riêng, ghế 32×32px; mã QR 280×280px |
| Tương phản màu | Chữ trên ghế VIP/đôi từ 13:1 trở lên, dấu sao/tim từ 4.5:1, viền từ 5:1, ở cả hai chế độ |

**Quét mã ở môi trường thử.** Mã QR của môi trường thử chỉ app **MoMo Test** mới quét được
(tải ở trang tải về dành cho nhà phát triển của MoMo, phải gỡ app MoMo thật trước khi cài). Tài
khoản ví thử dùng mật khẩu và OTP `000000`. Chưa cài được thì chọn "Thẻ ATM hoặc thẻ quốc tế qua
MoMo" và dùng thẻ thử ở mục 9.
