# Ke hoach Module 4 - Kien truc dung chung + Testing + Quan ly GitHub/PM

- **Nguoi phu trach:** Tho (leader)
- **Cap nhat lan cuoi:** 17/09/2026

Tai lieu nay chia nho cong viec cua Module 4 thanh cac buoc lam duoc trong 1-2 gio moi buoc,
de biet hom nay lam gi, buoc nao phai cho nguoi khac, va buoc nao la nen mong cho 3 module con lai.

Ky hieu: `[x]` da xong - `[ ]` chua lam - `[~]` dang lam / cho dieu kien ben ngoai.

---

## GIAI DOAN 0 - Ra soat moi truong (XONG 17/09)

- [x] **0.1** Doc README.md, CONTRIBUTING.md, database/schema.sql, toan bo entity + repository hien co.
- [x] **0.2** Kiem tra toolchain tren may leader: Java, Maven, git, GitHub CLI, Docker.
- [x] **0.3** Kiem tra SQL Server: instance nao dang chay, database `cinema_booking` da co chua.
- [x] **0.4** Ghi lai cac van de phat hien duoc (xem muc "Van de da phat hien" o cuoi file).

## GIAI DOAN 1 - Nen mong Testing (XONG 17/09)

- [x] **1.1** Tao database rieng cho test: `database/create-test-database.sql` -> `cinema_booking_test`.
      Ly do: test xoa sach du lieu truoc moi test case, khong duoc dung chung voi database dang dev.
- [x] **1.2** Profile `test`: `src/test/resources/application-test.properties`
      (tro vao database test, `ddl-auto=update`, lay user/password tu file secret chung).
- [x] **1.3** `IntegrationTestBase` - lop cha cho moi test tich hop cua ca 4 module:
      bat profile test, **chan test chay nham vao database that**, xoa sach du lieu truoc moi test.
- [x] **1.4** `TestDataFactory` - xuong tao du lieu mau dung chung (user / movie / room / seat / showtime / ticket).
- [x] **1.5** `SeatBookingConcurrencyIntegrationTest` - test ADR-001, 4 test case:
      - rang buoc UNIQUE (showtime_id, seat_id) co that trong database;
      - dat trung ghe tuan tu -> bi tu choi;
      - **2 request cung luc -> dung 1 request giu duoc ghe**;
      - **10 request cung luc -> van dung 1 request giu duoc ghe**.
- [x] **1.6** Chay `mvn test` -> pass.

## GIAI DOAN 2 - Database dung chung tren cloud (DANG LAM - can Tho tao tai khoan)

- [x] **2.1** Khao sat cac lua chon cloud mien phi (so sanh dung luong free: Azure SQL 32 GB vs
      CockroachDB 10 GiB vs Aiven 1 GB vs Neon 0.5 GB vs Supabase 500 MB), viet
      `docs/ADR-002-database-dung-chung-tren-cloud.md`. **Da chot: Azure SQL Database goi free.**
- [x] **2.2** Chuan bi `database/schema-azure.sql` (ban schema chay duoc tren Azure SQL Database).
- [x] **2.3** Chuan bi profile `cloud`: `src/main/resources/application-cloud.properties`
      + `application-secrets-cloud.properties.example`.
- [ ] **2.4** *(Tho lam tay)* Tao tai khoan Azure + tao database free, chon **Auto-pause khi het han muc**.
- [ ] **2.5** *(Tho lam tay)* Mo firewall cho IP cua 4 thanh vien, tao user rieng cho nhom.
- [ ] **2.6** Chay `schema-azure.sql` len cloud, kiem tra `ddl-auto=validate` khong bao loi lech schema.
- [ ] **2.7** Viet `database/seed-data.sql`: 5 phim, 2 phong, so do ghe, ~10 suat chieu -> du lieu mau chung
      de 4 nguoi test cung mot bo du lieu.
- [ ] **2.8** Bo sung huong dan chay profile `cloud` vao README.

## GIAI DOAN 3 - Kien truc dung chung cho 3 module con lai (XONG PHAN CHINH 17/09)

- [x] **3.1** Package `exception`: `BusinessException` (cha), `SeatAlreadyTakenException`,
      `ResourceNotFoundException`, `InvalidBookingException`.
- [x] **3.2** `GlobalExceptionHandler` (`@ControllerAdvice`) + trang `error.html` theo layout chung.
      Tra HTML cho request thuong, tra JSON cho request AJAX (phuc vu man hinh chon ghe cua Module 2).
      Co san luoi an toan: bat `DataIntegrityViolationException` neu Service quen doi sang
      `SeatAlreadyTakenException`. -> het canh loi 500 tho (yeu cau trong checklist PR).
