-- Chạy trên SQL Server nếu môi trường dùng ddl-auto=validate/none.
-- ddl-auto=update tự bổ sung các cột này; script có thể chạy lại.
IF COL_LENGTH('booking_orders', 'voucher_code') IS NULL
    ALTER TABLE booking_orders ADD voucher_code VARCHAR(40) NULL;
IF COL_LENGTH('booking_orders', 'discount_amount') IS NULL
    ALTER TABLE booking_orders ADD discount_amount DECIMAL(12,2) NOT NULL
        CONSTRAINT df_booking_orders_discount DEFAULT 0 WITH VALUES;
IF COL_LENGTH('tickets', 'original_price') IS NULL
    ALTER TABLE tickets ADD original_price DECIMAL(12,2) NULL;
IF COL_LENGTH('ticket_refunds', 'original_price') IS NULL
    ALTER TABLE ticket_refunds ADD original_price DECIMAL(12,2) NULL;
