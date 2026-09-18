# ADR-002: Database dung chung tren cloud cho ca nhom

- **Trang thai:** DA CHOT - **da doi nha cung cap ngay 18/09/2026, xem muc 2c**
- **Ngay:** 17/09/2026
- **Nguoi de xuat:** Tho (Module 4 - Kien truc dung chung)
- **Lien quan:** ADR-001 (chong dat trung ghe bang UNIQUE (showtime_id, seat_id))

---

## 1. Van de

Hien tai moi thanh vien chay SQL Server rieng tren may minh. Hau qua:

- Du lieu 4 may khac nhau: Tai them phim moi, Thang khong thay phim do de test seat-map.
- Khong ai chac schema cua minh con giong schema cua nguoi khac -> den luc merge moi vo ra.
- Luc demo cho thay phai chay tren 1 may cu the, may do hong la hong ca buoi demo.
- Bao cao do an kho chung minh "he thong nhieu nguoi dung cung luc" neu chi co 1 may.

Can 1 database dung chung, mien phi (sinh vien khong co the tin dung de tra phi).

## 2. Cac phuong an da xet

| # | Phuong an | Uu diem | Nhuoc diem |
|---|---|---|---|
| 1 | **Azure SQL Database - goi free** | Van la SQL Server that, khong phai sua 1 dong code nao, khong doi driver `mssql-jdbc`, co ban mien phi vinh vien | Can tai khoan Azure; goi free co han muc thang; database tu tam dung khi het han muc |
| 2 | Moi nguoi giu local + dung chung file `seed-data.sql` | Don gian nhat, khong can mang | Van khong phai du lieu chung that; van lech nhau khi ai do sua tay |
| 3 | Doi sang PostgreSQL cloud (Neon / Supabase free) | Han muc free rong rai hon Azure | **Phai doi driver, doi dialect, doi kieu du lieu `NVARCHAR`/`DATETIME2`, sua lai ca `schema.sql`** - qua ton thoi gian voi do an 4 tuan, va de bai/nhom da thong nhat SQL Server |
| 4 | Hosting free co kem MSSQL (somee.com, MonsterASP...) | Dang ky nhanh | Khong on dinh, hay gioi han so ket noi dong thoi - dung chet dung luc demo; nhieu noi chan ket noi tu ngoai |

## 2b. So sanh dung luong mien phi (tieu chi Tho yeu cau: free + dung luong du xai)

Da ra soat lai cac goi free dang co (thang 09/2026):

| Dich vu | Loai database | Dung luong free | Dung duoc cho du an nay? |
|---|---|---|---|
| **Azure SQL Database (goi free)** | **SQL Server** | **32 GB / database, toi da 10 database** | **Co - va la goi free dung luong LON NHAT trong danh sach** |
| CockroachDB Serverless | PostgreSQL-compatible | 10 GiB | Duoc ve dung luong nhung phai doi toan bo sang PostgreSQL |
| Aiven | PostgreSQL / MySQL | 1 GB (da bi cat tu 5 GB xuong 1 GB) | Phai doi sang PostgreSQL |
| Neon | PostgreSQL | 0.5 GB / project | Phai doi sang PostgreSQL |
| Supabase | PostgreSQL | 500 MB | Phai doi sang PostgreSQL |

**Ket luan luc do (17/09):** Azure SQL free vua cho dung luong cao nhat (32 GB), vua **khong
phai doi driver hay schema**. Cac lua chon PostgreSQL deu nho hon VA bat doi cong nghe.

> **Ket luan nay da bi thay the ngay 18/09** vi Azure tu choi cho dang ky - xem muc 2c.
> Phan so sanh ben tren van giu lai de sau nay con biet da can nhac nhung gi.

Noi thang cho de hinh dung: database cua do an nay (vai chuc phim, vai phong, vai nghin ve)
**chua toi 100 MB**. 32 GB la thua rat nhieu lan - dung luong khong phai thu can lo.
Thu that su gioi han la **thoi gian chay (compute)**: 100.000 vCore-giay/thang, o muc 0.5 vCore
tuong duong khoang **55 gio database thuc su hoat dong moi thang** cho ca 4 nguoi. Chi can nho
dong SSMS khi khong dung (xem muc 4) la du xai thoai mai trong 4 tuan lam do an.

## 2c. Doi nha cung cap - Azure khong dang ky duoc (18/09/2026)

Ngay 18/09 Tho dang ky Azure thi bi tu choi:

