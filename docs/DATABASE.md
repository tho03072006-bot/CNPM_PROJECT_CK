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
mà cột vẫn `varchar` thì phải `ALTER`. Các cột không có annotation (`users`, `seats`, `tickets`)
để `varchar` là đúng, đừng đổi bừa.

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
- Giới hạn đã biết: nếu sau này làm hoàn tiền cho vé **đã thanh toán**, sẽ đụng lại đúng vấn đề
  này — muốn trả ghế về trống thì phải xoá dòng, mà xoá dòng thì mất chứng từ thanh toán. Hoàn
  tiền nằm ngoài phạm vi đồ án nên chưa giải quyết.

---

## Lưu ý khi demo

Gói Free của MonsterASP ghi rõ *"no guarantees"* — không gói free nào của bất kỳ nhà cung cấp
nào có cam kết thời gian hoạt động. Vì vậy:

- **Hôm bảo vệ nên demo từ máy cá nhân** cho nhanh và không phụ thuộc mạng trường, cloud chỉ
  mở ra chứng minh nhóm có database dùng chung thật.
- Chạy thử trước buổi demo ít nhất một ngày.
- Dự phòng: `schema-cloud.sql` + `seed-data.sql` dựng lại toàn bộ database trong 30 giây ở bất
  kỳ đâu.

Datacenter của họ đặt ở châu Âu nên độ trễ từ Việt Nam khoảng 250–300ms, khởi động ứng dụng
mất ~10 giây thay vì ~4 giây khi chạy local. Đó là lý do cloud chỉ dùng để tích hợp, không
dùng để code hằng ngày.
