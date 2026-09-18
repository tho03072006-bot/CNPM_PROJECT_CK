# ADR-002: Database dùng chung trên cloud cho cả nhóm

- **Trạng thái:** ĐÃ CHỐT và **đã triển khai xong** (18/09/2026)
- **Ngày đề xuất:** 17/09/2026 · **Đổi nhà cung cấp:** 18/09/2026 (xem mục 2c)
- **Người đề xuất:** Thọ (Module 4 — Kiến trúc dùng chung)
- **Liên quan:** ADR-001 (chống đặt trùng ghế bằng UNIQUE (showtime_id, seat_id))

---

## 1. Vấn đề

Ban đầu mỗi thành viên chạy SQL Server riêng trên máy mình. Hậu quả:

- Dữ liệu 4 máy khác nhau: Tài thêm phim mới, Thắng không thấy phim đó để test seat-map.
- Không ai chắc schema của mình còn giống schema người khác — đến lúc merge mới vỡ ra.
- Lúc demo cho thầy phải chạy trên một máy cụ thể, máy đó hỏng là hỏng cả buổi demo.
- Báo cáo đồ án khó chứng minh "hệ thống nhiều người dùng cùng lúc" nếu chỉ có một máy.

Cần một database dùng chung, miễn phí (sinh viên không có thẻ tín dụng để trả phí).

## 2. Các phương án đã xét

| # | Phương án | Ưu điểm | Nhược điểm |
|---|---|---|---|
| 1 | **Azure SQL Database — gói free** | Vẫn là SQL Server thật, không phải sửa một dòng code nào | Cần tài khoản Azure; gói free có hạn mức tháng |
| 2 | Mỗi người giữ local + dùng chung `seed-data.sql` | Đơn giản nhất, không cần mạng | Vẫn không phải dữ liệu chung thật; vẫn lệch nhau khi ai đó sửa tay |
| 3 | Đổi sang PostgreSQL cloud (Neon / Supabase) | Hạn mức free rộng rãi hơn | **Phải đổi driver, dialect, kiểu dữ liệu, viết lại cả `schema.sql`** — quá tốn với đồ án 4 tuần |
| 4 | Hosting free có kèm MSSQL (Somee, MonsterASP...) | Đăng ký nhanh, không cần thẻ | Không có cam kết uptime, có thể giới hạn số kết nối đồng thời |

## 2b. So sánh dung lượng miễn phí (khảo sát 09/2026)

| Dịch vụ | Loại database | Dung lượng free | Dùng được cho dự án này? |
|---|---|---|---|
| Azure SQL Database (gói free) | SQL Server | 32 GB / database, tối đa 10 database | Được — dung lượng lớn nhất trong danh sách |
| CockroachDB Serverless | PostgreSQL-compatible | 10 GiB | Được về dung lượng nhưng phải đổi sang PostgreSQL |
| Aiven | PostgreSQL / MySQL | 1 GB (đã bị cắt từ 5 GB xuống) | Phải đổi sang PostgreSQL |
| Neon | PostgreSQL | 0,5 GB / project | Phải đổi sang PostgreSQL |
| Supabase | PostgreSQL | 500 MB | Phải đổi sang PostgreSQL |

**Kết luận lúc đó (17/09):** Azure SQL free vừa cho dung lượng cao nhất, vừa không phải đổi
driver hay schema.

> **Kết luận này đã bị thay thế ngày 18/09** vì Azure từ chối cho đăng ký — xem mục 2c.
> Phần so sánh bên trên vẫn giữ lại để sau này còn biết nhóm đã cân nhắc những gì.

Nói thẳng cho dễ hình dung: database của đồ án này (vài chục phim, vài phòng, vài nghìn vé)
**chưa tới 100 MB**. Dung lượng chưa bao giờ là thứ đáng lo — thứ đáng lo là độ ổn định và
việc có phải đổi công nghệ hay không.

## 2c. Đổi nhà cung cấp — Azure không đăng ký được (18/09/2026)

