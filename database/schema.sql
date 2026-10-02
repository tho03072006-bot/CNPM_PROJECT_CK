-- ================================================
-- UTE Cinema - Schema cho SQL Server trên máy cá nhân
--
-- Cách chạy:
--   sqlcmd -S localhost,1433 -U sa -C -f 65001 -i database\schema.sql
-- Hoặc mở file này trong SSMS rồi bấm Execute.
--
-- Chạy xong thì nạp dữ liệu mẫu bằng database/seed-data.sql để cả nhóm
-- test trên cùng một bộ dữ liệu.
--
-- Bản dùng cho database chung trên cloud là database/schema-cloud.sql
-- (không có CREATE DATABASE và USE vì nhà cung cấp đã tạo sẵn database).
-- ================================================
IF DB_ID('cinema_booking') IS NULL
    CREATE DATABASE cinema_booking;
GO
USE cinema_booking;
GO

IF OBJECT_ID('dbo.users', 'U') IS NULL
CREATE TABLE users (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    full_name       NVARCHAR(150)   NOT NULL,
    email           NVARCHAR(150)   NOT NULL UNIQUE,
    phone           NVARCHAR(20)    NULL,
    password_hash   NVARCHAR(255)   NOT NULL,
    role            NVARCHAR(20)    NOT NULL DEFAULT 'CUSTOMER', -- ADMIN | STAFF | CUSTOMER
    created_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);

IF OBJECT_ID('dbo.movies', 'U') IS NULL
CREATE TABLE movies (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    title           NVARCHAR(200)   NOT NULL,
    genre           NVARCHAR(100)   NULL,
    duration_min    INT             NOT NULL,
    description     NVARCHAR(MAX)   NULL,
    poster_url      NVARCHAR(500)   NULL,
    age_rating      NVARCHAR(10)    NULL, -- Nhãn phân loại hiện hành: P, K, T13, T16, T18, C
    is_active       BIT             NOT NULL DEFAULT 1,
    created_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);

IF OBJECT_ID('dbo.rooms', 'U') IS NULL
CREATE TABLE rooms (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(50)    NOT NULL,   -- ví dụ: Phòng 1, Phòng VIP
    total_rows      INT             NOT NULL,
    total_columns   INT             NOT NULL
);

IF OBJECT_ID('dbo.seats', 'U') IS NULL
CREATE TABLE seats (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    room_id         BIGINT          NOT NULL FOREIGN KEY REFERENCES rooms(id),
    seat_row        NVARCHAR(5)     NOT NULL,  -- A, B, C...
    seat_column     INT             NOT NULL,
    seat_type       NVARCHAR(20)    NOT NULL DEFAULT 'NORMAL', -- NORMAL | VIP | COUPLE
    CONSTRAINT uq_room_seat UNIQUE (room_id, seat_row, seat_column)
);

IF OBJECT_ID('dbo.showtimes', 'U') IS NULL
CREATE TABLE showtimes (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    movie_id        BIGINT          NOT NULL FOREIGN KEY REFERENCES movies(id),
    room_id         BIGINT          NOT NULL FOREIGN KEY REFERENCES rooms(id),
    start_time      DATETIME2       NOT NULL,
    end_time        DATETIME2       NOT NULL,
    base_price      DECIMAL(10,2)   NOT NULL
);

IF OBJECT_ID('dbo.tickets', 'U') IS NULL
CREATE TABLE tickets (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    showtime_id     BIGINT          NOT NULL FOREIGN KEY REFERENCES showtimes(id),
    seat_id         BIGINT          NOT NULL FOREIGN KEY REFERENCES seats(id),
    user_id         BIGINT          NOT NULL FOREIGN KEY REFERENCES users(id),
    status          NVARCHAR(20)    NOT NULL DEFAULT 'HELD', -- HELD | PAID | CANCELLED | EXPIRED
    price           DECIMAL(10,2)   NOT NULL,
    held_at         DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),
    paid_at         DATETIME2       NULL,
    -- Chống đặt trùng ghế cho cùng một suất chiếu - KHÔNG được xoá ràng buộc này
    CONSTRAINT uq_showtime_seat UNIQUE (showtime_id, seat_id)
);

