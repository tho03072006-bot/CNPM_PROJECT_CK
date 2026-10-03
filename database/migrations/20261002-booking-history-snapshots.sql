-- Hữu Tài: bổ sung dữ liệu lịch sử, chạy được nhiều lần, không xóa bảng/cột/vé.
-- Chạy trên database mà ứng dụng đang dùng; cloud vẫn giữ ddl-auto=validate.
SET XACT_ABORT ON;
GO
IF OBJECT_ID('dbo.booking_orders', 'U') IS NULL
    THROW 50001, N'Thiếu bảng booking_orders. Hãy chạy schema nền trước.', 1;
GO
IF COL_LENGTH('dbo.booking_orders', 'discount_amount') IS NULL
    ALTER TABLE dbo.booking_orders ADD discount_amount DECIMAL(12,2) NOT NULL
        CONSTRAINT df_booking_orders_discount_amount DEFAULT 0 WITH VALUES;
GO
IF COL_LENGTH('dbo.booking_orders', 'applied_voucher_code') IS NULL
    ALTER TABLE dbo.booking_orders ADD applied_voucher_code NVARCHAR(50) NULL;
GO
IF COL_LENGTH('dbo.booking_orders', 'applied_voucher_name') IS NULL
    ALTER TABLE dbo.booking_orders ADD applied_voucher_name NVARCHAR(200) NULL;
GO
IF COL_LENGTH('dbo.booking_orders', 'ticket_snapshot') IS NULL
    ALTER TABLE dbo.booking_orders ADD ticket_snapshot NVARCHAR(MAX) NULL;
GO
-- Khôi phục dữ liệu còn tồn tại, không suy đoán ghế của vé đã bị xóa từ trước.
-- Cột giá gốc cần có trước backfill, kể cả chạy migration lịch sử trước ưu đãi.
IF COL_LENGTH('dbo.tickets', 'original_price') IS NULL
    ALTER TABLE dbo.tickets ADD original_price DECIMAL(12,2) NULL;
GO
UPDATE orders SET ticket_snapshot = (
    SELECT ticket.id AS ticketId,
           CONCAT(seat.seat_row, seat.seat_column) AS seatLabel,
           seat.seat_type AS seatType, COALESCE(ticket.original_price, ticket.price) AS price, ticket.held_at AS heldAt
    FROM dbo.tickets AS ticket JOIN dbo.seats AS seat ON seat.id = ticket.seat_id
    WHERE ticket.booking_order_id = orders.id
    ORDER BY ticket.id FOR JSON PATH
)
FROM dbo.booking_orders AS orders
WHERE orders.ticket_snapshot IS NULL
  AND EXISTS (SELECT 1 FROM dbo.tickets AS ticket WHERE ticket.booking_order_id = orders.id);
GO
UPDATE dbo.booking_orders
SET discount_amount = ticket_subtotal + concession_subtotal - total_amount
WHERE discount_amount = 0 AND ticket_subtotal + concession_subtotal > total_amount;
GO
-- Đơn nháp không còn vé giữ được đóng lại, vẫn giữ mã/thời điểm/tổng tiền cũ.
UPDATE orders SET status = 'CANCELLED'
FROM dbo.booking_orders AS orders
WHERE orders.status = 'DRAFT' AND NOT EXISTS (
    SELECT 1 FROM dbo.tickets AS ticket WHERE ticket.booking_order_id = orders.id AND ticket.status = 'HELD'
);
GO