> You're not eligible for an Azure free account

Nguyen nhan: loi nay noi ve **Azure free account** (goi dung thu 200 USD), thuong xay ra khi
email truong chua nam trong danh sach truong Microsoft cong nhan, hoac khu vuc Viet Nam chi
duoc xep vao goi **Azure for Students Starter** - ma goi Starter thi KHONG dung duoc uu dai
SQL Database free.

Co mot duong vong: goi SQL Database free van chay tren moi loai subscription, ke ca
Pay-As-You-Go. Nhung Pay-As-You-Go bat buoc gan the tin dung va chi can lo tay tao them tai
nguyen khac la bi tinh tien that. **Da loai phuong an nay** - do an sinh vien khong dang mang
rui ro do.

Luc dang ky Azure co mot giao dich **1 USD tai Microsoft Store** hien tren sao ke. Day la
khoan giu tam de xac minh the, khong phai phi dich vu, va da co thong bao **Huy giao dich**
ngay trong cung mot giay.

### Nha cung cap moi: MonsterASP.NET (goi Free)

| Tieu chi | MonsterASP.NET | Somee.com | Neon (PostgreSQL) |
|---|---|---|---|
| Loai database | **MSSQL 2025** | MSSQL Express | PostgreSQL |
| Dung luong free | **1 GB** | 30 MB | 3 GiB |
| Can the tin dung? | **Khong** | Khong | Khong |
| Phai sua code? | **Khong** | Khong | **Co, rat nhieu** |
| Datacenter | Chau Au | My | Nhieu noi |

Chon MonsterASP vi **van la SQL Server** nen khong phai sua mot dong code nao, 1 GB gap hon
10 lan nhu cau that (database do an chua toi 100 MB), dang ky khong can the, va co ho tro
bat remote access de noi tu ung dung.

**Vi sao khong chon Neon du dung luong rong hon:** doi sang PostgreSQL khong chi la doi driver.
Phai sua dialect, viet lai `schema.sql` (`NVARCHAR` -> `TEXT`, `DATETIME2` -> `TIMESTAMP`,
`IDENTITY` -> `GENERATED`), viet lai `seed-data.sql`, sua `columnDefinition="NVARCHAR(MAX)"`
trong `Movie.java`, va sua 2 cau truy van rieng cua SQL Server trong test. Te hon nua: **ca 4
nguoi se phai go SQL Server va cai PostgreSQL tren may** - vi neu may ca nhan chay SQL Server
con cloud chay PostgreSQL thi se sinh ra dung loai loi "chay may minh duoc, len cloud thi hong",
rat kho truy. Giua tuan thu hai cua do an 4 tuan, doi dong co database la qua mao hiem.

**Nhuoc diem phai chap nhan:** datacenter dat o chau Au nen do tre tu Viet Nam khoang
250-300ms, va goi free ghi ro "no guarantees or warranties". Vi vay van giu dung mo hinh 2 tang
o muc 3: **code hang ngay tren SQL Server o may ca nhan**, cloud chi dung de tich hop va demo.

**Neu MonsterASP chay chap chon** thi lui ve phuong an 2 o bang muc 2 (moi nguoi chay local,
dung chung `database/seed-data.sql` de du lieu giong nhau) chu KHONG doi sang PostgreSQL.

## 3. Quyet dinh

**Chon MonsterASP.NET goi Free** (xem muc 2c ve ly do doi tu Azure sang), dung theo
mo hinh **2 tang**:

```
+----------------------------+      +------------------------------------+
|  May ca nhan (4 nguoi)     |      |  MonsterASP.NET (goi Free)         |
|  SQL Server local          |      |  MSSQL dung chung, 1 GB            |
|  cinema_booking            |      |  ddl-auto = validate               |
|  ddl-auto = update         |      |  -> tich hop, demo, du lieu that   |
|  -> code hang ngay, offline|      |                                    |
+----------------------------+      +------------------------------------+
         profile: (mac dinh)                  profile: cloud
```

- **Hang ngay moi nguoi van code tren SQL Server local** (nhanh, khong ton han muc, khong can mang).
- **Database cloud la noi tich hop + demo**: chay bang `-Dspring-boot.run.profiles=cloud`.
- Tren cloud dat `spring.jpa.hibernate.ddl-auto=validate`: **Hibernate khong duoc tu y sua schema chung**.
  Muon doi bang/cot tren cloud thi sua `database/schema-cloud.sql` roi bao ca nhom - tranh canh
  4 nguoi cung `update` lam schema chung bien dang.