GO

-- ----------------------------------------------------------------
-- Bổ sung 27/09/2026: thanh toán MoMo, huỷ vé hoàn tiền, soát vé vào phòng
-- (ADR-3 trong docs/DATABASE.md).
-- Chỉ THÊM cột cho phép NULL và thêm bảng, không sửa hay xoá gì của cái cũ. Database tạo
-- từ trước ngày này chỉ cần chạy lại cả file là được nâng cấp; code cũ vẫn chạy bình thường.
-- ----------------------------------------------------------------
IF COL_LENGTH('dbo.tickets', 'payment_method') IS NULL
    ALTER TABLE tickets ADD payment_method NVARCHAR(20) NULL;   -- COUNTER | MOMO | MOMO_DEMO, NULL coi như tại quầy
GO
IF COL_LENGTH('dbo.tickets', 'payment_ref') IS NULL
    ALTER TABLE tickets ADD payment_ref NVARCHAR(100) NULL;     -- mã giao dịch MoMo (transId)
GO
IF COL_LENGTH('dbo.tickets', 'checked_in_at') IS NULL
    ALTER TABLE tickets ADD checked_in_at DATETIME2 NULL;       -- lúc nhân viên soát vé cho vào phòng
GO

-- Biên nhận hoàn tiền khi khách huỷ vé đã thanh toán. Cố ý KHÔNG có khoá ngoại và chép luôn
-- tên phim, phòng, ghế: đây là chứng từ tiền bạc, phim đổi tên hay suất bị xoá sau này thì
-- biên nhận vẫn phải đọc được.
IF OBJECT_ID('dbo.ticket_refunds', 'U') IS NULL
CREATE TABLE ticket_refunds (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    original_ticket_id  BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    showtime_id         BIGINT          NOT NULL,
    movie_title         NVARCHAR(200)   NOT NULL,
    room_name           NVARCHAR(50)    NOT NULL,
    seat_label          NVARCHAR(10)    NOT NULL,
    showtime_start      DATETIME2       NOT NULL,
    paid_price          DECIMAL(10,2)   NOT NULL,
    refund_percent      INT             NOT NULL,
    refund_amount       DECIMAL(10,2)   NOT NULL,
    payment_method      NVARCHAR(20)    NULL,       -- COUNTER | MOMO | MOMO_DEMO
    payment_ref         NVARCHAR(100)   NULL,       -- mã giao dịch lúc trả tiền
    refund_ref          NVARCHAR(100)   NULL,       -- mã giao dịch hoàn tiền bên MoMo
    paid_at             DATETIME2       NULL,
    refunded_at         DATETIME2       NOT NULL
);
GO
-- Trang "Vé của tôi" liệt kê biên nhận theo khách.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_ticket_refunds_user_id'
               AND object_id = OBJECT_ID('dbo.ticket_refunds'))
    CREATE INDEX ix_ticket_refunds_user_id ON ticket_refunds(user_id);
GO

-- Tài khoản admin mặc định. Mật khẩu ở đây CHƯA HASH, chỉ là dữ liệu mẫu để test nhanh.
-- Module 3 làm xong phần đăng nhập thì phải thay bằng chuỗi hash BCrypt.
-- Mat khau cua ca 3 tai khoan mau deu la: 123456
-- Chuoi duoi day la ban da bam bang BCrypt (Module 3 dung BCryptPasswordEncoder
-- de kiem tra dang nhap). Tuyet doi khong luu mat khau tho vao cot nay.
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@utecinema.local')
    INSERT INTO users (full_name, email, password_hash, role)
    VALUES (N'Quản trị viên', 'admin@utecinema.local', '$2a$10$0hP214zsHpy5UeMXorB1bOze53HL8258/nZV3SGW9qh7HqNWm/jqu', 'ADMIN');
GO