- [ ] **3.3** **Bao Thang hop dong (contract) cua Module 2**: khi INSERT ve bi dinh UNIQUE
      (showtime_id, seat_id) thi Service phai bat `DataIntegrityViolationException` va nem
      `SeatAlreadyTakenException`. Can nhan tin trong nhom truoc khi ban ay viet `SeatBookingService`.
- [x] **3.4** Fragment thong bao dung chung `fragments/alert.html`, da nhung san vao `layout/base.html`
      nen moi trang tu dong co cho hien thong bao.
- [x] **3.5** Bo sung hang so dung chung vao `constants/Constants.java`
      (`VIEW_ERROR`, `MODEL_SUCCESS_MESSAGE`, `MODEL_ERROR_MESSAGE`, `MODEL_ERROR_CODE`, `MODEL_ERROR_PATH`).
- [x] **3.6** Them Muc 5 (xu ly loi) va Muc 6 (quy uoc viet test) vao CONTRIBUTING.md.
- [x] **3.7** `GlobalExceptionHandlerIntegrationTest` - 4 test case kiem chung phan tren chay that.

## GIAI DOAN 4 - CI tren GitHub Actions

- [ ] **4.1** Workflow `.github/workflows/ci.yml`: chay `mvn -B test` tren moi push va moi PR vao `develop`.
- [ ] **4.2** Dung service container `mcr.microsoft.com/mssql/server` lam database test cho CI
      (repo dang de PUBLIC nen GitHub Actions mien phi khong gioi han phut).
- [ ] **4.3** Gan badge trang thai build vao README.
- [ ] **4.4** Bat branch protection cho `develop` va `main`: bat buoc CI pass + 1 approve moi duoc merge.

## GIAI DOAN 5 - Quan ly GitHub / PM

- [ ] **5.0** **GAP NHAT**: push nhanh `main` len GitHub. Hien commit nen mong chi nam o may Tho,
      tren remote ca 6 nhanh deu dang o "Initial commit" (chi co README + .gitignore) -> 3 ban kia
      clone ve khong co gi de code. Sau do tao lai `develop` va 4 nhanh ca nhan tu `main`.
- [ ] **5.1** Moi 3 thanh vien lam collaborator cua repo (hien tai repo moi co mot minh Tho).
- [x] **5.2** Chot quy uoc ten nhanh: **moi nguoi code tren nhanh ca nhan mang ten minh**,
      gop vao `develop` qua Pull Request, `main` giu code on dinh. Nhanh phu `feature/...`
      chi dung khi tinh nang lam dai ngay. Da cap nhat lai Muc 1.4 + Muc 3 cua CONTRIBUTING.md
      va muc "Quy uoc GitHub" cua README cho khop.
- [ ] **5.3** Tao bo label: `module-1`, `module-2`, `module-3`, `module-4`, `uu-tien-cao`, `bug`, `test`, `tai-lieu`.
- [ ] **5.4** Tao 4 milestone theo timeline 4 tuan.
- [ ] **5.5** Viet `docs/PHAN_CONG.md` - bang phan cong chi tiet tung dau viec cua 4 module
      (README dang ghi "se bo sung"). **Day la nguon de sinh Issue** -> phai co truoc buoc 5.6.
- [ ] **5.6** Tao Issue cho tung dau viec, gan label + milestone + nguoi phu trach.
- [ ] **5.7** Tao GitHub Project board kieu Kanban: `Backlog | Dang lam | Review | Xong`, keo het Issue vao.
      Can chay `gh auth refresh -s project,read:project` truoc (token hien thieu quyen nay).
- [ ] **5.8** Them `.github/pull_request_template.md` theo dung checklist Muc 3 cua CONTRIBUTING.md.
- [ ] **5.9** Them `.github/ISSUE_TEMPLATE/` (mau bao loi + mau dau viec).
- [ ] **5.10** Them `CODEOWNERS` de PR dung file dung chung (layout, constants) tu dong goi Tho review.

## GIAI DOAN 6 - Test day du (PHU THUOC module 1, 2, 3 code xong)

- [ ] **6.1** Test tich hop luong dat ve end-to-end bang `MockMvc`:
      xem danh sach phim -> chon suat chieu -> xem seat-map -> giu ghe -> thanh toan -> ve chuyen sang `PAID`.
- [ ] **6.2** Test het han giu ghe: ve `HELD` qua `SEAT_HOLD_MINUTES` phut -> chuyen `EXPIRED`,
      ghe do phai duoc giai phong cho nguoi khac dat.
- [ ] **6.3** Test race-condition o **tang Service** (khi Thang co `SeatBookingService`):
      kiem tra Service nem dung `SeatAlreadyTakenException` chu khong de loi 500 loi ra ngoai.
- [ ] **6.4** Unit test cho cac ham tinh toan: tinh tien theo loai ghe (VIP/COUPLE), kiem tra trung gio chieu.
- [ ] **6.5** Test phan quyen: khach hang khong vao duoc trang admin.
- [ ] **6.6** Tong hop `docs/KE_HOACH_KIEM_THU.md`: bang test case, ket qua, do bao phu -> dua vao bao cao.

