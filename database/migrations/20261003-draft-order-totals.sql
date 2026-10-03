-- Đồng bộ tổng tiền đơn NHÁP cũ sau khi tích hợp ưu đãi.
-- Giữ nguyên giá vé/combo, mức giảm, snapshot và toàn bộ đơn đã trả/đã hủy.
SET XACT_ABORT ON;
GO
UPDATE dbo.booking_orders
SET total_amount = ticket_subtotal + concession_subtotal - discount_amount
WHERE status = 'DRAFT'
  AND discount_amount >= 0
  AND discount_amount <= ticket_subtotal + concession_subtotal
  AND total_amount <> ticket_subtotal + concession_subtotal - discount_amount;
GO