Ngày 18/09 Thọ đăng ký Azure thì bị từ chối:

> You're not eligible for an Azure free account

Nguyên nhân: lỗi này nói về **Azure free account** (gói dùng thử 200 USD), thường xảy ra khi
email trường chưa nằm trong danh sách trường Microsoft công nhận, hoặc khu vực Việt Nam chỉ
được xếp vào gói **Azure for Students Starter** — mà gói Starter thì không dùng được ưu đãi
SQL Database free.

Có một đường vòng: gói SQL Database free vẫn chạy trên mọi loại subscription, kể cả
Pay-As-You-Go. Nhưng Pay-As-You-Go bắt buộc gắn thẻ tín dụng, chỉ cần lỡ tay tạo thêm tài
nguyên khác là bị tính tiền thật. **Đã loại phương án này** — đồ án sinh viên không đáng mang
rủi ro đó.

Lúc đăng ký Azure có một giao dịch **1 USD tại Microsoft Store** hiện trên sao kê. Đó là khoản
giữ tạm để xác minh thẻ, không phải phí dịch vụ, và đã có thông báo **huỷ giao dịch** ngay
trong cùng một giây.

### Nhà cung cấp mới: MonsterASP.NET (gói Free)

| Tiêu chí | MonsterASP.NET | Somee.com | Neon (PostgreSQL) |
|---|---|---|---|
| Loại database | **MSSQL 2025** | MSSQL Express | PostgreSQL |
| Dung lượng free | **1 GB, 5 database** | 30 MB | 3 GiB |
| Cần thẻ tín dụng? | **Không** | Không | Không |
| Phải sửa code? | **Không** | Không | **Có, rất nhiều** |
| Datacenter | Châu Âu | Mỹ | Nhiều nơi |

Chọn MonsterASP vì **vẫn là SQL Server** nên không phải sửa một dòng code nào, 1 GB gấp hơn
10 lần nhu cầu thật, đăng ký không cần thẻ, và có hỗ trợ bật remote access để nối từ ứng dụng.

**Vì sao không chọn Neon dù dung lượng rộng hơn:** đổi sang PostgreSQL không chỉ là đổi driver.
Phải sửa dialect, viết lại `schema.sql` (`NVARCHAR` → `TEXT`, `DATETIME2` → `TIMESTAMP`,
`IDENTITY` → `GENERATED`), viết lại `seed-data.sql`, sửa `columnDefinition="NVARCHAR(MAX)"`
trong `Movie.java`, và sửa hai câu truy vấn riêng của SQL Server trong test. Tệ hơn nữa:
**cả 4 người sẽ phải gỡ SQL Server và cài PostgreSQL trên máy** — vì nếu máy cá nhân chạy
SQL Server còn cloud chạy PostgreSQL thì sẽ sinh ra đúng loại lỗi "chạy máy mình được, lên cloud
thì hỏng", rất khó truy. Giữa tuần thứ hai của đồ án 4 tuần, đổi động cơ database là quá mạo hiểm.

**Nếu MonsterASP chạy chập chờn** thì lùi về phương án 2 ở bảng mục 2 (mỗi người chạy local,
dùng chung `database/seed-data.sql` để dữ liệu giống nhau) chứ **không** đổi sang PostgreSQL.

## 3. Quyết định

**Chọn MonsterASP.NET gói Free**, dùng theo mô hình **2 tầng**:

```
+----------------------------+      +------------------------------------+
|  Máy cá nhân (4 người)     |      |  MonsterASP.NET (gói Free)         |
|  SQL Server local          |      |  MSSQL dùng chung, 1 GB            |
|  cinema_booking            |      |  ddl-auto = validate               |
|  ddl-auto = update         |      |  -> tích hợp, demo, dữ liệu thật   |
|  -> code hằng ngày, offline|      |                                    |
+----------------------------+      +------------------------------------+
         profile: (mặc định)                  profile: cloud
```