-- ----------------------------------------------------------------
-- Bổ sung 30/09/2026: bắp nước, hóa đơn điện tử và chăm sóc khách hàng.
-- Các lệnh đều chạy lại an toàn trên database đã có dữ liệu.
-- ----------------------------------------------------------------
IF OBJECT_ID('dbo.concession_products', 'U') IS NULL
CREATE TABLE concession_products (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    code            VARCHAR(40)     NOT NULL UNIQUE,
    name            NVARCHAR(150)   NOT NULL,
    description     NVARCHAR(500)   NULL,
    price           DECIMAL(10,2)   NOT NULL,
    icon            NVARCHAR(12)    NOT NULL DEFAULT N'🍿',
    active          BIT             NOT NULL DEFAULT 1,
    display_order   INT             NOT NULL DEFAULT 0
);
GO

IF OBJECT_ID('dbo.booking_orders', 'U') IS NULL
CREATE TABLE booking_orders (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    receipt_code        VARCHAR(40)      NOT NULL UNIQUE,
    user_id             BIGINT           NOT NULL FOREIGN KEY REFERENCES users(id),
    showtime_id         BIGINT           NOT NULL FOREIGN KEY REFERENCES showtimes(id),
    customer_name       NVARCHAR(150)    NOT NULL,
    customer_email      VARCHAR(150)     NOT NULL,
    movie_title         NVARCHAR(200)    NOT NULL,
    room_name           NVARCHAR(50)     NOT NULL,
    showtime_start      DATETIME2        NOT NULL,
    status              VARCHAR(20)      NOT NULL DEFAULT 'DRAFT',
    ticket_subtotal     DECIMAL(12,2)    NOT NULL DEFAULT 0,
    concession_subtotal DECIMAL(12,2)    NOT NULL DEFAULT 0,
    total_amount        DECIMAL(12,2)    NOT NULL DEFAULT 0,
    payment_method      VARCHAR(20)      NULL,
    payment_ref         VARCHAR(100)     NULL,
    created_at          DATETIME2        NOT NULL DEFAULT SYSUTCDATETIME(),
    paid_at             DATETIME2        NULL
);
GO

IF OBJECT_ID('dbo.booking_order_items', 'U') IS NULL
CREATE TABLE booking_order_items (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_order_id    BIGINT          NOT NULL FOREIGN KEY REFERENCES booking_orders(id) ON DELETE CASCADE,
    product_id          BIGINT          NOT NULL FOREIGN KEY REFERENCES concession_products(id),
    product_name        NVARCHAR(150)   NOT NULL,
    unit_price          DECIMAL(10,2)   NOT NULL,
    quantity            INT             NOT NULL,
    line_total          DECIMAL(12,2)   NOT NULL,
    CONSTRAINT ck_booking_item_quantity CHECK (quantity BETWEEN 1 AND 10)
);
GO

IF COL_LENGTH('dbo.tickets', 'booking_order_id') IS NULL
    ALTER TABLE tickets ADD booking_order_id BIGINT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_tickets_booking_order')
    ALTER TABLE tickets ADD CONSTRAINT fk_tickets_booking_order
        FOREIGN KEY (booking_order_id) REFERENCES booking_orders(id);
GO

IF OBJECT_ID('dbo.support_conversations', 'U') IS NULL
CREATE TABLE support_conversations (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id         BIGINT          NOT NULL FOREIGN KEY REFERENCES users(id),
    assigned_staff_id   BIGINT          NULL FOREIGN KEY REFERENCES users(id),
    subject             NVARCHAR(200)   NOT NULL,
    category            VARCHAR(30)     NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'WAITING_STAFF',
    created_at          DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

IF OBJECT_ID('dbo.support_messages', 'U') IS NULL
CREATE TABLE support_messages (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    conversation_id     BIGINT          NOT NULL FOREIGN KEY REFERENCES support_conversations(id) ON DELETE CASCADE,
    sender_id           BIGINT          NOT NULL FOREIGN KEY REFERENCES users(id),
    content             NVARCHAR(2000)  NOT NULL,
    sent_at             DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_booking_orders_user_id'
               AND object_id = OBJECT_ID('dbo.booking_orders'))
    CREATE INDEX ix_booking_orders_user_id ON booking_orders(user_id, created_at DESC);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_support_conversations_customer'
               AND object_id = OBJECT_ID('dbo.support_conversations'))
    CREATE INDEX ix_support_conversations_customer ON support_conversations(customer_id, updated_at DESC);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_support_conversations_status'
               AND object_id = OBJECT_ID('dbo.support_conversations'))
    CREATE INDEX ix_support_conversations_status ON support_conversations(status, updated_at DESC);
