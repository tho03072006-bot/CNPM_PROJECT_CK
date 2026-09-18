# Kế hoạch Module 4 — Kiến trúc dùng chung + Testing + Quản lý GitHub/PM

- **Người phụ trách:** Thọ (leader)
- **Cập nhật lần cuối:** 18/09/2026

Tài liệu này chia nhỏ công việc của Module 4 thành các bước làm được trong 1–2 giờ mỗi bước,
để biết hôm nay làm gì, bước nào phải chờ người khác, và bước nào là nền móng cho 3 module còn lại.

Ký hiệu: `[x]` đã xong · `[ ]` chưa làm · `[~]` đang làm / chờ điều kiện bên ngoài.

---

## GIAI ĐOẠN 0 — Rà soát môi trường (XONG 17/09)

- [x] **0.1** Đọc README.md, CONTRIBUTING.md, database/schema.sql, toàn bộ entity + repository hiện có.
- [x] **0.2** Kiểm tra công cụ trên máy leader: Java, Maven, git, GitHub CLI, Docker.
- [x] **0.3** Kiểm tra SQL Server: instance nào đang chạy, database `cinema_booking` đã có chưa.
- [x] **0.4** Ghi lại các vấn đề phát hiện được (xem mục "Vấn đề đã phát hiện" ở cuối file).

## GIAI ĐOẠN 1 — Nền móng Testing (XONG 17/09)

- [x] **1.1** Tạo database riêng cho test: `database/create-test-database.sql` → `cinema_booking_test`.
      Lý do: test xoá sạch dữ liệu trước mỗi test case, không được dùng chung với database đang dev.
- [x] **1.2** Profile `test`: `src/test/resources/application-test.properties`
      (trỏ vào database test, `ddl-auto=update`, lấy user/password từ file secret chung).
- [x] **1.3** `IntegrationTestBase` — lớp cha cho mọi test tích hợp của cả 4 module:
      bật profile test, **chặn test chạy nhầm vào database thật**, xoá sạch dữ liệu trước mỗi test.
- [x] **1.4** `TestDataFactory` — xưởng tạo dữ liệu mẫu dùng chung (user / movie / room / seat / showtime / ticket).
- [x] **1.5** `SeatBookingConcurrencyIntegrationTest` — test ADR-001, 4 test case:
      - ràng buộc UNIQUE (showtime_id, seat_id) có thật trong database;
      - đặt trùng ghế tuần tự → bị từ chối;
      - **2 request cùng lúc → đúng 1 request giữ được ghế**;
      - **10 request cùng lúc → vẫn đúng 1 request giữ được ghế**.
- [x] **1.6** Chạy `mvn test` → pass.

## GIAI ĐOẠN 2 — Database dùng chung trên cloud (XONG 18/09)

- [x] **2.1** Khảo sát các lựa chọn cloud miễn phí, viết `docs/ADR-002-database-dung-chung-tren-cloud.md`.
      **Chốt lần 1: Azure SQL Database gói free. Chốt lần 2 (18/09): đổi sang MonsterASP.NET**
      vì Azure từ chối cho mở tài khoản free — xem mục 2c của ADR-002.
- [x] **2.2** Chuẩn bị `database/schema-cloud.sql` (bản schema chạy được trên database cloud,
      không có lệnh CREATE DATABASE / USE vì nhà cung cấp đã tạo sẵn database).
- [x] **2.3** Chuẩn bị profile `cloud`: `src/main/resources/application-cloud.properties`
      + `application-secrets-cloud.properties.example`. Chuỗi kết nối viết theo kiểu không phụ
      thuộc nhà cung cấp, đổi nhà cung cấp chỉ sửa file secret chứ không sửa code.
