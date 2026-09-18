-- ================================================================
-- Cinema Booking - NHÓM 05 - Dữ liệu mẫu dùng chung
--
-- Mục đích: cả 4 người test trên CÙNG một bộ dữ liệu. Tài thêm suất chiếu,
-- Thắng thử giữ ghế, Thanh thử thanh toán - tất cả nhìn thấy cùng phim,
-- cùng phòng, cùng số ghế, nên nói chuyện với nhau mới khớp.
--
-- Cách chạy (sau khi đã chạy schema.sql):
--   sqlcmd -S localhost,1433 -U sa -C -f 65001 -d cinema_booking -i database\seed-data.sql
--
-- *** BẮT BUỘC có -f 65001 *** Thiếu tham số này thì sqlcmd đọc file sai bảng mã,
-- tên phim sẽ thành chữ rác kiểu "BÃ£o Giá»¯a Trá»i Quang" và chỉ phát hiện ra
-- khi đã nhập cả đống dữ liệu. Đã thử và xác nhận lỗi này là có thật.
--
-- Trên database dùng chung ở cloud thì đổi -S, -d, -U, -P cho đúng (xem ADR-002).
--
-- File này CHẠY LẠI ĐƯỢC NHIỀU LẦN: mỗi khối đều kiểm tra "nếu chưa có thì mới thêm",
-- nên không sợ chạy nhầm hai lần rồi nhân đôi dữ liệu.
--
-- LƯU Ý: đây là dữ liệu GIẢ để chạy thử. Mật khẩu trong này chưa hash,
-- phải thay bằng BCrypt khi Module 3 làm xong phần đăng nhập.
-- ================================================================
SET NOCOUNT ON;

-- ---------------------------------------------------------------
-- 1. Người dùng mẫu
-- ---------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'khachhang@nhom05.local')
    INSERT INTO users (full_name, email, phone, password_hash, role)
    VALUES (N'Nguyễn Văn Khách', 'khachhang@nhom05.local', '0901234567', N'chua_hash_123456', 'CUSTOMER');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'nhanvien@nhom05.local')
    INSERT INTO users (full_name, email, phone, password_hash, role)
    VALUES (N'Trần Thị Nhân Viên', 'nhanvien@nhom05.local', '0907654321', N'chua_hash_123456', 'STAFF');

-- ---------------------------------------------------------------
-- 2. Phim
-- ---------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Bão Giữa Trời Quang')
    INSERT INTO movies (title, genre, duration_min, description, age_rating, is_active)
    VALUES (N'Bão Giữa Trời Quang', N'Tâm lý', 118,
            N'Một gia đình nhỏ ở miền Trung đối mặt với mùa bão và những bí mật chưa từng nói ra.',
            N'C13', 1);

IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Mật Mã Thành Phố')
    INSERT INTO movies (title, genre, duration_min, description, age_rating, is_active)
    VALUES (N'Mật Mã Thành Phố', N'Hành động', 132,
            N'Một kỹ sư an ninh mạng bị cuốn vào âm mưu đánh sập hệ thống giao thông của cả thành phố.',
            N'C16', 1);

IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Chuyến Tàu Mùa Hạ')
    INSERT INTO movies (title, genre, duration_min, description, age_rating, is_active)
    VALUES (N'Chuyến Tàu Mùa Hạ', N'Hoạt hình', 95,
            N'Cô bé mười tuổi cùng chú mèo lạc lên chuyến tàu đi qua những mùa ký ức.',
            N'P', 1);

IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Đêm Không Ngủ')
    INSERT INTO movies (title, genre, duration_min, description, age_rating, is_active)
    VALUES (N'Đêm Không Ngủ', N'Kinh dị', 104,
            N'Ca trực đêm cuối cùng của một y tá trong bệnh viện sắp đóng cửa.',
            N'C18', 1);

IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Hẹn Em Ngày Nắng')
    INSERT INTO movies (title, genre, duration_min, description, age_rating, is_active)
    VALUES (N'Hẹn Em Ngày Nắng', N'Lãng mạn', 110,
            N'Hai người bạn thời đại học gặp lại nhau sau mười năm, ở đúng quán cà phê ngày xưa.',
            N'C13', 1);

-- Một phim đã ngừng chiếu, để thử bộ lọc "chỉ lấy phim đang chiếu"
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Phim Đã Ngừng Chiếu')
    INSERT INTO movies (title, genre, duration_min, description, age_rating, is_active)
    VALUES (N'Phim Đã Ngừng Chiếu', N'Tài liệu', 88,
            N'Phim này để trạng thái ngừng chiếu, dùng để kiểm tra chức năng lọc phim đang chiếu.',
            N'P', 0);

-- ---------------------------------------------------------------
-- 3. Phòng chiếu
-- ---------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Phòng 1')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Phòng 1', 8, 10);

IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Phòng 2 - VIP')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Phòng 2 - VIP', 5, 8);

