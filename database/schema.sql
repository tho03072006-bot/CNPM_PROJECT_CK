-- ================================================
-- Cinema Booking - NHOM05 - SQL Server schema
-- ================================================
IF DB_ID('cinema_booking') IS NULL
    CREATE DATABASE cinema_booking;
GO
USE cinema_booking;
GO

CREATE TABLE users (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    full_name       NVARCHAR(150)   NOT NULL,
    email           NVARCHAR(150)   NOT NULL UNIQUE,
    phone           NVARCHAR(20)    NULL,
    password_hash   NVARCHAR(255)   NOT NULL,
    role            NVARCHAR(20)    NOT NULL DEFAULT 'CUSTOMER', -- ADMIN | STAFF | CUSTOMER
    created_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME()
);

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

CREATE TABLE rooms (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(50)    NOT NULL,   -- vd: Phong 1, Phong VIP
    total_rows      INT             NOT NULL,
    total_columns   INT             NOT NULL
);

CREATE TABLE seats (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    room_id         BIGINT          NOT NULL FOREIGN KEY REFERENCES rooms(id),
    seat_row        NVARCHAR(5)     NOT NULL,  -- A, B, C...
    seat_column     INT             NOT NULL,
    seat_type       NVARCHAR(20)    NOT NULL DEFAULT 'NORMAL', -- NORMAL | VIP | COUPLE
    CONSTRAINT uq_room_seat UNIQUE (room_id, seat_row, seat_column)
);

CREATE TABLE showtimes (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    movie_id        BIGINT          NOT NULL FOREIGN KEY REFERENCES movies(id),
    room_id         BIGINT          NOT NULL FOREIGN KEY REFERENCES rooms(id),
    start_time      DATETIME2       NOT NULL,
    end_time        DATETIME2       NOT NULL,
    base_price      DECIMAL(10,2)   NOT NULL
);

CREATE TABLE tickets (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    showtime_id     BIGINT          NOT NULL FOREIGN KEY REFERENCES showtimes(id),
    seat_id         BIGINT          NOT NULL FOREIGN KEY REFERENCES seats(id),
    user_id         BIGINT          NOT NULL FOREIGN KEY REFERENCES users(id),
    status          NVARCHAR(20)    NOT NULL DEFAULT 'HELD', -- HELD | PAID | CANCELLED | EXPIRED
    price           DECIMAL(10,2)   NOT NULL,
    held_at         DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),
    paid_at         DATETIME2       NULL,
    -- Chong dat trung ghe cho cung 1 suat chieu (ADR-1)
    CONSTRAINT uq_showtime_seat UNIQUE (showtime_id, seat_id)
);

-- Tai khoan admin mac dinh (mat khau can duoc hash bang BCrypt truoc khi dung that,
-- day chi la du lieu mau de test nhanh - doi truoc khi deploy that)
INSERT INTO users (full_name, email, password_hash, role)
VALUES (N'Quan tri vien', 'admin@nhom05.local', '123456', 'ADMIN');
GO
