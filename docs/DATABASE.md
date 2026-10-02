# Database của dự án

Nhóm dùng **SQL Server** ở hai nơi, cộng thêm một database riêng cho test.

| Nơi | Dùng để làm gì | Profile | `ddl-auto` |
|---|---|---|---|
| `cinema_booking` trên máy cá nhân | Code hằng ngày | mặc định | `update` |
| `cinema_booking_test` trên máy cá nhân | Chạy `mvn test` | `test` | `update` |
| MSSQL trên **MonsterASP.NET** (gói Free) | Tích hợp và demo cho cả nhóm | `cloud` | `validate` |

**Vì sao chọn MonsterASP:** vẫn là SQL Server nên không phải sửa một dòng code nào, gói Free
cho 5 database / 1 GB (dự án này chưa tới 100 MB), và đăng ký không cần thẻ tín dụng.

---

## Dùng thế nào

### 1. Database trên máy cá nhân — dùng hằng ngày

```bash
sqlcmd -S localhost,1433 -U sa -C -f 65001 -i database/schema.sql
```

```bash
sqlcmd -S localhost,1433 -U sa -C -f 65001 -d cinema_booking -i database/seed-data.sql
```

Rồi copy `application-secrets.properties.example` thành `application-secrets.properties`,
điền thông tin SQL Server của máy bạn, và chạy `mvn spring-boot:run`.

### 2. Database cho test — làm một lần

```bash
sqlcmd -S localhost,1433 -U sa -C -f 65001 -i database/create-test-database.sql
```

Bảng bên trong do Hibernate tự sinh từ các `@Entity`, không cần chạy `schema.sql`.
Test **xoá sạch dữ liệu trước mỗi test case** nên bắt buộc phải tách riêng, không dùng chung
với `cinema_booking`.

### 3. Database dùng chung trên cloud — lúc tích hợp và demo

Xin Thọ thông tin kết nối trong nhóm chat. **Thông tin này cố ý không nằm trong repo** vì repo
để public.

```bash
cp application-secrets-cloud.properties.example application-secrets-cloud.properties
```

Điền 4 dòng `cloud.db.*` rồi chạy:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=cloud
```

---

## Năm thứ dễ sai

**Lệnh `sqlcmd` đọc file phải luôn có `-f 65001`.** Thiếu là tiếng Việt thành chữ rác kiểu
`BÃ£o Giá»¯a Trá»i Quang`, và thường tới lúc demo mới phát hiện.

**Server của cloud phải có chữ `.public.`** — lấy ở tab *Remote access* trong trang quản trị.
Tab *Local access* cho một hostname khác, chỉ dùng được từ bên trong datacenter của họ, máy
mình nối vào sẽ báo `No such host is known`.

**Giữ nguyên dấu `#` ở dòng `cloud.db.options`** trong file secret. Dòng đó là cấu hình riêng
của Azure, bỏ dấu `#` ra là dính lỗi TLS `PKIX path building failed`.

**Không tự sửa schema trên cloud.** Profile `cloud` đặt `ddl-auto=validate` nên Hibernate
không được tự đổi bảng. Cần thêm/sửa bảng thì sửa `database/schema-cloud.sql`, chạy tay lên
cloud, rồi báo cả nhóm — tránh cảnh 4 người cùng sửa làm schema chung biến dạng.

**Đổi kiểu cột trong `@Entity` thì database test trên máy KHÔNG tự đổi theo.** `ddl-auto=update`
chỉ biết thêm bảng và thêm cột mới — nó không bao giờ đổi kiểu của một cột đã tồn tại. Nên khi
ai đó thêm `@Nationalized` (hoặc đổi `length`, đổi kiểu dữ liệu) vào một trường, `cinema_booking_test`
trên máy bạn vẫn giữ cột `varchar` cũ, và `mvn test` đổ hàng loạt lỗi:

```
Could not extract column [3] from JDBC ResultSet
[The conversion from varchar to NCHAR is unsupported.]
```

