-- Bổ sung mã vé 8 số; không sửa hoặc xóa dữ liệu vé, đơn hàng và hoàn tiền.
IF OBJECT_ID(N'dbo.ticket_public_codes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ticket_public_codes (
        ticket_id BIGINT NOT NULL CONSTRAINT pk_ticket_public_codes PRIMARY KEY,
        public_code VARCHAR(8) NOT NULL CONSTRAINT uq_ticket_public_code UNIQUE,
        created_at DATETIME2(6) NOT NULL,
        CONSTRAINT ck_ticket_public_code_format CHECK (
            LEN(public_code) = 8 AND public_code COLLATE Latin1_General_100_BIN2
            LIKE '[1-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]')
    );
END;
-- Ứng dụng cấp mã bằng SecureRandom và khóa SQL Server dùng chung giữa mọi máy.
-- Không xóa bản ghi bảng này khi hủy vé: mã đã cấp không được tái sử dụng.

-- Ghi nhận hóa đơn gốc cho vé hủy; thêm cột nullable, giữ nguyên chứng từ hiện có.
IF COL_LENGTH(N'dbo.ticket_refunds', N'booking_order_id') IS NULL
    ALTER TABLE dbo.ticket_refunds ADD booking_order_id BIGINT NULL;
-- Chỉ ghép chứng từ lịch sử khi có duy nhất một hóa đơn khớp. Chạy lặp vẫn an toàn.
EXEC(N'UPDATE r SET booking_order_id = matches.order_id
FROM dbo.ticket_refunds r
CROSS APPLY (SELECT MIN(o.id) order_id, COUNT(*) amount FROM dbo.booking_orders o
 WHERE o.user_id = r.user_id AND o.showtime_id = r.showtime_id AND o.status = ''PAID''
 AND o.paid_at = r.paid_at AND (o.payment_ref = r.payment_ref OR (o.payment_ref IS NULL AND r.payment_ref IS NULL))) matches
WHERE r.booking_order_id IS NULL AND matches.amount = 1');