GO

-- ----------------------------------------------------------------
-- Bổ sung 02/10/2026: kho bắp nước.
--   - Món lẻ có tồn kho (stock_quantity) và ngưỡng báo sắp hết (low_stock_threshold).
--   - Combo không có kho riêng, ghép từ món lẻ theo bảng concession_combo_items.
--   - Mọi lần nhập kho / xuất hủy / kiểm kê / bán đều ghi vào concession_stock_movements.
-- Các lệnh đều chạy lại an toàn trên database đã có dữ liệu.
-- ----------------------------------------------------------------
IF COL_LENGTH('dbo.concession_products', 'stock_quantity') IS NULL
    ALTER TABLE concession_products ADD stock_quantity INT NOT NULL
        CONSTRAINT df_concession_products_stock DEFAULT 0;
GO
IF COL_LENGTH('dbo.concession_products', 'low_stock_threshold') IS NULL
    ALTER TABLE concession_products ADD low_stock_threshold INT NOT NULL
        CONSTRAINT df_concession_products_low_stock DEFAULT 10;
GO
-- Chốt chặn cuối ở database: tồn kho không bao giờ âm, kể cả khi code có lỗi.
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_concession_products_stock')
    ALTER TABLE concession_products ADD CONSTRAINT ck_concession_products_stock CHECK (stock_quantity >= 0);
GO

IF OBJECT_ID('dbo.concession_combo_items', 'U') IS NULL
CREATE TABLE concession_combo_items (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    combo_product_id        BIGINT  NOT NULL FOREIGN KEY REFERENCES concession_products(id),
    component_product_id    BIGINT  NOT NULL FOREIGN KEY REFERENCES concession_products(id),
    quantity                INT     NOT NULL DEFAULT 1,
    CONSTRAINT uq_combo_component UNIQUE (combo_product_id, component_product_id),
    CONSTRAINT ck_combo_item_quantity CHECK (quantity BETWEEN 1 AND 20),
    CONSTRAINT ck_combo_item_not_self CHECK (combo_product_id <> component_product_id)
);
GO

IF OBJECT_ID('dbo.concession_stock_movements', 'U') IS NULL
CREATE TABLE concession_stock_movements (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id          BIGINT          NOT NULL FOREIGN KEY REFERENCES concession_products(id),
    type                VARCHAR(20)     NOT NULL,       -- IMPORT | WRITE_OFF | STOCKTAKE | SALE
    quantity_change     INT             NOT NULL,       -- dương là tăng, âm là giảm
    quantity_after      INT             NOT NULL,
    note                NVARCHAR(255)   NULL,
    reference           VARCHAR(40)     NULL,           -- mã hóa đơn, chỉ có ở lần bán hàng
    actor_id            BIGINT          NULL FOREIGN KEY REFERENCES users(id),
    actor_name          NVARCHAR(150)   NULL,
    created_at          DATETIME2       NOT NULL DEFAULT SYSDATETIME()
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_stock_movements_product'
               AND object_id = OBJECT_ID('dbo.concession_stock_movements'))
    CREATE INDEX ix_stock_movements_product ON concession_stock_movements(product_id, created_at DESC);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_stock_movements_created'
               AND object_id = OBJECT_ID('dbo.concession_stock_movements'))
    CREATE INDEX ix_stock_movements_created ON concession_stock_movements(created_at DESC);
GO
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
