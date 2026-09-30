# CNPM_PROJECT_CK — Hệ thống đặt vé xem phim

[![Kiểm thử](https://github.com/tho03072006-bot/CNPM_PROJECT_CK/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/tho03072006-bot/CNPM_PROJECT_CK/actions/workflows/ci.yml)

Đồ án cuối kỳ môn Công nghệ phần mềm (CNPM) — Nhóm 8.

Người dùng chọn phim, chọn suất chiếu, chọn ghế trên sơ đồ trực quan rồi thanh toán. Ghế được
giữ riêng trong 5 phút để không ai đặt trùng.

---

## Tài khoản dùng thử

Đăng nhập được ngay, không phải đăng ký. Có sẵn trên **database dùng chung (cloud)** và trên
máy cá nhân sau khi chạy `database/seed-data.sql`. Mật khẩu chung: **`123456`**.

| Email | Mật khẩu | Vai trò | Nên thử gì |
|---|---|---|---|
| `khachhang@utecinema.local` | `123456` | Khách hàng | Chọn suất → chọn ghế → thanh toán (QR MoMo, thẻ hoặc tại quầy) → **Vé của tôi** → huỷ vé xem hoàn tiền |
| `nhanvien@utecinema.local` | `123456` | Nhân viên | Menu **Soát vé**: nhập mã vé (số `#` trên thẻ vé ở trang Vé của tôi) hoặc email khách, bấm "Cho vào phòng" |
| `admin@utecinema.local` | `123456` | Quản trị | Menu **Quản trị**: thêm/sửa phim, phòng, suất chiếu (thử thêm suất trùng giờ), cấp vai trò ở `/admin/users`, xem thống kê |

Cách thử nhanh cả vòng: đăng nhập tài khoản khách, đặt vé và trả **tại quầy** (xác nhận ngay,
không cần MoMo) → đăng xuất → đăng nhập tài khoản nhân viên, soát đúng mã vé đó → đăng nhập
quản trị xem thống kê doanh thu.

**Thanh toán MoMo (môi trường thử, tiền không bị trừ thật).**

- **Quét mã QR:** chỉ app **MoMo Test** quét được (tải tại developers.momo.vn/v3/download, phải
  gỡ app MoMo thật trước khi cài). Ví thử dùng mật khẩu và OTP `000000`.
- **Thẻ quốc tế:** bấm "Thẻ ATM hoặc thẻ quốc tế qua MoMo", nhập thẻ `4111 1111 1111 1111`, tên
  `NGUYEN VAN A`, hạn `12/30`, CVC `111`. Thẻ trong tài liệu MoMo ghi hạn `05/26` đã quá hạn nên
  bị từ chối.
- Máy nào chưa có khoá MoMo trong `application-secrets.properties` thì trang thanh toán chỉ còn
  nút trả tại quầy (xem mục [Cách chạy dự án](#cách-chạy-dự-án-máy-cá-nhân)).

**Email.** Tên miền `.local` không có thật nên ba tài khoản trên **không nhận thư** (hệ thống bỏ
qua, không báo lỗi). Muốn thử quên mật khẩu hay thư xác nhận vé thì đăng ký thêm một tài khoản
bằng email thật của mình.

Tài khoản đăng ký mới luôn là khách hàng. Cần thêm tài khoản nhân viên thì đăng nhập quản trị,
vào `/admin/users` cấp vai trò.

---

## Vai trò và phân quyền

Hệ thống có bốn nhóm người dùng. Quyền được chặn ở phía máy chủ (`AdminAccessInterceptor`,
`StaffAccessInterceptor` và từng Controller), không phải chỉ ẩn nút trên giao diện: gõ thẳng
địa chỉ vẫn bị chặn.

| Vai trò | Làm được gì | Bị chặn ở đâu |
|---|---|---|
| **Khách chưa đăng nhập** | Xem phim, tìm và lọc phim, lịch chiếu, bảng giá, xem sơ đồ ghế; đăng ký, đăng nhập, quên mật khẩu | Giữ ghế, thanh toán, tài khoản → chuyển sang trang đăng nhập |
| **Khách hàng** (`CUSTOMER`) | Thêm: giữ ghế, thanh toán (quét mã QR MoMo, thẻ qua MoMo hoặc tại quầy), vé của tôi, huỷ vé có hoàn tiền, sửa hồ sơ, đổi mật khẩu | `/nhan-vien/**`, `/admin/**` → 403 |
| **Nhân viên** (`STAFF`) | Như khách hàng, thêm trang **Soát vé**: tra vé theo mã hoặc email, cho khách vào phòng | `/admin/**` → 403 |
| **Quản trị** (`ADMIN`) | Tất cả những việc trên, thêm khu **Quản trị**: phim, phòng chiếu, suất chiếu, người dùng (cấp vai trò), thống kê doanh thu | — |

Tài khoản mới đăng ký luôn là khách hàng. Chỉ quản trị viên cấp được vai trò nhân viên hoặc
quản trị, ở trang `/admin/users`.

Tài khoản tạo sẵn để thử từng vai trò: xem mục [Tài khoản dùng thử](#tài-khoản-dùng-thử) ở đầu trang.

## Tính năng chính

- **Đặt vé:** chọn suất → chọn ghế trên sơ đồ → giữ ghế 5 phút → thanh toán. Sơ đồ phân biệt
  ghế thường, ghế VIP (tím, dấu sao) và ghế đôi (hồng, dấu tim), chú thích ghi giá thật của suất.
  Khách tự chọn hàng và vị trí; chỉ giữ hai luật như các rạp lớn: tối đa 8 chỗ một lượt (ghế đôi
  tính 2 chỗ) và không để trống đúng một ghế lẻ. Hai người không thể đặt trùng một ghế (ADR-1).
  Tải lại trang vẫn còn nguyên lượt giữ ghế và đồng hồ; mỗi khách chỉ giữ một lượt cho một suất;
  đặt vé trực tuyến đóng trước giờ chiếu 5 phút.
- **Loại phòng:** Phòng thường, Premium, Gold Class có nhãn màu riêng ở lịch chiếu, chi tiết phim,
  sơ đồ ghế và bảng giá. Loại phòng suy ra từ tên phòng, không thêm cột database.
- **Thanh toán:** ưu tiên quét mã QR MoMo ngay trên trang của rạp (trang tự nhận kết quả, không
  phải nhập thẻ); ngoài ra có thẻ ATM/thẻ quốc tế qua trang MoMo, hoặc trả tại quầy. MoMo đang
  chạy môi trường thử, tiền không đi thật; quét mã QR thử cần app **MoMo Test** của MoMo.
- **Huỷ vé và hoàn tiền:** còn từ 24 giờ trước giờ chiếu hoàn 100%, từ 2 tới dưới 24 giờ hoàn
  50%, dưới 2 giờ hoặc đã vào phòng thì không huỷ. Vé trả qua MoMo được hoàn thẳng về MoMo.
- **Email:** xác nhận vé, xác nhận huỷ vé, link đặt lại mật khẩu (qua Gmail).
- **Soát vé:** nhân viên tra mã vé, ghi giờ vào phòng; một vé không dùng được hai lần.
- **Quản trị:** thêm/sửa/ngừng chiếu/mở bán lại phim, phòng chiếu tự sinh ghế, xếp suất chiếu
  có chặn trùng giờ, cấp vai trò người dùng, thống kê doanh thu đã trừ tiền hoàn.

## Thành viên & phân công

| Thành viên | Vai trò | Module phụ trách |
|---|---|---|
| **Thọ** | Leader | Module 4: Kiến trúc dùng chung, Testing, Quản lý GitHub/PM |
| **Tài** | Thành viên | Module 1: Quản lý Phim / Phòng chiếu / Suất chiếu (Admin CRUD) |
| **Thắng** | Thành viên | Module 2: Ghế & Vé — đặt vé, seat-map, giữ ghế (lõi đặt vé) |
| **Thanh** | Thành viên | Module 3: User/Auth, thanh toán, email, thống kê |

Công việc chi tiết của từng người: [`docs/PHAN_CONG.md`](docs/PHAN_CONG.md) — 33 đầu việc kèm
mã việc, phụ thuộc, ước lượng giờ và tuần thực hiện.

**Việc cụ thể của bạn nằm ở [tab Issues](https://github.com/tho03072006-bot/CNPM_PROJECT_CK/issues)**,
lọc theo nhãn `module-1` / `module-2` / `module-3` / `module-4`. Làm mấy việc gắn nhãn
`uu-tien-cao` trước, vì chúng đang chặn việc của người khác.

## Công nghệ sử dụng

- Java 21, Spring Boot 3.5.16
- Thymeleaf + thymeleaf-layout-dialect 4.0.1
- Spring Data JPA / Hibernate 6
- SQL Server (mssql-jdbc 13.6.0.jre11)
- Spring Mail (gửi email xác nhận vé)
- Maven

Không dùng Lombok — viết tay getter/setter/constructor. Không dùng thư viện CSS ngoài
(Bootstrap, Tailwind...) — giao diện tự viết, xem mục [Giao diện dùng chung](#giao-diện-dùng-chung).

## Môi trường phát triển — cả 4 người cài giống nhau

Lệch phiên bản là nguồn gốc của kiểu lỗi *"máy tao chạy được mà máy mày không chạy được"*,
rất tốn thời gian dò. Dùng **đúng** các phiên bản dưới đây:

| Thành phần | Phiên bản chốt | Ghi chú |
|---|---|---|
| JDK | **21** (Temurin 21.0.12) | `mvn -v` phải báo `Java version: 21.x`. Báo 1.8 hay 17 là sai |
| Maven | 3.9.x | Bản đã thử: 3.9.16 |
| Spring Boot | 3.5.16 | Khoá trong `pom.xml`, không tự ý nâng |
| Hibernate ORM | 6.6.53.Final | Đi kèm Spring Boot, không khai báo riêng |
| Tomcat nhúng | 10.1.55 | Đi kèm Spring Boot |
| thymeleaf-layout-dialect | 4.0.1 | Khoá trong `pom.xml` |
| mssql-jdbc | 13.6.0.jre11 | Khoá trong `pom.xml` |
| SQL Server | 2022 Express trở lên | Bản đã thử: SQL Server 2022 (16.0.1000.6) Express |
| Cổng ứng dụng | 8082 | Đổi trong `application.properties` nếu máy bạn đã dùng cổng này |

**Không ai được tự nâng phiên bản trong `pom.xml`.** Cần nâng thì báo cả nhóm trước, vì nâng
một cái kéo theo cả chuỗi phụ thuộc và có thể làm đổ test của người khác.

### Kiểm tra máy trước khi bắt đầu

Chạy 3 lệnh này, xanh hết mới bắt đầu code:

```bash
mvn -v
```

```bash
sqlcmd -S localhost,1433 -U sa -C -Q "SELECT @@VERSION"
```

```bash
mvn test
```

Kết quả mong đợi: `Apache Maven 3.9.x` + `Java version: 21.x` · kết nối được SQL Server ·
`BUILD SUCCESS`.

### Ba cái bẫy hay gặp khi cài

**JAVA_HOME trỏ nhầm.** `mvn -v` báo Java 1.8 hay 17 thì đổi biến môi trường `JAVA_HOME` sang
thư mục JDK 21 rồi **mở lại terminal**. Spring Boot 3.5 không build được bằng Java 8/11.

**Bản Express vẫn dùng `localhost,1433`**, không cần gõ `\SQLEXPRESS`. Kiểm tra instance nào
đang chạy: `sqlcmd -S localhost,1433 -U sa -C -Q "SELECT @@SERVERNAME"`.

**Đặt thư mục dự án ở đường dẫn KHÔNG CÓ DẤU tiếng Việt** — ví dụ `D:\CNPM\Project_CK`.
Nếu đường dẫn có dấu, `mvn spring-boot:run` sẽ báo `Could not find or load main class`, vì JVM
trên Windows tiếng Việt dùng bảng mã Cp1252 nên đọc sai đường dẫn khi tạo tiến trình con.
Bẫy ở chỗ **`mvn test` vẫn chạy bình thường**, rất dễ tưởng là lỗi code.

## Cấu trúc thư mục

```
src/main/java/edu/hcmute/cnpm/cinema/
  entity/       — các lớp @Entity ánh xạ bảng database (KHÔNG chứa logic nghiệp vụ)
  repository/   — interface extends JpaRepository
  service/      — logic nghiệp vụ, mỗi người tự tạo service cho module của mình
  controller/   — @Controller Spring MVC, chỉ nhận request và trả view
  exception/    — exception nghiệp vụ + GlobalExceptionHandler dùng chung
  constants/    — hằng số dùng chung
src/main/resources/
  templates/
    layout/     — layout chung
    fragments/  — header / footer / alert dùng chung
    error.html  — trang báo lỗi chung
    <module>/   — mỗi module một thư mục con cho trang của mình
  static/css/style.css — design system dùng chung
  static/js/theme.js   — chuyển chế độ sáng/tối
src/test/java/edu/hcmute/cnpm/cinema/
  support/      — IntegrationTestBase + TestDataFactory dùng chung
  <module>/     — test của từng module
database/
  schema.sql              — tạo database trên máy cá nhân
  schema-cloud.sql        — tạo bảng trên database dùng chung
  create-test-database.sql— tạo database riêng cho test
  seed-data.sql           — dữ liệu mẫu dùng chung
docs/                     — kế hoạch, phân công, ADR
```

**Nguyên tắc quan trọng:** Controller gọi Service, Service gọi Repository. Controller **không
được** gọi thẳng Repository hay chứa logic tính toán/điều kiện nghiệp vụ. Chi tiết trong
[`CONTRIBUTING.md`](CONTRIBUTING.md) Mục 2.

## Cách chạy dự án (máy cá nhân)

1. Cài SQL Server, tạo database bằng `database/schema.sql`.
2. Nạp dữ liệu mẫu để cả nhóm test trên cùng một bộ dữ liệu:
   ```bash
   sqlcmd -S localhost,1433 -U sa -C -f 65001 -d cinema_booking -i database/seed-data.sql
   ```
3. Copy file cấu hình bí mật rồi điền thông tin thật:
   ```bash
   cp application-secrets.properties.example application-secrets.properties
   ```
   Bắt buộc điền `spring.datasource.url` / `username` / `password`. Các mục còn lại để trống
   thì ứng dụng vẫn chạy, chỉ tắt đúng tính năng đó:
   - `spring.mail.username` / `spring.mail.password` (Gmail App Password) — thiếu thì không gửi thư.
   - `momo.partner-code` / `access-key` / `secret-key` (bộ khoá thử của MoMo) — thiếu thì trang
     thanh toán chỉ còn "Trả tiền mặt tại quầy".
   - `app.password-reset.secret` — thiếu thì mỗi lần khởi động lại, link đặt lại mật khẩu cũ hết hiệu lực.

   File này **đã nằm trong `.gitignore`, tuyệt đối không commit**.
4. Chạy:
   ```bash
   mvn spring-boot:run
   ```
   Mở `http://localhost:8082`.

   Thư mục dự án có dấu tiếng Việt (ví dụ `Công nghệ phần mềm`) thì lệnh trên và nút Run của
   Eclipse đều báo `Could not find or load main class`. Khi đó: trong Eclipse bật
   *Run Configurations → Dependencies → Use temporary JAR to specify classpath*, hoặc chạy
   `mvn -DskipTests package` rồi `java -jar target/cinema-booking-0.0.1-SNAPSHOT.jar`.

## Cách chạy test

Test tích hợp chạy trên **database riêng** `cinema_booking_test`, không dùng chung với
`cinema_booking` — vì test **xoá sạch dữ liệu trước mỗi test case**.

Tạo database test, chỉ cần làm một lần:

```bash
sqlcmd -S localhost,1433 -U sa -C -f 65001 -i database/create-test-database.sql
```

Bảng và cột bên trong do Hibernate tự sinh từ các `@Entity`, không cần chạy `schema.sql`.
Sau đó chạy:

```bash
mvn test
```

Mọi test đụng database **phải kế thừa `IntegrationTestBase`** và tạo dữ liệu mẫu bằng
`TestDataFactory` — xem `src/test/java/edu/hcmute/cnpm/cinema/support/` và Mục 6 của
[`CONTRIBUTING.md`](CONTRIBUTING.md).

CI trên GitHub Actions tự chạy toàn bộ test mỗi khi có push hoặc mở Pull Request, dùng
container SQL Server riêng. Đừng để CI báo đỏ rồi mới sửa — chạy `mvn test` trên máy trước.

## Database dùng chung của nhóm (cloud)

Ngoài database trên máy cá nhân, nhóm dùng thêm một database MSSQL chung trên
**MonsterASP.NET** (gói Free) để tích hợp và demo. Chi tiết đầy đủ về cả ba database:
[`docs/DATABASE.md`](docs/DATABASE.md).

**Mô hình 2 tầng — nhớ cho kỹ:**

- **Code hằng ngày** dùng SQL Server trên máy mình: nhanh, không cần mạng.
- **Cloud** chỉ dùng lúc tích hợp và demo. Datacenter đặt ở châu Âu nên mỗi truy vấn chậm hơn
  local khoảng 250–300ms, khởi động ứng dụng mất ~10 giây thay vì ~4 giây.

**Cài đặt một lần trên máy bạn:**

1. Xin Thọ thông tin kết nối trong nhóm chat (server, tên database, user, mật khẩu).
   Thông tin này **cố ý không nằm trong repo**, đừng mất công tìm.
2. ```bash
   cp application-secrets-cloud.properties.example application-secrets-cloud.properties
   ```
3. Điền 4 dòng `cloud.db.*`. **Giữ nguyên dấu `#` ở dòng `cloud.db.options`** — đó là cấu hình
   riêng của Azure, bỏ dấu `#` ra là dính lỗi TLS `PKIX path building failed`.
4. ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=cloud
   ```

**Lưu ý về schema trên cloud:** profile `cloud` đặt `spring.jpa.hibernate.ddl-auto=validate`,
nghĩa là Hibernate **không được tự sửa** schema chung. Muốn thêm/sửa bảng thì sửa
`database/schema-cloud.sql`, chạy tay lên cloud, rồi báo cả nhóm — tránh cảnh 4 người cùng sửa
làm schema chung biến dạng.

**Trước buổi demo phải chạy thử trước ít nhất một ngày.** Gói Free ghi rõ *"no guarantees"*,
không có cam kết thời gian hoạt động. Nên **demo từ máy cá nhân** cho chắc, cloud chỉ mở ra để
chứng minh nhóm có database dùng chung thật. Phương án dự phòng luôn sẵn: `schema-cloud.sql` +
`seed-data.sql` dựng lại toàn bộ database trong 30 giây ở bất kỳ đâu.

## Giao diện dùng chung

Giao diện lấy tông màu từ **logo trường**: xanh dương + trắng, thêm vàng hổ phách cho ra chất
rạp chiếu phim. Có **hai chế độ sáng và tối**, người dùng bấm nút ở góc phải header để đổi và
hệ thống nhớ lựa chọn đó.

Chạy ứng dụng rồi mở **`http://localhost:8082/ui-kit`** để xem sẵn toàn bộ thành phần giao diện:
nút, nhãn trạng thái, thẻ phim, vé, form, bảng dữ liệu, sơ đồ ghế, thông báo. **Cần cái nào thì
chép class ở đó về dùng, đừng tự viết CSS riêng cho module mình** — để giao diện 4 người làm ra
nhìn như một người làm.

Ba quy tắc không được phá:

- Chỉ dùng biến CSS trong `:root`, không gõ thẳng mã màu vào từng class.
- Mọi animation phải tắt được khi người dùng bật chế độ giảm chuyển động của hệ điều hành.
- Thêm class mới thì **nối vào cuối** `style.css`, không sửa class người khác đang dùng.

## Xử lý lỗi — dùng chung, không tự chế

Cả nhóm dùng chung bộ exception trong `edu.hcmute.cnpm.cinema.exception`. **Không tự viết
try/catch rồi tự render trang lỗi riêng.** Tầng Service cứ ném exception, đã có
`GlobalExceptionHandler` lo phần đổi sang mã HTTP và hiển thị:

| Exception | Mã HTTP | Dùng khi nào |
|---|---|---|
| `ResourceNotFoundException` | 404 | Không tìm thấy phim / phòng / suất chiếu / vé theo id |
| `InvalidBookingException` | 400 | Yêu cầu sai nghiệp vụ (suất đã chiếu, vé hết hạn giữ...) |
| `SeatAlreadyTakenException` | 409 | Ghế đã có người khác giữ — **bắt buộc dùng** sau khi catch `DataIntegrityViolationException` |
| `BusinessException` | 400 | Các lỗi nghiệp vụ khác |

Cách dùng cụ thể: Mục 5 của [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Quy ước GitHub

- Nhánh `main`: code ổn định, nhận code từ `develop` khi đã chạy được.
- Nhánh `develop`: nhánh gộp chung của 4 module.
- Mỗi thành viên code trên nhánh cá nhân mang tên mình (`Minh_Thọ`, `Hữu_Tài`, `Hữu_Thắng`,
  `Tuấn_Thanh`), xong việc thì đưa lên `develop` — push thẳng hoặc mở Pull Request, tuỳ bạn.
- Tính năng làm dài ngày thì tách thêm nhánh phụ `feature/<module>-<mô-tả-ngắn>`
  (ví dụ `feature/module1-movie-crud`) từ nhánh cá nhân.
- Commit theo dạng `<loại>(<module>): <mô tả ngắn>` với loại là `feat` / `fix` / `refactor` /
  `docs` / `test` / `chore`. **Mỗi commit chỉ làm một việc.**
- `develop` không khoá: ai cũng push và merge được, không phải chờ người khác duyệt.
  `main` chỉ chặn force-push và chặn xoá nhánh, để bản nộp không bị lỡ tay làm mất.
- **Commit sớm, commit thường xuyên.** Trước khi đổi nhánh luôn chạy `git status` xem còn gì
  chưa commit.

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Quy ước đặt tên, cấu trúc, xử lý lỗi, viết test, quy trình Pull Request |
| [`docs/PHAN_CONG.md`](docs/PHAN_CONG.md) | Bảng phân công 33 đầu việc + timeline 4 tuần |
| [`docs/DATABASE.md`](docs/DATABASE.md) | Ba database của dự án, cách dùng từng cái, các lỗi hay gặp |
| [`docs/KE_HOACH_KIEM_THU.md`](docs/KE_HOACH_KIEM_THU.md) | Kiểm thử những gì, bằng cách nào, và quy tắc nghiệp vụ nào được test nào bảo vệ |