**CI không bắt được lỗi này** vì mỗi lần chạy nó tạo một database rỗng hoàn toàn, Hibernate sinh
cột mới đúng kiểu ngay từ đầu. Chỉ máy cá nhân — nơi database test đã tồn tại từ trước — mới dính.

Ngày 20/09/2026 nhóm đã dính đúng lỗi này sau khi Module 1 thêm `@Nationalized` vào `Movie` và
`Room`. Cách sửa: đổi kiểu đúng những cột bị lệch, không cần xoá database.

```bash
sqlcmd -S localhost,1433 -U sa -C -f 65001 -d cinema_booking_test -Q "ALTER TABLE movies ALTER COLUMN title NVARCHAR(200) NOT NULL; ALTER TABLE movies ALTER COLUMN genre NVARCHAR(100) NULL; ALTER TABLE movies ALTER COLUMN poster_url NVARCHAR(500) NULL; ALTER TABLE movies ALTER COLUMN age_rating NVARCHAR(10) NULL; ALTER TABLE rooms ALTER COLUMN name NVARCHAR(50) NOT NULL;"
```

Muốn kiểm tra máy mình có bị lệch không thì liệt kê các cột chưa phải Unicode:

```bash
sqlcmd -S localhost,1433 -U sa -C -f 65001 -d cinema_booking_test -Q "SELECT t.name, c.name, ty.name FROM sys.tables t JOIN sys.columns c ON c.object_id=t.object_id JOIN sys.types ty ON ty.user_type_id=c.user_type_id WHERE ty.name IN ('varchar','char','text') ORDER BY 1,2;"
```

Đối chiếu danh sách đó với các trường có `@Nationalized` trong `entity/`. Trường nào có annotation
mà cột vẫn `varchar` thì phải `ALTER`. Các cột không có annotation (email, mật khẩu băm, vai trò,
`seats`, `tickets`) chỉ chứa chữ không dấu nên để `varchar` là đúng, đừng đổi bừa.

Ngày 27/09/2026 dính lần hai: `User.fullName` bị sót `@Nationalized`, nên database test lưu
"Trần Văn Mới" thành "Tr?n Van M?i". Database dev và cloud không sao vì dựng từ `schema.sql`
(đã là `NVARCHAR`), chỉ database test và CI do Hibernate tự sinh bảng mới bị. Đã thêm annotation;
máy nào đã có sẵn `cinema_booking_test` thì chạy thêm:

```bash
sqlcmd -S localhost,1433 -U sa -C -f 65001 -d cinema_booking_test -Q "ALTER TABLE users ALTER COLUMN full_name NVARCHAR(150) NOT NULL;"
```

---

## Ràng buộc quan trọng nhất

Bảng `tickets` có ràng buộc `UNIQUE (showtime_id, seat_id)`. **Đây là cơ chế chống hai người
đặt trùng một ghế**, và là thứ duy nhất chặn được chắc chắn — kiểm tra ở tầng Java không ăn
thua vì giữa lúc kiểm tra và lúc ghi vẫn có người chen vào.

Tuyệt đối không xoá ràng buộc này. Cách xử lý khi đụng phải nó: xem Mục 5 của
[`CONTRIBUTING.md`](../CONTRIBUTING.md).

### ADR-2: vé chưa thanh toán mà hết hạn hoặc bị huỷ thì **xoá hẳn dòng**

*Chốt ngày 20/09/2026.*

**Vấn đề.** Ràng buộc `UNIQUE (showtime_id, seat_id)` ở trên chỉ nhìn hai cột suất chiếu và
ghế — nó **không nhìn cột `status`**. Nên chỉ cần dòng vé còn nằm trong bảng là chỗ đó đã bị
chiếm, bất kể vé mang trạng thái gì. Hệ quả: đổi vé sang `EXPIRED` hay `CANCELLED` **không
giải phóng được ghế** — ghế đó chết luôn cho tới hết suất chiếu, không ai mua lại được.

Đã kiểm chứng trực tiếp trên SQL Server: giữ ghế → đổi sang `EXPIRED` → người khác đặt lại
cùng ghế đó thì nhận `Violation of UNIQUE KEY constraint 'uq_showtime_seat'`. Thử lại với
`CANCELLED` cũng y hệt.

