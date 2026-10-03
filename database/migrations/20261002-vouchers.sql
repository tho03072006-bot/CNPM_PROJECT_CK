-- Lưu danh sách voucher trong database. Chạy lại không ghi đè mã đã có.
IF OBJECT_ID('dbo.vouchers', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.vouchers (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code VARCHAR(40) NOT NULL,
        title NVARCHAR(200) NOT NULL,
        discount_percent INT NOT NULL DEFAULT 0,
        discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
        minimum_ticket_subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
        maximum_discount DECIMAL(12,2) NOT NULL,
        starts_on DATE NULL,
        ends_on DATE NULL,
        active BIT NOT NULL DEFAULT 1,
        CONSTRAINT uq_vouchers_code UNIQUE (code),
        CONSTRAINT ck_vouchers_rules CHECK (
            discount_percent BETWEEN 0 AND 99 AND discount_amount >= 0
            AND minimum_ticket_subtotal >= 0 AND maximum_discount > 0
            AND ((discount_percent > 0 AND discount_amount = 0)
                OR (discount_percent = 0 AND discount_amount > 0))
            AND (starts_on IS NULL OR ends_on IS NULL OR ends_on >= starts_on)
        )
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.vouchers WHERE code = 'UTE10')
    INSERT INTO dbo.vouchers
        (code, title, discount_percent, discount_amount, minimum_ticket_subtotal, maximum_discount, active)
    VALUES ('UTE10', N'Giảm 10% vé xem phim', 10, 0, 100000, 30000, 1);

IF NOT EXISTS (SELECT 1 FROM dbo.vouchers WHERE code = 'UTE20K')
    INSERT INTO dbo.vouchers
        (code, title, discount_percent, discount_amount, minimum_ticket_subtotal, maximum_discount, active)
    VALUES ('UTE20K', N'Giảm 20.000 đồng cho đơn vé', 0, 20000, 200000, 20000, 1);
