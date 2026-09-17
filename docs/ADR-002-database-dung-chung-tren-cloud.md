# ADR-002: Database dung chung tren cloud cho ca nhom

- **Trang thai:** DA CHOT (Tho duyet 17/09/2026) - con lai la buoc tao tai khoan
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

**Ket luan:** Azure SQL free vua cho dung luong cao nhat (32 GB), vua **khong phai doi driver hay
schema**. Cac lua chon PostgreSQL deu nho hon VA bat doi cong nghe.

Noi thang cho de hinh dung: database cua do an nay (vai chuc phim, vai phong, vai nghin ve)
**chua toi 100 MB**. 32 GB la thua rat nhieu lan - dung luong khong phai thu can lo.
Thu that su gioi han la **thoi gian chay (compute)**: 100.000 vCore-giay/thang, o muc 0.5 vCore
tuong duong khoang **55 gio database thuc su hoat dong moi thang** cho ca 4 nguoi. Chi can nho
dong SSMS khi khong dung (xem muc 4) la du xai thoai mai trong 4 tuan lam do an.

## 3. Quyet dinh

**Chon phuong an 1: Azure SQL Database - goi free**, va dung theo mo hinh **2 tang**:

```
+---------------------------+       +------------------------------------+
|  May ca nhan (4 nguoi)    |       |  Azure SQL Database (goi free)     |
|  cinema_booking           |       |  cinema_booking (DUNG CHUNG)       |
|  ddl-auto = update        |       |  ddl-auto = validate               |
|  -> code hang ngay, offline|      |  -> tich hop, demo, du lieu that   |
+---------------------------+       +------------------------------------+
         profile: (mac dinh)                  profile: cloud
```

- **Hang ngay moi nguoi van code tren SQL Server local** (nhanh, khong ton han muc, khong can mang).
- **Database cloud la noi tich hop + demo**: chay bang `-Dspring-boot.run.profiles=cloud`.
- Tren cloud dat `spring.jpa.hibernate.ddl-auto=validate`: **Hibernate khong duoc tu y sua schema chung**.
  Muon doi bang/cot tren cloud thi sua `database/schema-azure.sql` roi bao ca nhom - tranh canh
  4 nguoi cung `update` lam schema chung bien dang.
- **Test tich hop KHONG chay tren cloud** (xem `docs/KE_HOACH_MODULE4.md`), vi test xoa sach du lieu
  truoc moi test case va se dot han muc vCore.

## 4. Han muc cua goi free (can biet truoc de khong bi hut hang)

Theo tai lieu chinh thuc cua Microsoft:

- **100.000 vCore-giay compute / thang / database** va toi da **32 GB du lieu** + 32 GB backup.
- Duoc tao toi da **10 database General Purpose** tren 1 subscription.
- Het han muc thang thi co 2 lua chon (dat luc tao database):
  - *Auto-pause the database until next month* - database tam dung den dau thang sau (**chon cai nay**, khong bao gio bi tinh tien);
  - *Continue using database for additional charges* - chay tiep va tinh tien.
- Han muc **tu reset dau moi thang duong lich**, mien phi tron doi subscription.
- Luu y: **goi "Azure for Students Starter" KHONG dung duoc** uu dai nay. Dung "Azure for College
  Students" hoac tai khoan Azure thuong.
- Khi da chon region cho database free dau tien thi cac database free sau **phai cung region** (chon
  **Southeast Asia** cho gan Viet Nam).

**Meo tiet kiem han muc (quan trong):** dong SSMS / Azure Data Studio khi khong dung. De Object
Explorer mo lien tuc se giu ket noi song, database khong tu ngu duoc va dot het vCore-giay.

## 5. Viec can lam de trien khai

1. **Tho** tao tai khoan Azure va tao database free (vao `aka.ms/azuresqlhub` -> *Create a database* ->
   **Start free**; kiem tra co banner "Free offer applied!" va Cost summary = 0 truoc khi bam Create).
2. Tao login/user rieng cho nhom (khong dung tai khoan admin de chia cho 4 nguoi).
3. Mo **Firewall rule** cho IP cua ca 4 thanh vien (Networking -> Add your client IPv4 address).
   IP nha mang o VN hay doi -> neu ket noi bao loi firewall thi vao them lai IP moi.
4. Chay `database/schema-azure.sql` tren database cloud.
5. Chay `database/seed-data.sql` de co du lieu mau chung (se bo sung o buoc sau).
6. Gui cho 3 thanh vien: server name, database name, user, password -> moi nguoi tu dien vao
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
- [The Best Free Database Tiers in 2026 (15 Compared) - FreeTier.co](https://freetier.co/articles/best-free-database-free-tiers-2026)
- [Top PostgreSQL Database Free Tiers in 2026 - Koyeb](https://www.koyeb.com/blog/top-postgresql-database-free-tiers-in-2026)
