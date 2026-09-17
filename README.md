# CNPM_PROJECT_CK_NHOM5 - He thong dat ve xem phim

Do an cuoi ky mon Cong nghe phan mem (CNPM) - Nhom 05.

## Thanh vien & phan cong

| Thanh vien | Vai tro | Module phu trach |
|---|---|---|
| **Tho** | Leader | Module 4: Kien truc dung chung, Testing, Quan ly GitHub/PM |
| **Tai** | Thanh vien | Module 1: Quan ly Phim / Phong chieu / Suat chieu (Admin CRUD) |
| **Thang** | Thanh vien | Module 2: Ghe & Ve - dat ve, seat-map, giu ghe (core booking) |
| **Thanh** | Thanh vien | Module 3: User/Auth, thanh toan, email, dashboard |

Chi tiet cong viec tung nguoi: xem tai lieu ke hoach du an (Claude Docs) hoac file `docs/PHAN_CONG.md` (se bo sung).

## Cong nghe su dung

- Java 21, Spring Boot 3.5.16
- Thymeleaf + thymeleaf-layout-dialect 4.0.1
- Spring Data JPA / Hibernate 6.x
- SQL Server (mssql-jdbc 13.6.0.jre11)
- Spring Mail (gui email xac nhan ve)
- Maven

## Cau truc thu muc

```
src/main/java/edu/hcmute/cnpm/cinema/
  entity/        - cac lop entity (Movie, Room, Seat, Showtime, Ticket, User...)
  repository/    - Spring Data JPA repository
  controller/    - Spring MVC controller
  constants/     - hang so dung chung
src/main/resources/
  templates/     - giao dien Thymeleaf (layout, fragments, cac trang)
  static/        - css, js, anh
  application.properties
database/
  schema.sql     - script tao database SQL Server
```

## Cach chay du an (local)

1. Cai SQL Server, tao database theo `database/schema.sql`.
2. Copy `application-secrets.properties.example` thanh `application-secrets.properties`
   (file nay da co trong `.gitignore`, KHONG commit len GitHub) va dien thong tin that:
   - `spring.datasource.url` / `username` / `password`
   - `spring.mail.username` / `spring.mail.password` (Gmail App Password)
3. Chay: `mvn spring-boot:run` (mac dinh port 8082).

## Quy uoc GitHub

- Nhanh `main`: code on dinh, chi merge tu `develop` qua Pull Request.
- Nhanh `develop`: nhanh tich hop chung.
- Nhanh tinh nang: `feature/<module>-<mo-ta-ngan>` (vd: `feature/module1-movie-crud`).
- Commit theo dang: `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`, `chore: ...`.
- Moi Pull Request can it nhat 1 thanh vien khac review truoc khi merge vao `develop`.

Chi tiet day du ke hoach du an (yeu cau, kien truc, ADR, timeline 4 tuan, phan cong chi tiet,
ke hoach kiem thu, rui ro...) duoc luu trong tai lieu ke hoach cua nhom.
