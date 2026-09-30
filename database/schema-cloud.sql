-- ================================================================
-- UTE Cinema - Schema cho database dùng chung trên cloud
--
-- Khác gì so với database/schema.sql (bản chạy ở máy cá nhân):
--   - Nhà cung cấp cloud đã tạo sẵn database cho mình, nên file này KHÔNG có
--     lệnh "CREATE DATABASE" và cũng không có lệnh "USE <database>".
--     => Kết nối thẳng vào database được cấp rồi chạy file này.
--   - Các lệnh đều có kiểm tra "nếu chưa tồn tại" -> chạy lại nhiều lần không bị lỗi.
--
-- Cách chạy (thay <...> bằng thông tin Thọ gửi trong nhóm chat):
--   sqlcmd -S <server> -d <ten-database> -U <login> -P <password> -C -f 65001 -i database\schema-cloud.sql
--   (hoặc mở bằng SSMS, chọn đúng database rồi Execute)
--
-- LƯU Ý: khi đã chạy file này thì trên profile "cloud" Hibernate chạy ở chế độ
-- ddl-auto=validate - tức là Hibernate KHÔNG được tự sửa schema chung. Muốn thêm/sửa
-- bảng thì sửa file này + database/schema.sql rồi báo cả nhóm (xem docs/DATABASE.md).
-- ================================================================

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
GO

IF OBJECT_ID('dbo.movies', 'U') IS NULL
CREATE TABLE movies (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    title           NVARCHAR(200)   NOT NULL,
    genre           NVARCHAR(100)   NULL,
    duration_min    INT             NOT NULL,
    description     NVARCHAR(MAX)   NULL,
    poster_url      NVARCHAR(500)   NULL,
    age_rating      NVARCHAR(10)    NULL, -- P, C13, C16, C18
    is_active       BIT             NOT NULL DEFAULT 1,
    created_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

IF OBJECT_ID('dbo.rooms', 'U') IS NULL
CREATE TABLE rooms (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(50)    NOT NULL,   -- vd: Phong 1, Phong VIP
    total_rows      INT             NOT NULL,
    total_columns   INT             NOT NULL
);
GO

IF OBJECT_ID('dbo.seats', 'U') IS NULL
CREATE TABLE seats (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    room_id         BIGINT          NOT NULL FOREIGN KEY REFERENCES rooms(id),
    seat_row        NVARCHAR(5)     NOT NULL,  -- A, B, C...
    seat_column     INT             NOT NULL,
    seat_type       NVARCHAR(20)    NOT NULL DEFAULT 'NORMAL', -- NORMAL | VIP | COUPLE
    CONSTRAINT uq_room_seat UNIQUE (room_id, seat_row, seat_column)
);
GO

IF OBJECT_ID('dbo.showtimes', 'U') IS NULL
CREATE TABLE showtimes (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    movie_id        BIGINT          NOT NULL FOREIGN KEY REFERENCES movies(id),
    room_id         BIGINT          NOT NULL FOREIGN KEY REFERENCES rooms(id),
    start_time      DATETIME2       NOT NULL,
    end_time        DATETIME2       NOT NULL,
    base_price      DECIMAL(10,2)   NOT NULL
);
GO

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
    ALTER TABLE tickets ADD payment_method NVARCHAR(20) NULL;   -- COUNTER | MOMO, NULL coi như tại quầy
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
    payment_method      NVARCHAR(20)    NULL,       -- COUNTER | MOMO
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

-- Tai khoan admin mac dinh
-- Mat khau cua ca 3 tai khoan mau deu la: 123456
-- Chuoi duoi day la ban da bam bang BCrypt (Module 3 dung BCryptPasswordEncoder
-- de kiem tra dang nhap). Tuyet doi khong luu mat khau tho vao cot nay.
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@utecinema.local')
    INSERT INTO users (full_name, email, password_hash, role)
    VALUES (N'Quản trị viên', 'admin@utecinema.local', '$2a$10$0hP214zsHpy5UeMXorB1bOze53HL8258/nZV3SGW9qh7HqNWm/jqu', 'ADMIN');
GO
