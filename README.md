# CNPM_PROJECT_CK_NHOM5 - He thong dat ve xem phim

Do an cuoi ky mon Cong nghe phan mem (CNPM) - Nhom 05.

## Thanh vien & phan cong

| Thanh vien | Vai tro | Module phu trach |
|---|---|---|
| **Tho** | Leader | Module 4: Kien truc dung chung, Testing, Quan ly GitHub/PM |
| **Tai** | Thanh vien | Module 1: Quan ly Phim / Phong chieu / Suat chieu (Admin CRUD) |
| **Thang** | Thanh vien | Module 2: Ghe & Ve - dat ve, seat-map, giu ghe (core booking) |
| **Thanh** | Thanh vien | Module 3: User/Auth, thanh toan, email, dashboard |

Chi tiet cong viec tung nguoi: xem tai lieu ke hoach du an hoac file `docs/PHAN_CONG.md` (se bo sung).

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

**Luu y khi cai dat:**

- `mvn -v` phai bao **Java version: 21.x**. Neu bao 1.8 hay 17 thi doi bien moi truong `JAVA_HOME`
  sang thu muc JDK 21 roi mo lai terminal - Spring Boot 3.5 khong build duoc bang Java 8/11.
- Neu ban cai SQL Server ban Express, `localhost,1433` van dung - khong can go `\SQLEXPRESS`.
  Kiem tra nhanh instance dang chay: `sqlcmd -S localhost,1433 -U sa -C -Q "SELECT @@SERVERNAME"`.
- **Dat thu muc du an o duong dan KHONG CO DAU tieng Viet** (vi du `D:\CNPM\Project_CK_NHOM05`).
  Neu duong dan co dau, `mvn spring-boot:run` se bao `Could not find or load main class` vi JVM
  tren Windows tieng Viet dung bang ma Cp1252, khong doc duoc duong dan co dau khi tao tien
  trinh con. (`mvn test` thi van chay binh thuong nen loi nay rat de bi hieu nham.)

## Cach chay test

Test tich hop chay tren **database rieng** `cinema_booking_test`, khong dung chung voi
`cinema_booking` (vi test xoa sach du lieu truoc moi test case).

1. Tao database test - chi can lam 1 lan:
   `sqlcmd -S localhost,1433 -U sa -C -i database\create-test-database.sql`
   (bang/cot se do Hibernate tu sinh ra tu cac `@Entity`, khong can chay `schema.sql`)
2. Chay toan bo test: `mvn test`

Moi test tich hop moi deu **ke thua `IntegrationTestBase`** va dung `TestDataFactory` de tao du lieu mau -
xem `src/test/java/edu/hcmute/cnpm/cinema/support/` va Muc 6 cua `CONTRIBUTING.md`.

## Database dung chung cua nhom (cloud)

Ngoai database tren may ca nhan, nhom dung them 1 database chung tren cloud de tich hop va demo.
Chi tiet + ly do chon: `docs/ADR-002-database-dung-chung-tren-cloud.md`.

Chay ung dung tren database chung: `mvn spring-boot:run -Dspring-boot.run.profiles=cloud`
(can co file `application-secrets-cloud.properties` - xem file `.example`).

## Quy uoc GitHub

- Nhanh `main`: code on dinh, chi merge tu `develop` qua Pull Request.
- Nhanh `develop`: nhanh gop chung cua 4 module.
- Moi thanh vien code tren nhanh ca nhan mang ten minh (`Minh_Thọ`, `Hữu_Tài`, `Hữu_Thắng`,
  `Tuấn_Thanh`), xong viec thi mo Pull Request vao `develop`.
- Tinh nang lam dai ngay thi tach them nhanh phu `feature/<module>-<mo-ta-ngan>`
  (vd: `feature/module1-movie-crud`) tu nhanh ca nhan.
- Commit theo dang: `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`, `chore: ...`.
- Moi Pull Request can it nhat 1 thanh vien khac review truoc khi merge vao `develop`.

Chi tiet day du ke hoach du an (yeu cau, kien truc, ADR, timeline 4 tuan, phan cong chi tiet,
ke hoach kiem thu, rui ro...) duoc luu trong tai lieu ke hoach cua nhom.
