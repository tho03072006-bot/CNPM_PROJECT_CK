-- ================================================
-- Cinema Booking - NHOM05
-- Tao database RIENG danh cho test tich hop.
--
-- Vi sao can database rieng:
--   Test tich hop se XOA SACH du lieu truoc moi test case (xem IntegrationTestBase).
--   Neu chay thang tren "cinema_booking" thi se bay het du lieu dang dev cua ban.
--
-- Cach chay (1 lan duy nhat tren may moi nguoi):
--   sqlcmd -S localhost,1433 -U sa -C -i database\create-test-database.sql
-- Hoac mo file nay trong SSMS roi bam Execute.
--
-- Bang/cot trong database nay do Hibernate tu sinh ra tu cac @Entity
-- (profile "test" dat spring.jpa.hibernate.ddl-auto=update), khong can chay schema.sql.
-- ================================================
IF DB_ID('cinema_booking_test') IS NULL
    CREATE DATABASE cinema_booking_test;
GO