**Quyết định.** Vé **chưa thanh toán** mà hết hạn giữ hoặc bị người dùng huỷ thì `DELETE` hẳn
dòng đó, **không** `UPDATE` sang `EXPIRED`/`CANCELLED`.

```java
// M2.6 - don ve giu qua han
ticketRepository.deleteAll(
        ticketRepository.findExpiredHeldTickets(LocalDateTime.now().minusMinutes(Constants.SEAT_HOLD_MINUTES)));

// M2.7 - nguoi dung tu huy truoc khi thanh toan
ticketRepository.delete(ticket);
```

**Vì sao chọn cách này.** Ràng buộc ADR-1 giữ nguyên không phải đụng tới, hạ tầng test không
phải sửa, Module 2 làm được ngay. Và trong phạm vi đồ án thì không mất mát gì thật: M2.7 ghi
rõ là huỷ **trước khi thanh toán**, nên `CANCELLED` và `EXPIRED` chỉ rơi vào vé chưa trả tiền.
Vé `PAID` không bao giờ bị xoá, nên trang lịch sử vé của Module 3 vẫn đủ dữ liệu.

**Đã cân nhắc và loại: filtered index.** Có thể thay ràng buộc bằng
`CREATE UNIQUE INDEX ... WHERE status IN ('HELD','PAID')` để giữ được lịch sử. Loại vì
Hibernate không khai báo được mệnh đề `WHERE` của index qua annotation, mà database test lại
do Hibernate tự sinh bảng (`ddl-auto=update`) — index sẽ không tồn tại trong database test và
test ADR-1 sẽ báo xanh giả, tức là mất luôn thứ đang chứng minh ADR-1 hoạt động.

**Hai điều kèm theo.**

- Hai giá trị `EXPIRED` và `CANCELLED` trong enum `TicketStatus` **không còn được dùng** trong
  luồng hiện tại. Giữ lại trong enum vì Module 2 đang tham chiếu `TicketStatus.values()`, xoá đi
  sẽ làm gãy `SeatService`.
- Giới hạn từng ghi ở đây — hoàn tiền cho vé **đã thanh toán** thì xoá dòng sẽ mất chứng từ —
  đã giải quyết ở ADR-3 ngay dưới.

### ADR-3: huỷ vé đã thanh toán — vẫn xoá dòng, nhưng chép biên nhận sang bảng riêng

**Bối cảnh.** Từ ngày 27/09/2026 khách được tự huỷ vé đã thanh toán và nhận lại tiền theo chính
sách (còn ≥ 24 giờ: 100%, 2 tới dưới 24 giờ: 50%, dưới 2 giờ hoặc đã vào phòng: không huỷ).
Ghế phải trống lại để người khác mua, mà ràng buộc `UNIQUE (showtime_id, seat_id)` của ADR-1
không nhìn cột `status` — giống hệt vấn đề của ADR-2.

**Quyết định.** Huỷ vé đã thanh toán cũng **xoá dòng** trong `tickets`. Nhưng trước khi xoá,
chép mọi thứ liên quan tới tiền sang bảng `ticket_refunds`: giá đã trả, phần trăm và số tiền
hoàn, mã giao dịch MoMo, mã giao dịch hoàn, thời điểm trả và thời điểm hoàn. Chép luôn tên phim,
phòng, ghế, giờ chiếu (không chỉ giữ khoá ngoại) vì đây là chứng từ tiền bạc: phim đổi tên hay
suất bị xoá sau này thì biên nhận vẫn phải đọc được.

**Hệ quả.**

- Thống kê: vé đã huỷ không còn tính là vé bán ra, nhưng phần rạp giữ lại (giá trừ tiền hoàn)
  vẫn cộng vào doanh thu ngày bán. Trang thống kê có thêm ô "Đã hoàn tiền".
