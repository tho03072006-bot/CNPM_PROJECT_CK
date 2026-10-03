-- Tên voucher và tên đã chốt trên đơn đều hỗ trợ 200 ký tự.
-- Chỉ mở rộng cột; không cập nhật hay xóa dữ liệu. Chạy lại an toàn.
SET XACT_ABORT ON;
GO
IF OBJECT_ID('dbo.booking_orders', 'U') IS NULL
    THROW 50001, N'Thiếu bảng booking_orders.', 1;
GO
IF COL_LENGTH('dbo.booking_orders', 'applied_voucher_name') IS NULL
    ALTER TABLE dbo.booking_orders ADD applied_voucher_name NVARCHAR(200) NULL;
ELSE IF COL_LENGTH('dbo.booking_orders', 'applied_voucher_name') < 400
    ALTER TABLE dbo.booking_orders ALTER COLUMN applied_voucher_name NVARCHAR(200) NULL;
GO
