-- ================================================
-- Cinema Booking - NHÓM 05
-- Tạo database RIÊNG dành cho test tích hợp.
--
-- Vì sao cần database riêng:
--   Test tích hợp sẽ XOÁ SẠCH dữ liệu trước mỗi test case (xem IntegrationTestBase).
--   Nếu chạy thẳng trên "cinema_booking" thì sẽ bay hết dữ liệu đang dev của bạn.
--
-- Cách chạy (một lần duy nhất trên máy mỗi người):
--   sqlcmd -S localhost,1433 -U sa -C -f 65001 -i database\create-test-database.sql
-- Hoặc mở file này trong SSMS rồi bấm Execute.
--
-- Bảng và cột trong database này do Hibernate tự sinh ra từ các @Entity
-- (profile "test" đặt spring.jpa.hibernate.ddl-auto=update), không cần chạy schema.sql.
-- ================================================
IF DB_ID('cinema_booking_test') IS NULL
    CREATE DATABASE cinema_booking_test;
GO
