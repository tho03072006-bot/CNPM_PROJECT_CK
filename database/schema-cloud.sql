-- ================================================================
-- Cinema Booking - NHOM05 - Schema cho Azure SQL Database (cloud)
--
-- Khac gi so voi database/schema.sql (ban chay o may ca nhan):
--   - Azure SQL Database KHONG cho chay "CREATE DATABASE ..." chung script voi bang,
--     va KHONG ho tro cau lenh "USE <database>".
--     => Ban phai TAO DATABASE TRUOC tren Azure Portal, roi ket noi thang vao database do
--        va chay file nay.
--   - Cac lenh deu co kiem tra "neu chua ton tai" -> chay lai nhieu lan khong bi loi.
--
-- Cach chay:
--   sqlcmd -S <ten-server>.database.windows.net -d cinema_booking -U <user> -P <password> -N -i database\schema-azure.sql
--   (hoac mo bang SSMS / Azure Data Studio, chon dung database cinema_booking roi Execute)
--
-- LUU Y: khi da chay file nay thi tren profile "cloud" Hibernate chay o che do
-- ddl-auto=validate - tuc la Hibernate KHONG duoc tu sua schema chung. Muon them/sua bang
-- thi sua file nay + database/schema.sql roi bao ca nhom (xem ADR-002).
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
    -- Chong dat trung ghe cho cung 1 suat chieu (ADR-001) - KHONG duoc xoa rang buoc nay
    CONSTRAINT uq_showtime_seat UNIQUE (showtime_id, seat_id)
);
GO

-- Tai khoan admin mac dinh (mat khau can duoc hash bang BCrypt truoc khi dung that,
-- day chi la du lieu mau de test nhanh - doi truoc khi demo)
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@nhom05.local')
    INSERT INTO users (full_name, email, password_hash, role)
    VALUES (N'Quan tri vien', 'admin@nhom05.local', '123456', 'ADMIN');
GO
