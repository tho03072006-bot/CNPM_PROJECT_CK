-- ================================================
-- Cinema Booking - NHÓM 8 - Schema cho SQL Server trên máy cá nhân
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

-- Tài khoản admin mặc định. Mật khẩu ở đây CHƯA HASH, chỉ là dữ liệu mẫu để test nhanh.
-- Module 3 làm xong phần đăng nhập thì phải thay bằng chuỗi hash BCrypt.
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@nhom8.local')
    INSERT INTO users (full_name, email, password_hash, role)
    VALUES (N'Quản trị viên', 'admin@nhom8.local', '123456', 'ADMIN');
GO
