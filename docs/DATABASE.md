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

## Bốn thứ dễ sai

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

---

## Ràng buộc quan trọng nhất

Bảng `tickets` có ràng buộc `UNIQUE (showtime_id, seat_id)`. **Đây là cơ chế chống hai người
đặt trùng một ghế**, và là thứ duy nhất chặn được chắc chắn — kiểm tra ở tầng Java không ăn
thua vì giữa lúc kiểm tra và lúc ghi vẫn có người chen vào.

Tuyệt đối không xoá ràng buộc này. Cách xử lý khi đụng phải nó: xem Mục 5 của
[`CONTRIBUTING.md`](../CONTRIBUTING.md).

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
