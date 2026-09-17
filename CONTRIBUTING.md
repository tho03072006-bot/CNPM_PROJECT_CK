# Quy uoc lam viec nhom - NHOM05 (Do an CNPM: He thong dat ve xem phim)

Tai lieu nay ap dung cho ca 4 thanh vien (Tho, Tai, Thang, Thanh). Muc tieu: code cua 4 nguoi
nhin vao giong nhu 1 nguoi viet, tranh xung dot khi merge, va de cham diem theo dung tinh chat
mon Cong nghe phan mem (dat ten ro rang, cau truc mo-dun hoa, de bao tri).

---

## 1. Quy uoc dat ten (Naming convention)

### 1.1 Java

| Doi tuong | Quy tac | Vi du |
|---|---|---|
| Class / Interface / Enum | PascalCase, danh tu | `MovieService`, `TicketRepository`, `TicketStatus` |
| Method | camelCase, dong tu + bo ngu, ro hanh dong | `findAvailableSeats()`, `holdSeat()`, `calculateTotalPrice()` |
| Bien / tham so | camelCase, danh tu ro nghia, KHONG viet tat kho hieu | `showtimeId` (khong dung `stId`), `customerEmail` (khong dung `e`) |
| Hang so (constant) | UPPER_SNAKE_CASE | `SEAT_HOLD_MINUTES`, `ROLE_ADMIN` |
| Package | chu thuong, khong dau, so nhieu cho tang chua nhieu class cung loai | `entity`, `repository`, `service`, `controller`, `constants` |
| Boolean | tien to `is`/`has`/`can` | `isActive`, `hasDiscount`, `canCancel()` |

Khong dung Lombok (dung dung convention nhom da quen o mon Lap trinh Web) — viet tay
getter/setter/constructor.

### 1.2 Entity & Database (da ap dung trong `database/schema.sql`)

- Ten bang: so nhieu, snake_case — `movies`, `showtimes`, `tickets`.
- Ten cot khoa ngoai: `<ten_bang_so_it>_id` — `movie_id`, `room_id`, `seat_id`.
- Cot trang thai: dung enum String (`status NVARCHAR(20)`), gia tri VIET HOA — `HELD`, `PAID`.
- Text tieng Viet: luon `NVARCHAR`, khong dung `VARCHAR` (tranh loi font khi luu dau).

### 1.3 Thymeleaf / Frontend

- Ten file template: kebab-case, trung voi chuc nang trang — `movie-list.html`, `seat-map.html`,
  `payment-confirm.html`.
- Class CSS: kebab-case, co tien to theo module de tranh dam vao nhau — `.seat`, `.seat-map`,
  `.admin-table` (da co san trong `static/css/style.css`, moi module them class MOI thi noi
  tiep vao cuoi file, khong sua lai class nguoi khac dang dung).
- Bien Thymeleaf trong model: camelCase, trung ten voi field cua entity/DTO tuong ung.

### 1.4 Git — branch & commit

- **Mo hinh nhanh cua nhom (da chot):** moi nguoi co 1 nhanh ca nhan mang ten minh, code va
  commit tren do; xong viec thi gop vao `develop`; `main` chi chua code on dinh.

  | Nhanh | Ai dung | Dung de lam gi |
  |---|---|---|
  | `main` | ca nhom | Code on dinh, chi nhan merge tu `develop` |
  | `develop` | ca nhom | Nhanh gop chung cua 4 module |
  | `Minh_Thọ` | Tho | Module 4 - kien truc dung chung, testing, tai lieu |
  | `Hữu_Tài` | Tai | Module 1 - phim / phong chieu / suat chieu |
  | `Hữu_Thắng` | Thang | Module 2 - ghe & ve, seat-map, giu ghe |
  | `Tuấn_Thanh` | Thanh | Module 3 - user/auth, thanh toan, email, dashboard |

  Truoc khi bat dau viec moi, nho keo `develop` ve nhanh cua minh cho khoi lech:
  `git switch <nhanh-cua-ban>` roi `git merge origin/develop`.

- Neu mot tinh nang lam dai ngay (vi du sua lai ca luong dat ve) thi co the tach them nhanh phu
  tu nhanh ca nhan, dat ten `feature/<module>-<mo-ta-ngan>` (vd `feature/module2-seat-map`).
  Khong dat ten chung chung nhu `feature/fix` hay `test1`.