- [x] **2.4** Tạo tài khoản nhà cung cấp cloud. Azure từ chối ("not eligible for an Azure free
      account") → đã đăng ký MonsterASP.NET gói Free, được 5 database MSSQL.
- [x] **2.5** Đã tạo database MSSQL trên MonsterASP (datacenter EU, collation
      `SQL_Latin1_General_CP1_CI_AS` — trùng với máy cá nhân nên không lệch), đã bật **Remote access**.
- [x] **2.6** Đã chạy `schema-cloud.sql` và `seed-data.sql` lên database thật
      (6 bảng, 3 người dùng, 6 phim, 2 phòng, 120 ghế, 10 suất chiếu). Chạy ứng dụng với profile
      `cloud` → **`ddl-auto=validate` PASS**, trang chủ và trang lỗi đều render được từ dữ liệu cloud.
      Cloud chạy SQL Server **2025** Express, máy cá nhân chạy **2022** — không ảnh hưởng.
      Thời gian khởi động: 10,5 giây (local là 4,5 giây), chậm hơn do datacenter ở châu Âu.
- [x] **2.7** Viết `database/seed-data.sql`: 6 phim, 2 phòng, 120 ghế, 10 suất chiếu → dữ liệu
      mẫu chung để 4 người test cùng một bộ dữ liệu. Đã chạy thử 2 lần, không nhân đôi dữ liệu.
- [x] **2.8** Bổ sung hướng dẫn chạy profile `cloud` vào README.
- [ ] **2.9** **Gửi thông tin kết nối cho 3 thành viên qua nhóm chat.** Thông tin này cố ý không
      nằm trong repo (repo để public).

## GIAI ĐOẠN 3 — Kiến trúc dùng chung cho 3 module còn lại (XONG PHẦN CHÍNH 17/09)

- [x] **3.1** Package `exception`: `BusinessException` (cha), `SeatAlreadyTakenException`,
      `ResourceNotFoundException`, `InvalidBookingException`.
- [x] **3.2** `GlobalExceptionHandler` (`@ControllerAdvice`) + trang `error.html` theo layout chung.
      Trả HTML cho request thường, trả JSON cho request AJAX (phục vụ màn hình chọn ghế của Module 2).
      Có sẵn lưới an toàn: bắt `DataIntegrityViolationException` nếu Service quên đổi sang
      `SeatAlreadyTakenException` → hết cảnh lỗi 500 thô (yêu cầu trong bảng kiểm duyệt PR).
- [ ] **3.3** **Báo Thắng hợp đồng của Module 2**: khi INSERT vé bị dính UNIQUE
      (showtime_id, seat_id) thì Service phải bắt `DataIntegrityViolationException` và ném
      `SeatAlreadyTakenException`. Nội dung này đã ghi rõ trong Issue #14, nhưng vẫn nên nhắn
      trực tiếp trước khi bạn ấy viết `SeatBookingService`.
- [x] **3.4** Fragment thông báo dùng chung `fragments/alert.html`, đã nhúng sẵn vào `layout/base.html`
      nên mọi trang tự động có chỗ hiện thông báo.
- [x] **3.5** Bổ sung hằng số dùng chung vào `constants/Constants.java`
      (`VIEW_ERROR`, `MODEL_SUCCESS_MESSAGE`, `MODEL_ERROR_MESSAGE`, `MODEL_ERROR_CODE`, `MODEL_ERROR_PATH`).
- [x] **3.6** Thêm Mục 5 (xử lý lỗi) và Mục 6 (quy ước viết test) vào CONTRIBUTING.md.
- [x] **3.7** `GlobalExceptionHandlerIntegrationTest` — 4 test case kiểm chứng phần trên chạy thật.
- [~] **3.8** Giao diện dùng chung (design system): làm theo lộ trình riêng 7 ngày ở
      `docs/LO_TRINH_GIAO_DIEN.md`. Đã xong Ngày 1 (hệ màu theo logo trường + 2 chế độ sáng/tối
      có nút bấm), Ngày 2 (bộ component + trang `/ui-kit`) và Ngày 7 (rà soát tương phản màu:
      20/20 đạt chuẩn WCAG AA ở cả 2 chế độ). Ngày 3–6 phải chờ code của 3 module kia.

## GIAI ĐOẠN 4 — CI trên GitHub Actions (XONG 18/09)

- [x] **4.1** Workflow `.github/workflows/ci.yml`: chạy `mvn -B test` trên mọi push và mọi PR vào `develop`.
- [x] **4.2** Dùng service container `mcr.microsoft.com/mssql/server` làm database test cho CI
      (repo để PUBLIC nên GitHub Actions miễn phí không giới hạn phút).
- [x] **4.3** Gắn huy hiệu trạng thái build vào README.
- [ ] **4.4** Bật branch protection cho `develop` và `main`: bắt buộc CI pass + 1 approve mới được merge.

## GIAI ĐOẠN 5 — Quản lý GitHub / PM

- [x] **5.0** Đã push `develop` sang `main` và cả 3 nhánh cá nhân của Tài, Thắng, Thanh.
      Cả 6 nhánh hiện ở cùng một commit, 3 bạn clone về là có đủ `pom.xml`, entity, schema, test.
- [~] **5.1** Mời 3 thành viên làm collaborator — **Thọ đã gửi lời mời 18/09**, chờ các bạn chấp nhận.
      Sau đó: gán người phụ trách cho từng Issue, và điền tên tài khoản GitHub vào 3 dòng đang
      để trống trong `.github/CODEOWNERS`.
- [x] **5.2** Chốt quy ước tên nhánh: **mỗi người code trên nhánh cá nhân mang tên mình**,
      gộp vào `develop` qua Pull Request, `main` giữ code ổn định. Nhánh phụ `feature/...`
      chỉ dùng khi tính năng làm dài ngày.
- [x] **5.3** Đã tạo 9 nhãn: `module-1..4`, `uu-tien-cao`, `test`, `tai-lieu`, `giao-dien`, `cho-viec-khac`.
- [x] **5.4** Đã tạo 4 milestone theo timeline 4 tuần (hạn 24/09, 01/10, 08/10, 15/10).
- [x] **5.5** Viết `docs/PHAN_CONG.md` — 29 đầu việc của 4 module, mỗi việc có mã, phụ thuộc,
      ước lượng, thuộc tuần nào.
- [x] **5.6** Đã tạo 33 Issue (#2 đến #34) từ bảng phân công, gán đủ nhãn và milestone.
      **Chưa gán được người phụ trách** vì 3 bạn chưa chấp nhận lời mời — xem bước 5.1.
- [ ] **5.7** Tạo GitHub Project board kiểu Kanban: `Backlog | Đang làm | Review | Xong`, kéo hết Issue vào.
      Cần chạy `gh auth refresh -s project,read:project` trước (token hiện thiếu quyền này).
- [x] **5.8** Thêm `.github/pull_request_template.md` theo đúng bảng kiểm Mục 3 của CONTRIBUTING.md.
- [x] **5.9** Thêm `.github/ISSUE_TEMPLATE/` (mẫu báo lỗi + mẫu đầu việc).
- [x] **5.10** Thêm `CODEOWNERS`. Còn thiếu tên tài khoản GitHub của 3 bạn — điền nốt sau bước 5.1.

## GIAI ĐOẠN 6 — Test đầy đủ (PHỤ THUỘC module 1, 2, 3 code xong)

- [ ] **6.1** Test tích hợp luồng đặt vé end-to-end bằng `MockMvc`:
      xem danh sách phim → chọn suất chiếu → xem seat-map → giữ ghế → thanh toán → vé chuyển sang `PAID`.
- [ ] **6.2** Test hết hạn giữ ghế: vé `HELD` quá `SEAT_HOLD_MINUTES` phút → chuyển `EXPIRED`,
      ghế đó phải được giải phóng cho người khác đặt.
- [ ] **6.3** Test race-condition ở **tầng Service** (khi Thắng có `SeatBookingService`):
      kiểm tra Service ném đúng `SeatAlreadyTakenException` chứ không để lỗi 500 lọt ra ngoài.
- [ ] **6.4** Unit test cho các hàm tính toán: tính tiền theo loại ghế (VIP/COUPLE), kiểm tra trùng giờ chiếu.
- [ ] **6.5** Test phân quyền: khách hàng không vào được trang admin.
- [ ] **6.6** Tổng hợp `docs/KE_HOACH_KIEM_THU.md`: bảng test case, kết quả, độ bao phủ → đưa vào báo cáo.

## GIAI ĐOẠN 7 — Tích hợp & bàn giao

- [ ] **7.1** Merge lần lượt 4 module vào `develop`, chạy lại toàn bộ test sau mỗi lần merge.
- [ ] **7.2** Chạy thử toàn hệ thống trên database cloud (tập demo).
- [ ] **7.3** Merge `develop` → `main`, gắn tag phiên bản.
- [ ] **7.4** Hoàn thiện README (ảnh chụp màn hình, sơ đồ kiến trúc).

---

## Thứ tự ưu tiên (làm gì trước)

1. **Bước 2.9 + 5.1** — gửi thông tin database và hoàn tất mời collaborator. Chưa xong hai
   việc này thì 3 bạn vẫn chưa làm việc đầy đủ được.
2. **Bước 3.3** — nhắn Thắng hợp đồng exception trước khi bạn ấy viết `SeatBookingService`.
3. **Bước 5.7** — dựng Project board để thầy thấy nhóm có quản lý công việc.
4. **Bước 4.4** — bật branch protection, tránh có người lỡ tay push thẳng vào `develop`.
5. **Giai đoạn 6** — chờ code của 3 module.

## Vấn đề đã phát hiện

| # | Vấn đề | Ảnh hưởng | Hướng xử lý |
|---|---|---|---|
| V1 | `JAVA_HOME` trên máy leader trỏ vào **Java 8** (`C:\Java8`) trong khi dự án cần **Java 21** | `mvn` chạy bằng Java 8 không build được Spring Boot 3.5 | Đổi `JAVA_HOME` sang JDK 21. Đã ghi vào README để 3 bạn tự kiểm tra bằng `mvn -v` |
| V2 | ~~Database `cinema_booking` chưa tồn tại, `schema.sql` chưa từng được chạy~~ **ĐÃ XỬ LÝ** | — | Đã chạy trên máy leader; README có hướng dẫn cho 3 người còn lại |
| V3 | `localhost,1433` thực tế là instance **`MTho\SQLEXPRESS`** (instance mặc định `MSSQLSERVER` đang tắt) | Báo lỗi kết nối khó hiểu nếu khởi động nhầm instance | Đã ghi rõ trong README: dùng `localhost,1433`, không dùng `.\SQLEXPRESS` |
| V4 | ~~Repo chỉ có 1 collaborator~~ **ĐANG XỬ LÝ** | 3 bạn không push được, không gán Issue cho ai được | Thọ đã gửi lời mời 18/09, chờ chấp nhận — bước 5.1 |
| V5 | Token GitHub CLI thiếu quyền `project` | Không tạo được Project board bằng lệnh | Chạy `gh auth refresh -s project,read:project` rồi làm bước 5.7 |
| V6 | ~~Tên nhánh trên remote đặt theo tên người, lệch với CONTRIBUTING.md~~ **ĐÃ XỬ LÝ 17/09** | — | Đã chốt giữ nhánh cá nhân, sửa lại CONTRIBUTING.md + README cho khớp |
| V7 | Không có Docker trên máy | Không dùng được Testcontainers | Đã chọn hướng khác: test chạy trên SQL Server thật (local) + service container trên GitHub Actions |
| V8 | Chưa có Maven wrapper (`mvnw`) | 4 máy có thể dùng 4 phiên bản Maven khác nhau | Tạm thời chốt phiên bản bằng bảng trong README. Cần bổ sung `mvn wrapper:wrapper` |
| V9 | ~~Commit nền móng chưa từng được push, cả 6 nhánh remote đều ở "Initial commit"~~ **ĐÃ XỬ LÝ 18/09** | — | Đã push `develop` sang `main` và 3 nhánh cá nhân; cả 6 nhánh cùng một commit |
| V10 | `mvn spring-boot:run` báo `Could not find or load main class`. Nguyên nhân: đường dẫn dự án có dấu tiếng Việt mà JVM đang chạy `sun.jnu.encoding=Cp1252` nên giải mã sai classpath khi fork tiến trình con | Không chạy được ứng dụng bằng lệnh Maven. Bẫy ở chỗ `mvn test` vẫn chạy bình thường nên rất dễ tưởng là lỗi code | Hai cách: (a) đổi thư mục dự án sang đường dẫn không dấu, ví dụ `D:\CNPM\Project_CK_NHOM05`; hoặc (b) bật "Beta: Use Unicode UTF-8" trong Windows Region settings. **Nên chọn (a)** vì không đụng chạm cài đặt hệ thống. Cách chạy tạm: `mvn -DskipTests package` rồi `java -jar "target\<tên>.jar"` bằng đường dẫn tương đối |
| V11 | 17/09 mất một đợt file: đổi nhánh sang `Minh_Thọ` (đang ở Initial commit) làm bay hết file chưa commit | Mất công làm lại | **Bài học: commit sớm, commit thường xuyên.** Trước khi đổi nhánh phải `git status` xem còn gì chưa commit. Đã ghi vào CONTRIBUTING.md Mục 1.4 |
| V12 | Entity và `schema.sql` lệch nhau về kiểu số: `Showtime.basePrice` / `Ticket.price` không khai báo `precision`/`scale` nên Hibernate hiểu là `numeric(38,2)`, trong khi `schema.sql` ghi `DECIMAL(10,2)` | **Đã kiểm chứng 18/09: KHÔNG chặn được ứng dụng.** Chạy app ở chế độ `ddl-auto=validate` trên database sạch → khởi động bình thường, Hibernate 6 không kiểm tra precision/scale của kiểu số | Không gấp. Vẫn nên thêm `precision = 10, scale = 2` vào `@Column` cho sạch, nhưng **không chặn cloud**. `Ticket` là file của Thắng, `Showtime` của Tài → hỏi 2 bạn rồi sửa sau |
| V13 | Tên tác giả của commit nền móng `a3d087e` không phải tên thật của Thọ (là một tên tạm đặt lúc khởi tạo), và email cũng khác email đang dùng | Hồ sơ git nộp cho trường nên dùng tên thật và nhất quán | Nội dung tài liệu đã dọn ở commit `c2da0fe`. **Còn tên tác giả của `a3d087e`: phải viết lại lịch sử git mới đổi được — chưa làm, chờ Thọ đồng ý.** Lưu ý: giờ đã push rồi nên viết lại lịch sử sẽ cần `--force-with-lease` và phải báo cả nhóm |