- **Test tich hop KHONG chay tren cloud** (xem `docs/KE_HOACH_MODULE4.md`), vi test xoa sach du lieu
  truoc moi test case va se dot han muc vCore.

## 4. Gioi han cua goi Free MonsterASP (can biet truoc)

- **5 database**, tong **1 GB** dung luong. Du an nay chua toi 100 MB nen thoai mai.
- **Khong can the tin dung**, khong co ngay het han.
- **Remote access mac dinh bi TAT**, phai vao tung database bat thu cong.
- Datacenter dat o **chau Au** -> do tre tu Viet Nam khoang **250-300ms**. Dung de tich hop
  va demo thi chap nhan duoc, nhung **dung lay lam database code hang ngay** - van giu
  SQL Server o may ca nhan cho viec do.
- Goi Free ghi ro **"no guarantees or warranties"**: co the cham hoac gian doan bat ky luc nao.
  Vi vay **truoc buoi demo phai chay thu truoc it nhat 1 ngay**, va luon co san phuong an du
  phong la chay local voi `database/seed-data.sql`.
- Goi Free gioi han so ket noi dong thoi (khong cong bo con so cu the) -> `application-cloud.properties`
  da dat `maximum-pool-size=5` cho moi may, 4 nguoi cung chay la khoang 20 ket noi.
  Neu bi tu choi ket noi thi ha so nay xuong.

**Meo:** dong SSMS khi khong dung. Goi free nao cung gioi han ket noi, de Object Explorer
mo lien tuc la chiem mat mot ket noi cua ca nhom.

## 5. Viec can lam de trien khai

1. **Tho** dang ky tai khoan tai `monsterasp.net`, chon goi **Free** (da xong 18/09,
   goi Free cho toi 5 database).
2. Trong trang quan tri: **Databases** -> **Create** -> chon loai **MSSQL** (khong phai MySQL).
   Ghi lai ten database that ma he thong sinh ra - thuong KHONG phai `cinema_booking` ma la
   mot ten dang `db_xxxxx`.
3. Mo database vua tao -> muc **Users and remote** -> bam **Enabled** de bat remote access.
   Mac dinh remote access bi TAT, khong bat thi ung dung khong noi vao duoc.
4. Ngay tai man hinh do, chep lai: **server**, **login**, **password**.
5. Chay `database/schema-cloud.sql` roi `database/seed-data.sql` len database do.
6. Gui cho 3 thanh vien: server, ten database, user, password -> moi nguoi tu dien vao
   `application-secrets-cloud.properties` tren may minh (**khong bo vao Git, khong gui trong file code**).

## 6. He qua

**Tich cuc**
- Ca nhom nhin chung 1 bo du lieu; demo chay tu bat ky may nao.
- Khong phai sua code/dependency: van `mssql-jdbc`, van `SQLServerDialect`.
- Co the demo that canh "2 may cung dat 1 ghe" - dung y ADR-001.

**Tieu cuc / rui ro**
- Phu thuoc mang; mat mang la khong code duoc tren profile cloud (nen van giu local lam chinh).
- Het han muc giua thang -> database ngu den thang sau. Phai dat canh bao khi con < 10.000 vCore-giay.
- Phai nho them IP vao firewall moi khi doi mang (o truong / o nha khac IP).

**Nguon tham khao**
- [Try Azure SQL Database for Free - Microsoft Learn](https://learn.microsoft.com/en-us/azure/azure-sql/database/free-offer?view=azuresql)
- [Azure SQL Database free offer FAQ](https://learn.microsoft.com/en-us/azure/azure-sql/database/free-offer-faq?view=azuresql)
- [MonsterASP.NET - goi hosting mien phi](https://www.monsterasp.net/)
- [MonsterASP - Bat remote access cho database](https://help.monsterasp.net/books/databases/page/remote-access-for-database)
- [MonsterASP - Ket noi bang SQL Server Management Studio](https://help.monsterasp.net/books/databases/page/sql-server-management-studio-ssms)
- [The Best Free Database Tiers in 2026 (15 Compared) - FreeTier.co](https://freetier.co/articles/best-free-database-free-tiers-2026)
- [Top PostgreSQL Database Free Tiers in 2026 - Koyeb](https://www.koyeb.com/blog/top-postgresql-database-free-tiers-in-2026)