- Commit message: `<loai>(<module>): <mo ta ngan, tieng Viet khong dau hoac co dau deu duoc>`
  - `feat` = them tinh nang moi
  - `fix` = sua loi
  - `refactor` = sua cau truc code, khong doi hanh vi
  - `docs` = tai lieu (README, CONTRIBUTING, comment)
  - `test` = them/sua test
  - `chore` = viec linh tinh (cau hinh, dependency, gitignore...)
  - Vi du: `feat(module2): them API giu ghe theo suat chieu`,
    `fix(module1): sua loi validate trung gio chieu`
- MOI commit chi lam MOT viec — khong gom "sua 3 bug + them 2 tinh nang" vao 1 commit, se rat
  kho review va khi can revert.
- **Commit som, commit thuong xuyen.** Truoc khi doi nhanh luon chay `git status` xem con gi
  chua commit — ngay 17/09 nhom da mat mot dot cong viec vi doi nhanh luc con file chua commit.

---

## 2. Cau truc thu muc (da dung san trong nen mong du an)

```
src/main/java/edu/hcmute/cnpm/cinema/
  entity/        - cac lop @Entity anh xa bang database (KHONG chua logic nghiep vu)
  repository/     - interface extends JpaRepository (chi khai bao truy van, khong xu ly logic)
  service/        - logic nghiep vu (tinh tien, khoa ghe, gui email...) — MOI THANH VIEN TU TAO
                     goi service rieng cho module minh trong thu muc nay, vi du: MovieService,
                     SeatBookingService, PaymentService, AuthService
  controller/     - @Controller Spring MVC, chi nhan request/tra view, KHONG chua logic nghiep vu
  exception/      - exception nghiep vu + GlobalExceptionHandler dung chung (Tho quan ly)
  constants/      - hang so dung chung
src/main/resources/
  templates/
    layout/       - layout dung chung (Tho quan ly, khong tu y sua)
    fragments/    - header/footer/alert dung chung (Tho quan ly)
    error.html    - trang bao loi chung (Tho quan ly)
    <module>/     - MOI MODULE tao 1 thu muc con rieng cho trang cua minh, vi du:
                     templates/movie/, templates/booking/, templates/account/
  static/css/style.css - CSS dung chung, them class moi vao cuoi file
src/test/java/edu/hcmute/cnpm/cinema/
  support/        - IntegrationTestBase + TestDataFactory dung chung cho test (Tho quan ly)
  <module>/       - test cua tung module
database/schema.sql    - script SQL tong hop (Tho gop lai tu de xuat cua tung nguoi)
```

**Nguyen tac quan trong:** Controller goi Service, Service goi Repository — KHONG duoc de Controller
goi thang Repository hay chua logic tinh toan/dieu kien nghiep vu (dung dung kien truc phan lop
da hoc o Chuong 2 va da ap dung o mon Lap trinh Web).

---

## 3. Quy trinh Pull Request

1. Code tren nhanh ca nhan cua ban (xem bang o Muc 1.4). Truoc khi bat dau, keo `develop` ve
   nhanh minh de khong lam tren ban cu: `git merge origin/develop`.
2. Code xong, tu kiem tra lai (co the dung skill `engineering:code-review` de tu ra soat truoc).
3. Push nhanh ca nhan, mo Pull Request **vao `develop`** (khong mo thang vao `main`), mo ta ro PR
   lam gi, anh chup man hinh neu co giao dien moi.
4. Can it nhat 1 thanh vien khac approve truoc khi merge — uu tien nguoi lam module lien quan gan nhat.
5. Checklist toi thieu truoc khi duyet PR:
   - [ ] Khong co logic nghiep vu trong Controller
   - [ ] Co xu ly loi (try/catch hoac validation), khong de loi 500 tho
   - [ ] Khong hardcode chuoi ket noi DB / mat khau / API key
   - [ ] Dat ten bien/ham dung quy uoc o Muc 1
   - [ ] Khong sua file cua module khac ma khong bao truoc trong nhom chat
6. Nhanh ca nhan thi GIU LAI (dung suot ky do an). Chi xoa cac nhanh phu `feature/...` sau khi
   da merge xong, de repo khoi roi.
