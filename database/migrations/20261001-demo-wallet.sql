-- © Nhóm 8. Ví MoMo GIẢ LẬP; chạy lại an toàn, không sửa vé cũ.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID('dbo.demo_payments','U') IS NULL
BEGIN
    CREATE TABLE dbo.demo_payments (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        public_id VARCHAR(36) NOT NULL CONSTRAINT uq_demo_payments_public_id UNIQUE,
        token_hash VARCHAR(64) NOT NULL,
        user_id BIGINT NOT NULL CONSTRAINT fk_demo_payments_user REFERENCES dbo.users(id),
        showtime_id BIGINT NOT NULL CONSTRAINT fk_demo_payments_showtime REFERENCES dbo.showtimes(id),
        ticket_ids VARCHAR(200) NOT NULL,
        seat_labels NVARCHAR(100) NOT NULL,
        movie_title NVARCHAR(200) NOT NULL,
        room_name NVARCHAR(50) NOT NULL,
        showtime_start DATETIME2 NOT NULL,
        amount BIGINT NOT NULL CONSTRAINT ck_demo_payments_amount CHECK(amount > 0 AND amount <= 1000000000),
        status VARCHAR(20) NOT NULL CONSTRAINT ck_demo_payments_status
            CHECK(status IN ('PENDING','SUCCESS','CANCELLED','EXPIRED','INVALIDATED')),
        created_at DATETIME2 NOT NULL,
        expires_at DATETIME2 NOT NULL,
        paid_at DATETIME2 NULL,
        CONSTRAINT ck_demo_payments_time CHECK(expires_at > created_at),
        CONSTRAINT ck_demo_payments_paid CHECK(
            (status = 'SUCCESS' AND paid_at IS NOT NULL) OR (status <> 'SUCCESS' AND paid_at IS NULL))
    );
    CREATE INDEX ix_demo_payments_owner ON dbo.demo_payments(user_id,showtime_id,status);
END;

-- Mở rộng đúng enum phương thức thanh toán; không nới lỏng ràng buộc khác.
DECLARE @table SYSNAME, @constraint SYSNAME, @definition NVARCHAR(MAX), @normalized NVARCHAR(MAX), @sql NVARCHAR(MAX);
DECLARE legacy CURSOR LOCAL FAST_FORWARD FOR
SELECT OBJECT_NAME(parent_object_id), name, definition
FROM sys.check_constraints
WHERE parent_object_id IN (OBJECT_ID('dbo.tickets'),OBJECT_ID('dbo.booking_orders'),OBJECT_ID('dbo.ticket_refunds'))
  AND definition LIKE '%payment_method%' AND definition NOT LIKE '%MOMO_DEMO%';
OPEN legacy;
FETCH NEXT FROM legacy INTO @table,@constraint,@definition;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @normalized=UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(@definition,
        '[',''),']',''),'(',''),')',''),' ',''),CHAR(13),''),CHAR(10),''));
    IF @normalized NOT IN ('PAYMENT_METHOD=''MOMO''ORPAYMENT_METHOD=''COUNTER''','PAYMENT_METHOD=''COUNTER''ORPAYMENT_METHOD=''MOMO''')
        THROW 51000, 'Unexpected payment_method constraint: migration cancelled for manual review.', 1;
    SET @sql=N'ALTER TABLE dbo.'+QUOTENAME(@table)+N' DROP CONSTRAINT '+QUOTENAME(@constraint);
    EXEC sys.sp_executesql @sql;
    FETCH NEXT FROM legacy INTO @table,@constraint,@definition;
END;
CLOSE legacy;
DEALLOCATE legacy;
DECLARE methods CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM sys.tables
WHERE schema_id=SCHEMA_ID('dbo') AND name IN ('tickets','booking_orders','ticket_refunds');
OPEN methods;
FETCH NEXT FROM methods INTO @table;
WHILE @@FETCH_STATUS=0
BEGIN
    SET @constraint=N'ck_'+@table+N'_payment_method_demo';
    IF OBJECT_ID(N'dbo.'+@constraint,'C') IS NULL
    BEGIN
        SET @sql=N'ALTER TABLE dbo.'+QUOTENAME(@table)+N' WITH CHECK ADD CONSTRAINT '+QUOTENAME(@constraint)
            +N' CHECK(payment_method IS NULL OR payment_method IN (''COUNTER'',''MOMO'',''MOMO_DEMO''))';
        EXEC sys.sp_executesql @sql;
    END;
    FETCH NEXT FROM methods INTO @table;
END;
CLOSE methods;
DEALLOCATE methods;
COMMIT TRANSACTION;