- **Hằng ngày mọi người vẫn code trên SQL Server local** — nhanh, không cần mạng.
- **Database cloud là nơi tích hợp và demo**, chạy bằng `-Dspring-boot.run.profiles=cloud`.
- Trên cloud đặt `spring.jpa.hibernate.ddl-auto=validate`: **Hibernate không được tự ý sửa
  schema chung.** Muốn đổi bảng/cột thì sửa `database/schema-cloud.sql`, chạy tay lên cloud,
  rồi báo cả nhóm — tránh cảnh 4 người cùng `update` làm schema chung biến dạng.
- **Test tích hợp KHÔNG chạy trên cloud**, vì test xoá sạch dữ liệu trước mỗi test case.

## 4. Giới hạn của gói Free MonsterASP (cần biết trước)

- **5 database, tổng 1 GB.** Dự án này chưa tới 100 MB nên thoải mái.
- **Không cần thẻ tín dụng**, không có ngày hết hạn.
- **Remote access mặc định bị TẮT**, phải vào từng database bật thủ công.
- Datacenter đặt ở **châu Âu** → độ trễ từ Việt Nam khoảng **250–300ms**. Khởi động ứng dụng
  mất ~10 giây thay vì ~4 giây khi chạy local. Dùng để tích hợp và demo thì chấp nhận được,
  nhưng **đừng lấy làm database code hằng ngày**.
- Gói Free ghi rõ **"no guarantees or warranties"**. Đây là chuyện bình thường: **không gói
  free nào của bất kỳ nhà cung cấp nào có cam kết uptime** — SLA chỉ đi kèm gói trả tiền, vì
  SLA nghĩa là nhà cung cấp phải đền tiền khi dịch vụ chết.
- Gói Free giới hạn số kết nối đồng thời (không công bố con số) → `application-cloud.properties`
  đã đặt `maximum-pool-size=5` cho mỗi máy, 4 người cùng chạy là khoảng 20 kết nối. Nếu bị từ
  chối kết nối thì hạ số này xuống.

**Vì không có cam kết uptime, quy định của nhóm là:**

1. **Hôm bảo vệ demo từ máy cá nhân.** Cloud chỉ mở ra chứng minh với thầy là có database dùng
   chung thật. Mạng trường chập hoặc datacenter trễ là mất điểm oan.
2. **Chạy thử trước buổi demo ít nhất một ngày.**
3. Phương án dự phòng luôn sẵn: `schema-cloud.sql` + `seed-data.sql` dựng lại toàn bộ database
   trong 30 giây ở bất kỳ đâu. Thứ duy nhất không được sao lưu là dữ liệu phát sinh khi dùng
   (vé đã đặt, tài khoản đăng ký thêm) — gần ngày bảo vệ thấy cần thì dùng mục **Backups**
   hoặc **Export** trong trang quản trị MonsterASP tải một bản về.

**Mẹo:** đóng SSMS khi không dùng. Gói free nào cũng giới hạn kết nối, để Object Explorer mở
liên tục là chiếm mất một kết nối của cả nhóm.

## 5. Các bước triển khai

1. **Thọ** đăng ký tài khoản tại `monsterasp.net`, chọn gói **Free** (xong 18/09).
2. Trong trang quản trị: **Databases → Create**, chọn loại **MSSQL** (không phải MySQL).
   Ghi lại tên database thật mà hệ thống sinh ra — thường **không** phải `cinema_booking` mà
   là một tên dạng `db_xxxxx`.
3. Mở database vừa tạo → mục **Users and remote** → bấm **Enabled** để bật remote access.
   Mặc định remote access bị TẮT, không bật thì ứng dụng không nối vào được.
4. Ngay tại màn hình đó, chép lại **server**, **login**, **password**.
5. Chạy `database/schema-cloud.sql` rồi `database/seed-data.sql` lên database đó.
6. Gửi cho 3 thành viên: server, tên database, user, password — mỗi người tự điền vào
   `application-secrets-cloud.properties` trên máy mình. **Không bỏ vào Git, không gửi kèm
   trong file code** (repo để public).