- Hai chốt chặn nằm ngay trong câu lệnh SQL, cùng tinh thần ADR-1: huỷ vé là
  `DELETE ... WHERE checked_in_at IS NULL`, soát vé vào phòng là `UPDATE ... WHERE checked_in_at IS NULL`.
  Khách bấm huỷ đúng lúc nhân viên soát vé ở cửa thì chỉ một bên thắng.
- Vé trả qua MoMo: gọi API hoàn tiền của MoMo **sau** khi đã ghi biên nhận và xoá vé, trong
  cùng transaction. MoMo từ chối thì transaction rollback, vé còn nguyên.

### Các cột và bảng thêm ngày 27/09/2026

| Chỗ | Kiểu | Dùng để |
|---|---|---|
| `tickets.payment_method` | `NVARCHAR(20)` NULL | `COUNTER` (tại quầy) hoặc `MOMO`. Vé cũ để NULL, coi như trả tại quầy |
| `tickets.payment_ref` | `NVARCHAR(100)` NULL | Mã giao dịch MoMo (`transId`), cần để hoàn tiền đúng giao dịch |
| `tickets.checked_in_at` | `DATETIME2` NULL | Lúc nhân viên soát vé cho vào phòng; khác NULL là vé đã dùng |
| bảng `ticket_refunds` | — | Biên nhận hoàn tiền theo ADR-3 |

Chỉ **thêm** cột cho phép NULL và thêm bảng, không sửa hay xoá gì của cái cũ, nên code cũ vẫn
chạy được với database đã nâng cấp.

- **Máy cá nhân và database test:** không phải làm gì. Hai database này chạy `ddl-auto=update`,
  lần khởi động đầu tiên Hibernate tự thêm cột và bảng.
- **Cloud: đã nâng cấp ngày 30/09/2026** bằng cách chạy lại `schema-cloud.sql` (file này giờ có
  thêm khối "Bổ sung 27/09/2026": `ALTER TABLE ... ADD` có kiểm tra "nếu chưa có" và tạo bảng
  `ticket_refunds`). Dữ liệu cũ giữ nguyên; chạy lại lần hai không lỗi. Đã kiểm chứng bằng cách
  chạy app với profile `cloud`: Hibernate `validate` chấp nhận, trang Vé của tôi và sơ đồ ghế
  chạy bình thường.
- **Database tạo mới:** `schema.sql` và `schema-cloud.sql` đều đã có cột và bảng mới.

### Kho bắp nước — thêm ngày 02/10/2026

| Chỗ | Kiểu | Dùng để |
|---|---|---|
| `concession_products.stock_quantity` | `INT NOT NULL DEFAULT 0` | Tồn kho của món lẻ. Có `CHECK (stock_quantity >= 0)` làm chốt chặn cuối |
| `concession_products.low_stock_threshold` | `INT NOT NULL DEFAULT 10` | Còn từ mức này trở xuống thì báo "Sắp hết" cho nhân viên |
| bảng `concession_combo_items` | — | Công thức combo: combo gồm món lẻ nào, mỗi món mấy phần |
| bảng `concession_stock_movements` | — | Lịch sử kho: nhập, xuất hủy, kiểm kê, bán (kèm mã hóa đơn) |

**Combo không có kho riêng.** Số combo còn bán được tính theo món thành phần còn ít nhất; bán một
combo là trừ kho từng món bên trong. Nhờ vậy nhân viên chỉ nhập kho bắp và nước, không phải nhập
riêng "combo".

**Trừ kho lúc thanh toán, không giữ kho lúc chọn món.** Chọn món chỉ kiểm tra còn đủ hay không.
Lúc thanh toán, kho bị trừ bằng một câu `UPDATE ... SET stock_quantity = stock_quantity - n
WHERE id = ? AND stock_quantity >= n`. Câu lệnh trả về 0 dòng nghĩa là không đủ hàng, khi đó cả
lần thanh toán bị hủy (MoMo tự hoàn tiền). Hai khách cùng mua phần cuối cùng thì chỉ một người
mua được, giống tinh thần ADR-1.

