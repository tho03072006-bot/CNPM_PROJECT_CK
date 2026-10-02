-- Bổ sung MOMO_DEMO vào CHECK do Hibernate sinh trên database test cũ.
-- ddl-auto=update không tự cập nhật CHECK khi enum Java thêm giá trị.
-- Chạy sau khi database test đã có bảng:
-- sqlcmd -S localhost,1433 -d cinema_booking_test -U sa -C -b -f 65001 -i database/update-test-payment-method.sql
-- Chỉ đổi CHECK của payment_method, giữ nguyên dữ liệu và các ràng buộc khác.
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF RIGHT(DB_NAME(), 5) <> '_test'
    THROW 50001, N'Script này chỉ được chạy trên database có tên kết thúc bằng _test.', 1;

DECLARE @sql NVARCHAR(MAX) = N'';
SELECT @sql = @sql
    + N'ALTER TABLE ' + QUOTENAME(s.name) + N'.' + QUOTENAME(t.name)
    + N' DROP CONSTRAINT ' + QUOTENAME(cc.name) + N';'
    + N'ALTER TABLE ' + QUOTENAME(s.name) + N'.' + QUOTENAME(t.name)
    + N' WITH CHECK ADD CONSTRAINT ' + QUOTENAME(cc.name)
    + N' CHECK (payment_method IN (''COUNTER'', ''MOMO'', ''MOMO_DEMO''));'
FROM sys.check_constraints cc
JOIN sys.tables t ON t.object_id = cc.parent_object_id
JOIN sys.schemas s ON s.schema_id = t.schema_id
JOIN sys.columns c ON c.object_id = t.object_id AND c.column_id = cc.parent_column_id
WHERE s.name = N'dbo'
  AND t.name IN (N'tickets', N'booking_orders', N'ticket_refunds')
  AND c.name = N'payment_method'
  AND cc.is_system_named = 1
  AND cc.definition NOT LIKE N'%MOMO_DEMO%';

BEGIN TRANSACTION;
EXEC sys.sp_executesql @sql;
COMMIT TRANSACTION;