-- ---------------------------------------------------------------
-- 4. Ghế - sinh tự động theo số hàng/cột của từng phòng
--    Hàng đặt tên A, B, C... ; cột đánh số từ 1.
--    Hai hàng cuối của mỗi phòng để loại VIP (giá cao hơn).
-- ---------------------------------------------------------------
DECLARE @roomId BIGINT, @totalRows INT, @totalColumns INT;
DECLARE @rowIndex INT, @columnIndex INT, @rowLabel NVARCHAR(5), @seatType NVARCHAR(20);

DECLARE room_cursor CURSOR FOR
    SELECT id, total_rows, total_columns FROM rooms;

OPEN room_cursor;
FETCH NEXT FROM room_cursor INTO @roomId, @totalRows, @totalColumns;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @rowIndex = 1;
    WHILE @rowIndex <= @totalRows
    BEGIN
        SET @rowLabel = CHAR(64 + @rowIndex);  -- 1 -> 'A', 2 -> 'B'...
        -- Hai hàng cuối cùng là ghế VIP
        SET @seatType = CASE WHEN @rowIndex > @totalRows - 2 THEN 'VIP' ELSE 'NORMAL' END;

        SET @columnIndex = 1;
        WHILE @columnIndex <= @totalColumns
        BEGIN
            IF NOT EXISTS (
                SELECT 1 FROM seats
                WHERE room_id = @roomId AND seat_row = @rowLabel AND seat_column = @columnIndex
            )
                INSERT INTO seats (room_id, seat_row, seat_column, seat_type)
                VALUES (@roomId, @rowLabel, @columnIndex, @seatType);

            SET @columnIndex = @columnIndex + 1;
        END

        SET @rowIndex = @rowIndex + 1;
    END

    FETCH NEXT FROM room_cursor INTO @roomId, @totalRows, @totalColumns;
END

CLOSE room_cursor;
DEALLOCATE room_cursor;

-- ---------------------------------------------------------------
-- 5. Suất chiếu
--    Tính từ 0 giờ hôm nay để dữ liệu luôn "tương lai gần", chạy ngày nào
--    cũng có suất chiếu hợp lệ mà không phải sửa tay ngày tháng.
-- ---------------------------------------------------------------
DECLARE @today DATETIME2 = CAST(CAST(SYSDATETIME() AS DATE) AS DATETIME2);
DECLARE @room1 BIGINT = (SELECT id FROM rooms WHERE name = N'Phòng 1');
DECLARE @room2 BIGINT = (SELECT id FROM rooms WHERE name = N'Phòng 2 - VIP');

-- Bảng tạm mô tả các suất muốn tạo: phim, phòng, số giờ kể từ 0h hôm nay, giá vé
DECLARE @plannedShowtimes TABLE (
    movieTitle   NVARCHAR(200),
    roomId       BIGINT,
    hoursFromNow INT,
    basePrice    DECIMAL(10, 2)
);

INSERT INTO @plannedShowtimes (movieTitle, roomId, hoursFromNow, basePrice) VALUES
    (N'Bão Giữa Trời Quang', @room1, 33,  75000),
    (N'Bão Giữa Trời Quang', @room1, 39,  85000),
    (N'Mật Mã Thành Phố',    @room1, 36,  75000),
    (N'Mật Mã Thành Phố',    @room2, 42, 120000),
    (N'Chuyến Tàu Mùa Hạ',   @room1, 31,  65000),
    (N'Chuyến Tàu Mùa Hạ',   @room2, 34, 110000),
    (N'Đêm Không Ngủ',       @room1, 45,  85000),
    (N'Hẹn Em Ngày Nắng',    @room1, 38,  75000),
    (N'Hẹn Em Ngày Nắng',    @room2, 44, 120000),
    (N'Bão Giữa Trời Quang', @room2, 57, 120000);

INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price)
SELECT
    m.id,
    p.roomId,
    DATEADD(HOUR, p.hoursFromNow, @today),
    -- Kết thúc = bắt đầu + thời lượng phim + 15 phút dọn phòng
    DATEADD(MINUTE, m.duration_min + 15, DATEADD(HOUR, p.hoursFromNow, @today)),
    p.basePrice
FROM @plannedShowtimes p
JOIN movies m ON m.title = p.movieTitle
WHERE NOT EXISTS (
    SELECT 1 FROM showtimes s
    WHERE s.movie_id = m.id
      AND s.room_id = p.roomId
      AND s.start_time = DATEADD(HOUR, p.hoursFromNow, @today)
);

-- ---------------------------------------------------------------
-- 6. Báo cáo kết quả
-- ---------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM users)     AS so_nguoi_dung,
    (SELECT COUNT(*) FROM movies)    AS so_phim,
    (SELECT COUNT(*) FROM rooms)     AS so_phong,
    (SELECT COUNT(*) FROM seats)     AS so_ghe,
    (SELECT COUNT(*) FROM showtimes) AS so_suat_chieu,
    (SELECT COUNT(*) FROM tickets)   AS so_ve;
GO