- **Máy cá nhân và database test:** Hibernate (`ddl-auto=update`) tự thêm cột và bảng. Cột mới có
  `@ColumnDefault` nên thêm được vào bảng đã có dữ liệu. Muốn có sẵn công thức combo và tồn đầu kỳ
  thì chạy lại `seed-data.sql` (mục 6b); chạy lại nhiều lần vẫn an toàn.
- **Cloud: đã nâng cấp ngày 02/10/2026** bằng cách chạy khối "Bổ sung 02/10/2026" của
  `schema-cloud.sql` trong một transaction (lỗi giữa chừng thì hoàn lại toàn bộ). Khối này chỉ thêm
  cột và bảng, kèm công thức combo và tồn đầu kỳ (bắp vừa 120, bắp lớn 80, nước 200); 5 đơn hàng
  cũ giữ nguyên. Đã kiểm chứng bằng cách chạy app với profile `cloud`: Hibernate `validate` chấp
  nhận, trang kho và hóa đơn cũ hiển thị đúng. Code cũ vẫn chạy bình thường trên database đã nâng
  cấp. Lưu ý: profile `cloud` dùng `validate`, nên database nào chưa có cột `stock_quantity` thì
  bản code có kho bắp nước không khởi động được — chạy khối trên trước.
- **Giờ trên server cloud là giờ châu Âu (UTC+2)**, không phải giờ Việt Nam. Ứng dụng tự ghi giờ
  Việt Nam nên không sao; riêng câu SQL tự ghi thời gian thì quy về UTC+7 như trong khối trên
  (`CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00') AS DATETIME2)`).

---

## Lưu ý khi demo

Gói Free của MonsterASP ghi rõ *"no guarantees"* — không gói free nào của bất kỳ nhà cung cấp
nào có cam kết thời gian hoạt động. Vì vậy:

- **Hôm bảo vệ nên demo từ máy cá nhân** cho nhanh và không phụ thuộc mạng trường, cloud chỉ
  mở ra chứng minh nhóm có database dùng chung thật.
- Chạy thử trước buổi demo ít nhất một ngày.
- **Kiểm tra lịch chiếu còn hạn không.** Đây là cái dễ quên nhất và hỏng demo nặng nhất: dữ
  liệu mẫu sinh suất chiếu cho 5 ngày *kể từ ngày chạy `seed-data.sql`*, nên để lâu là lịch
  chiếu nằm hết trong quá khứ, mở app ra thấy trang lịch trống và không đặt vé được câu nào.
  Chạy lại `seed-data.sql` trước hôm bảo vệ một ngày là xong — file này chạy lại được nhiều
  lần, chỉ thêm suất chiếu mới chứ không xoá gì, cũng không nhân đôi dữ liệu cũ.

  Từ 30/09/2026 file seed còn **bỏ qua suất chồng giờ** với suất đã có trong cùng phòng. Trước đó
  nó chỉ bỏ qua suất trùng y hệt, nên chạy lại vào một ngày khác thì mẫu lịch dịch đi và một
  phòng chiếu hai phim cùng lúc: cả database máy cá nhân lẫn cloud còn 139 cặp như vậy ở ngày
  23–25/09 (đều đã qua, không có vé, không ảnh hưởng gì). Lần chạy ngày 30/09 nối lịch tới hết
  05/10 trên cả hai nơi, không thêm cặp chồng giờ nào.

  Câu kiểm tra nhanh còn bao nhiêu ngày lịch chiếu:

  ```sql
  SELECT COUNT(*) AS suat_con_o_tuong_lai,
         COUNT(DISTINCT CAST(start_time AS DATE)) AS so_ngay_con_lich,
         MAX(start_time) AS suat_cuoi_cung
  FROM showtimes WHERE start_time > GETDATE();
  ```
- Dự phòng: `schema-cloud.sql` + `seed-data.sql` dựng lại toàn bộ database trong 30 giây ở bất
  kỳ đâu.

Datacenter của họ đặt ở châu Âu nên độ trễ từ Việt Nam khoảng 250–300ms, khởi động ứng dụng
mất ~10 giây thay vì ~4 giây khi chạy local. Đó là lý do cloud chỉ dùng để tích hợp, không
dùng để code hằng ngày.