## GIAI DOAN 7 - Tich hop & ban giao

- [ ] **7.1** Merge lan luot 4 module vao `develop`, chay lai toan bo test sau moi lan merge.
- [ ] **7.2** Chay thu toan he thong tren database cloud (demo that).
- [ ] **7.3** Merge `develop` -> `main`, gan tag phien ban.
- [ ] **7.4** Hoan thien README (anh chup man hinh, huong dan chay, so do kien truc).

---

## Thu tu uu tien (lam gi truoc)

1. **Buoc 5.0** - push nen mong len GitHub. Chua co buoc nay thi ca nhom khong code duoc.
2. **Giai doan 3.3** - bao hop dong exception cho Thang truoc khi ban ay viet `SeatBookingService`.
3. **Giai doan 5.5 + 5.6 + 5.7** (phan cong + Issue + board) - de thay thay nhom co quan ly cong viec.
4. **Giai doan 2.4 -> 2.7** (database cloud) - can lam truoc khi 4 nguoi bat dau nhap du lieu that.
5. **Giai doan 4** (CI) - lam duoc luc nao cung duoc, nhung co som thi bat loi merge som.
6. **Giai doan 6** - cho code cua 3 module.

## Van de da phat hien (can xu ly)

| # | Van de | Anh huong | Huong xu ly |
|---|---|---|---|
| V1 | `JAVA_HOME` tren may leader dang tro vao **Java 8** (`C:\Java8`), trong khi du an can **Java 21** | `mvn` chay bang Java 8 se khong build duoc Spring Boot 3.5 | Doi `JAVA_HOME` sang JDK 21 (may da cai san `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`). Can dan ca 3 thanh vien kiem tra bang `mvn -v` |
| V2 | Database `cinema_booking` chua ton tai, `schema.sql` chua tung duoc chay | Khong ai chay duoc ung dung | Da chay `schema.sql` tren may leader. Can dan 3 nguoi con lai lam theo README |
| V3 | Instance SQL Server: `localhost,1433` thuc te la **`MTho\SQLEXPRESS`** (instance mac dinh `MSSQLSERVER` dang tat) | Bao loi ket noi kho hieu neu khoi dong nham instance | Ghi ro trong README: dung `localhost,1433`, khong dung `.\SQLEXPRESS` |
| V4 | Repo moi co **1 collaborator** (tho03072006-bot) | 3 nguoi kia khong push / khong duoc gan Issue | Buoc 5.1 |
| V5 | Token GitHub CLI thieu quyen `project` | Khong tao duoc Project board bang lenh | Chay `gh auth refresh -s project,read:project` roi lam buoc 5.7 |
| V6 | ~~Ten nhanh tren remote dat theo ten nguoi, lech voi CONTRIBUTING.md~~ **DA XU LY 17/09** | - | Da chot: giu nhanh ca nhan, sua lai CONTRIBUTING.md + README cho khop (buoc 5.2) |
| V7 | Khong co Docker tren may | Khong dung duoc Testcontainers | Da chon huong khac: test chay tren SQL Server that (local) + service container tren GitHub Actions |
| V8 | Chua co Maven wrapper (`mvnw`) | 4 may co the dung 4 phien ban Maven khac nhau | Can bo sung `mvn wrapper:wrapper` |
| V9 | **Commit nen mong `a3d087e` chua tung duoc push.** Ca 6 nhanh tren remote (main, develop, 4 nhanh ca nhan) deu dang o `3764755 Initial commit` - chi co README + .gitignore | 3 thanh vien clone ve khong co pom.xml, khong co entity, khong co gi de code | Buoc 5.0 - **lam ngay** |
| V10 | `mvn spring-boot:run` bao `Could not find or load main class`. Nguyen nhan: duong dan du an co dau tieng Viet (`D:\Cong nghe phan mem\...`) ma JVM tren may dang co `sun.jnu.encoding=Cp1252` nen giai ma sai classpath khi fork tien trinh con | Khong chay duoc ung dung bang lenh Maven (rieng `mvn test` van chay duoc vi surefire truyen classpath kieu khac) | 2 cach: (a) doi thu muc du an sang duong dan khong dau, vi du `D:\CNPM\Project_CK_NHOM05`; hoac (b) bat "Beta: Use Unicode UTF-8 for worldwide language support" trong Windows Region settings. Nen chon (a) vi khong dung cham cai dat he thong |
| V11 | 17/09 da xay ra 1 lan mat file: doi nhanh sang `Minh_Thọ` (dang o Initial commit) lam bay het file chua commit | Mat cong lam lai | **Bai hoc: commit som, commit thuong xuyen.** Truoc khi doi nhanh phai `git status` xem con gi chua commit |
