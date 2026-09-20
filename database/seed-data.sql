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
-- Trên database dùng chung ở cloud thì đổi -S, -d, -U, -P cho đúng (xem docs/DATABASE.md).
--
-- File này CHẠY LẠI ĐƯỢC NHIỀU LẦN: mỗi khối đều kiểm tra "nếu chưa có thì mới thêm",
-- nên không sợ chạy nhầm hai lần rồi nhân đôi dữ liệu.
--
-- THÔNG TIN PHIM LÀ THẬT: tên phim, thể loại, thời lượng, nhãn tuổi và ngày khởi chiếu
-- lấy theo lịch chiếu rạp Việt Nam ngày 20/09/2026. Ảnh poster để ở dạng đường dẫn tới
-- máy chủ ảnh của nguồn, không tải về kho mã. Nếu sau này ảnh hỏng thì chỉ cần thay
-- cột poster_url, không ảnh hưởng gì tới chương trình.
--
-- Tài khoản, phòng chiếu và suất chiếu vẫn là dữ liệu tự đặt để chạy thử.
-- Mật khẩu trong này chưa hash, phải thay bằng BCrypt khi Module 3 làm xong đăng nhập.
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
-- Bóng Ma Nhà Hát - khởi chiếu 18/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Bóng Ma Nhà Hát')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Bóng Ma Nhà Hát', N'Hài, Kinh dị', 97,
            N'Một chuyên viên bất động sản nhận nhiệm vụ vực dậy nhà hát cũ trong vòng một tháng, rồi phát hiện nơi này có hồn ma một nữ diễn viên chưa siêu thoát.',
            N'https://cdn.moveek.com/storage/media/cache/tall/6aa3a066c7afd223776710.webp',
            N'T16', 1);


-- Vùng Đất Quỷ Dữ 2026 - khởi chiếu 18/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Vùng Đất Quỷ Dữ 2026')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Vùng Đất Quỷ Dữ 2026', N'Kinh dị, Khoa học viễn tưởng', 94,
            N'Phần phim Resident Evil mới với cốt truyện riêng: một nhân viên vận chuyển vật tư y tế bị cuốn vào cuộc chạy trốn sinh tồn suốt một đêm.',
            N'https://cdn.moveek.com/storage/media/cache/tall/6a98ed8bf12b6567604371.webp',
            N'T18', 1);


-- Lên Hương - khởi chiếu 14/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Lên Hương')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Lên Hương', N'Tâm lý, Gia đình', 121,
            N'Bà chủ một trại hòm ế khách và chàng trai cần tiền chữa bệnh cho mẹ vướng vào một giao kèo, kéo theo món nợ bị chôn giấu nhiều năm.',
            N'https://cdn.moveek.com/storage/media/cache/tall/6a853e8a114e2776669135.webp',
            N'T16', 1);


-- Tế Nhi Cải Mệnh - khởi chiếu 18/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Tế Nhi Cải Mệnh')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Tế Nhi Cải Mệnh', N'Kinh dị', 104,
            N'Người đàn ông ngập trong nợ cờ bạc tìm tới tà thuật cấm kỵ để đổi lấy tiền, và tai hoạ ập xuống cả gia đình ngay sau đó.',
            N'https://cdn.moveek.com/storage/media/cache/tall/6aa4d2eb9ebdd653449192.webp',
            N'T18', 1);


-- Người Nhện 4: Khởi Đầu Mới - khởi chiếu 31/07/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Người Nhện 4: Khởi Đầu Mới')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Người Nhện 4: Khởi Đầu Mới', N'Hành động, Khoa học viễn tưởng', 145,
            N'Peter Parker phải chiến đấu một mình khi không còn ai bên cạnh, trước một thế lực mới và một biến đổi thể chất đe doạ chính anh.',
            N'https://cdn.moveek.com/storage/media/cache/tall/6a545dce71636477839863.webp',
            N'T13', 1);


-- Conan Movie 29 (2026): Thiên Thần Sa Ngã Trên Xa Lộ - khởi chiếu 24/07/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Conan Movie 29 (2026): Thiên Thần Sa Ngã Trên Xa Lộ')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Conan Movie 29 (2026): Thiên Thần Sa Ngã Trên Xa Lộ', N'Hoạt hình, Trinh thám, Hành động', 109,
            N'Conan cùng nhóm bạn tới Yokohama dự lễ hội mô tô và vướng vào vụ án xoay quanh một tay đua bí ẩn cùng mẫu xe công nghệ cao.',
            N'https://cdn.moveek.com/storage/media/cache/tall/6a2e2b5d405dd381486132.webp',
            N'T13', 1);


-- PAW Patrol: Phim Khủng Long - khởi chiếu 14/08/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'PAW Patrol: Phim Khủng Long')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'PAW Patrol: Phim Khủng Long', N'Hoạt hình, Phiêu lưu, Gia đình', 89,
            N'Đội cứu hộ Paw Patrol trở lại màn ảnh rộng với một chuyến phiêu lưu giữa những con khủng long.',
            N'https://cdn.moveek.com/storage/media/cache/tall/69f9578c938ef360861815.webp',
            N'P', 1);


-- Yêu Nhân Thần Thám: Kỳ Án Trường An - khởi chiếu 18/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Yêu Nhân Thần Thám: Kỳ Án Trường An')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Yêu Nhân Thần Thám: Kỳ Án Trường An', N'Hoạt hình, Hài, Trinh thám', 117,
            N'Ở Trường An thời Đường nơi người và yêu quái sống chung, một thiếu niên mê suy luận cùng tân binh yêu sói cùng nhau phá án.',
            N'https://cdn.moveek.com/storage/media/cache/tall/6a98ed129ae79346045889.webp',
            N'K', 1);


-- Phim ra rạp đã lâu, để trạng thái ngừng chiếu nhằm kiểm tra bộ lọc "chỉ lấy phim đang chiếu".
-- Minions & Quái Vật - khởi chiếu 01/07/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Minions & Quái Vật')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Minions & Quái Vật', N'Hoạt hình, Hài', 90,
            N'Nhóm Minions nổi danh ở Hollywood rồi mất tất cả, vô tình thả quái vật ra khắp thế giới và phải tự dọn mớ hỗn loạn do mình gây ra.',
            N'https://cdn.moveek.com/storage/media/cache/tall/6a0191c8e9620912668089.webp',
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
    -- Phòng 1 - xếp nối tiếp nhau, suất sau bắt đầu sau khi suất trước đã dọn xong
    (N'PAW Patrol: Phim Khủng Long',                         @room1, 31,  65000),
    (N'Lên Hương',                                           @room1, 33,  75000),
    (N'Bóng Ma Nhà Hát',                                     @room1, 36,  75000),
    (N'Vùng Đất Quỷ Dữ 2026',                                @room1, 38,  85000),
    (N'Người Nhện 4: Khởi Đầu Mới',                          @room1, 40,  95000),
    (N'Conan Movie 29 (2026): Thiên Thần Sa Ngã Trên Xa Lộ',  @room1, 43,  85000),
    (N'Tế Nhi Cải Mệnh',                                     @room1, 46,  85000),
    -- Phòng 2 - VIP, giá cao hơn
    (N'Yêu Nhân Thần Thám: Kỳ Án Trường An',                 @room2, 33, 110000),
    (N'Bóng Ma Nhà Hát',                                     @room2, 36, 120000),
    (N'Vùng Đất Quỷ Dữ 2026',                                @room2, 38, 130000),
    (N'Lên Hương',                                           @room2, 41, 120000),
    (N'Người Nhện 4: Khởi Đầu Mới',                          @room2, 44, 140000);

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