7. Cuoi tuan 3 / dau tuan 4: merge `develop` vao `main` sau khi ca 4 module da tich hop va chay duoc.

---

## 4. Dinh nghia "Hoan thanh" (Definition of Done) cho 1 tinh nang

Mot tinh nang duoc tinh la xong khi:
- Chay dung nhu mo ta trong bang phan cong (xem tai lieu ke hoach du an)
- Co it nhat 1 unit test hoac test thu cong ghi lai trong PR
- Khong con `System.out.println` debug con sot lai
- Giao dien dung layout chung, khong vo bo cuc tren man hinh nho
- Da duoc it nhat 1 nguoi khac review

---

## 5. Xu ly loi - dung chung, khong tu che

Ca nhom dung chung bo exception trong `edu.hcmute.cnpm.cinema.exception` va bo bat loi tap trung
`GlobalExceptionHandler`. **Khong tu viet try/catch roi tu render trang loi rieng** cho module minh.

Cach dung (o tang **Service**, khong phai Controller):

```java
// Khong tim thay du lieu -> tu dong thanh trang 404
Movie movie = movieRepository.findById(movieId)
        .orElseThrow(() -> new ResourceNotFoundException("phim", movieId));

// Nghiep vu khong cho phep -> tu dong thanh trang 400
if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
    throw new InvalidBookingException("Suat chieu nay da bat dau, ban khong the dat ve nua.");
}

// Ghe bi nguoi khac giu mat (ADR-001) -> tu dong thanh trang 409
try {
    ticketRepository.saveAndFlush(ticket);
} catch (DataIntegrityViolationException ex) {
    throw new SeatAlreadyTakenException(showtimeId, seatId, ex);
}
```

| Exception | Ma HTTP | Dung khi nao |
|---|---|---|
| `ResourceNotFoundException` | 404 | Khong tim thay phim / phong / suat chieu / ve theo id |
| `InvalidBookingException` | 400 | Yeu cau sai nghiep vu (suat da chieu, ve het han giu, qua so ghe...) |
| `SeatAlreadyTakenException` | 409 | Ghe da co nguoi khac giu - **bat buoc dung sau khi catch `DataIntegrityViolationException`** |
| `BusinessException` | 400 | Cac loi nghiep vu khac chua co lop rieng |

Message truyen vao exception se hien **thang ra man hinh cho nguoi dung doc**, nen viet cau
hoan chinh, de hieu - dung ghi kieu "err code 3" hay ten class.

Neu request la AJAX (`X-Requested-With: XMLHttpRequest`) thi handler tra ve JSON
`{"success": false, "message": "..."}` thay vi trang HTML - tien cho man hinh chon ghe.

Muon bao thanh cong / bao loi nhe tren trang (khong phai exception) thi dung 2 hang so
`Constants.MODEL_SUCCESS_MESSAGE` / `Constants.MODEL_ERROR_MESSAGE` - fragment `fragments/alert.html`
da nhung san trong layout se tu hien thi.

## 6. Quy uoc viet test

- File test dat trong `src/test/java/edu/hcmute/cnpm/cinema/<module>/`, ten ket thuc bang
  `Test` (test don le) hoac `IntegrationTest` (test co dung database).
- **Moi test co dung database phai `extends IntegrationTestBase`** (o package `support`).
  Lop cha nay tu bat profile `test`, tu chan test chay nham vao database that, va tu xoa sach
  du lieu truoc moi test case.
- Tao du lieu mau bang `TestDataFactory` (cung o package `support`) thay vi tu `new` entity
  trong tung file test. Can kieu du lieu mau moi thi **them method moi** vao day, khong sua
  method nguoi khac dang dung.
- Ten method test: `should<KetQuaMongDoi>_when<TinhHuong>()`, kem `@DisplayName` mo ta bang
  tieng Viet de doc bao cao cho de.
- Truoc khi chay test lan dau: `sqlcmd -S localhost,1433 -U sa -C -i database\create-test-database.sql`.
  Chay test: `mvn test`.

## 7. Lien he / thac mac

Neu khong chac quy uoc ap dung the nao cho truong hop cu the, hoi truoc trong nhom chat thay vi
tu quyet dinh roi phai sua lai sau — do an chi co 4 tuan, sua di sua lai rat ton thoi gian.