### Kết quả triển khai (18/09/2026)

Đã hoàn tất bước 1–5. Kết quả kiểm chứng thật:

| Kiểm tra | Kết quả |
|---|---|
| Kết nối tới database cloud | Được — cloud chạy SQL Server **2025** Express (máy cá nhân là 2022, không ảnh hưởng) |
| Chạy `schema-cloud.sql` | Tạo đủ 6 bảng kèm ràng buộc `uq_showtime_seat` của ADR-001 |
| Chạy `seed-data.sql` | 3 người dùng, 6 phim, 2 phòng, 120 ghế, 10 suất chiếu |
| Chạy ứng dụng với profile `cloud` | **`ddl-auto=validate` PASS** — schema khớp hoàn toàn với entity |
| Mở trang trên trình duyệt | Trang chủ và trang lỗi render đúng, đọc được dữ liệu từ cloud |
| Collation | `SQL_Latin1_General_CP1_CI_AS` — **trùng với máy cá nhân** nên không lệch. Tiếng Việt lưu, so sánh và sắp xếp đều đúng vì mọi cột chữ đều dùng `NVARCHAR` |

Thời gian khởi động: **10,5 giây** trên cloud so với **4,5 giây** ở local — đúng như dự đoán
về độ trễ datacenter châu Âu.

Một rủi ro đã được gỡ: trước đó lo `Showtime.basePrice` và `Ticket.price` khai báo
`DECIMAL(10,2)` trong schema nhưng Hibernate hiểu là `numeric(38,2)`, sợ chế độ `validate` sẽ
chặn không cho ứng dụng khởi động. **Đã thử thật: không chặn** — Hibernate 6 không kiểm tra
precision/scale của kiểu số.

Còn lại một việc duy nhất: **gửi thông tin kết nối cho 3 thành viên qua nhóm chat.**

## 6. Hệ quả

**Tích cực**

- Cả nhóm nhìn chung một bộ dữ liệu; demo chạy được từ bất kỳ máy nào.
- Không phải sửa code hay dependency: vẫn `mssql-jdbc`, vẫn `SQLServerDialect`.
- Có thể demo thật cảnh "2 máy cùng đặt một ghế" — đúng ý ADR-001.

**Tiêu cực / rủi ro**

- Phụ thuộc mạng; mất mạng là không code được trên profile cloud, nên vẫn giữ local làm chính.
- Không có cam kết uptime, có thể chậm hoặc gián đoạn bất kỳ lúc nào.
- Độ trễ 250–300ms làm mọi thao tác chậm hơn rõ rệt so với local.
- Mật khẩu database do nhà cung cấp sinh sẵn và 4 người dùng chung một tài khoản. Nếu lộ thì
  vào trang quản trị bấm **Change** đổi mật khẩu, rồi báo cả nhóm sửa lại file secret trên máy.

**Nguồn tham khảo**

- [Try Azure SQL Database for Free — Microsoft Learn](https://learn.microsoft.com/en-us/azure/azure-sql/database/free-offer?view=azuresql)
- [Azure SQL Database free offer FAQ](https://learn.microsoft.com/en-us/azure/azure-sql/database/free-offer-faq?view=azuresql)
- [MonsterASP.NET — gói hosting miễn phí](https://www.monsterasp.net/)
- [MonsterASP — Bật remote access cho database](https://help.monsterasp.net/books/databases/page/remote-access-for-database)
- [MonsterASP — Kết nối bằng SQL Server Management Studio](https://help.monsterasp.net/books/databases/page/sql-server-management-studio-ssms)
- [The Best Free Database Tiers in 2026 (15 Compared) — FreeTier.co](https://freetier.co/articles/best-free-database-free-tiers-2026)
- [Top PostgreSQL Database Free Tiers in 2026 — Koyeb](https://www.koyeb.com/blog/top-postgresql-database-free-tiers-in-2026)
