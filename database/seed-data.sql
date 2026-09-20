-- ================================================================
-- UTE Cinema - Dữ liệu mẫu dùng chung
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
-- DỮ LIỆU LẤY TỪ MỘT RẠP CÓ THẬT: phim, lịch chiếu, số phòng, loại phòng và giá vé
-- đều lấy theo CGV Vincom Đồng Khởi (Quận 1, TP.HCM) ngày 20/09/2026. Ảnh poster để ở dạng đường dẫn tới
-- máy chủ ảnh của nguồn (bản 800px, sắc nét cho màn hình lớn), không tải về kho mã.
-- Nếu sau này ảnh hỏng thì chỉ cần thay
-- cột poster_url, không ảnh hưởng gì tới chương trình.
--
-- Tài khoản, phòng chiếu và suất chiếu vẫn là dữ liệu tự đặt để chạy thử.
-- Mật khẩu trong này chưa hash, phải thay bằng BCrypt khi Module 3 làm xong đăng nhập.
-- ================================================================
SET NOCOUNT ON;

-- ---------------------------------------------------------------
-- 1. Người dùng mẫu
-- ---------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'khachhang@utecinema.local')
    INSERT INTO users (full_name, email, phone, password_hash, role)
    VALUES (N'Nguyễn Văn Khách', 'khachhang@utecinema.local', '0901234567', N'chua_hash_123456', 'CUSTOMER');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'nhanvien@utecinema.local')
    INSERT INTO users (full_name, email, phone, password_hash, role)
    VALUES (N'Trần Thị Nhân Viên', 'nhanvien@utecinema.local', '0907654321', N'chua_hash_123456', 'STAFF');

-- ---------------------------------------------------------------
-- 2. Phim
-- ---------------------------------------------------------------
-- Bóng Ma Nhà Hát - khởi chiếu 18/09/2026
-- Út Lan 2 - khởi chiếu 25/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Út Lan 2')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Út Lan 2', N'Tâm lý, Kinh dị', 107,
            N'Một con rắn hai đầu xuất hiện, ngôi làng ven sông liên tiếp có người chết bí ẩn và những lời nguyền cũ dần trồi lên mặt nước.',
            N'https://cdn.moveek.com/storage/media/cache/full/6aa3a03fbe122916480363.webp',
            N'T18', 1);


-- Lên Hương - khởi chiếu 14/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Lên Hương')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Lên Hương', N'Tâm lý, Gia đình', 121,
            N'Bà chủ một trại hòm ế khách và chàng trai cần tiền chữa bệnh cho mẹ vướng vào một giao kèo, kéo theo món nợ bị chôn giấu nhiều năm.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a853e8a114e2776669135.webp',
            N'T16', 1);


-- Vùng Đất Quỷ Dữ 2026 - khởi chiếu 18/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Vùng Đất Quỷ Dữ 2026')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Vùng Đất Quỷ Dữ 2026', N'Kinh dị, Khoa học viễn tưởng', 94,
            N'Phần phim Resident Evil mới với cốt truyện riêng: một nhân viên vận chuyển vật tư y tế bị cuốn vào cuộc chạy trốn sinh tồn suốt một đêm.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a98ed8bf12b6567604371.webp',
            N'T18', 1);


-- Bóng Ma Nhà Hát - khởi chiếu 18/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Bóng Ma Nhà Hát')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Bóng Ma Nhà Hát', N'Hài, Kinh dị', 97,
            N'Một chuyên viên bất động sản nhận nhiệm vụ vực dậy nhà hát cũ trong vòng một tháng, rồi phát hiện nơi này có hồn ma một nữ diễn viên chưa siêu thoát.',
            N'https://cdn.moveek.com/storage/media/cache/full/6aa3a066c7afd223776710.webp',
            N'T16', 1);


-- Yêu Nhân Thần Thám: Kỳ Án Trường An - khởi chiếu 18/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Yêu Nhân Thần Thám: Kỳ Án Trường An')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Yêu Nhân Thần Thám: Kỳ Án Trường An', N'Hoạt hình, Hài, Trinh thám', 117,
            N'Ở Trường An thời Đường nơi người và yêu quái sống chung, một thiếu niên mê suy luận cùng tân binh yêu sói cùng nhau phá án.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a98ed129ae79346045889.webp',
            N'K', 1);


-- Marine Yêu Dấu - khởi chiếu 18/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Marine Yêu Dấu')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Marine Yêu Dấu', N'Hoạt hình, Tâm lý, Gia đình', 103,
            N'Phim hoạt hình Nhật Bản về cô bé được gửi về vùng quê ven biển và tình bạn kỳ lạ với một cô gái sống trong toà nhà bên đầm lầy.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a98eff7bd6ee444134711.webp',
            N'K', 1);


-- Nghỉ Hè Sợ Nghỉ Hưu - khởi chiếu 21/08/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Nghỉ Hè Sợ Nghỉ Hưu')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Hài, Tâm lý, Gia đình', 117,
            N'Câu chuyện gia đình xoay quanh kỳ nghỉ hè và nỗi lo tuổi hưu của thế hệ đi trước.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a5dacd32459b264392741.webp',
            N'T13', 1);


-- Hope Vùng Tử Địa - khởi chiếu 04/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Hope Vùng Tử Địa')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Hope Vùng Tử Địa', N'Hành động, Trinh thám, Khoa học viễn tưởng', 157,
            N'Phim hành động viễn tưởng dài 157 phút, suất chiếu đặc biệt có phụ đề cả tiếng Việt lẫn tiếng Anh.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a828aed8c1ae817893779.webp',
            N'T16', 1);


-- Quý Tử Vượt Giàu - khởi chiếu 28/08/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Quý Tử Vượt Giàu')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Quý Tử Vượt Giàu', N'Hài, Gia đình', 118,
            N'Phim hài gia đình về cậu ấm nhà giàu và hành trình tự chứng minh mình.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a7bfad989660368179051.webp',
            N'K', 1);


-- Bùa Yêu: Bí Mật Gia Tộc - khởi chiếu 11/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Bùa Yêu: Bí Mật Gia Tộc')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Bùa Yêu: Bí Mật Gia Tộc', N'Hài, Lãng mạn, Giả tưởng', 130,
            N'Phần tiếp theo của Practical Magic, với Sandra Bullock và Nicole Kidman trong vai hai chị em nhà Owens và phép thuật gia truyền.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a98eb5339465087886411.webp',
            N'T13', 1);


-- Chiikawa: Bí Mật Đảo Người Cá - khởi chiếu 28/08/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Chiikawa: Bí Mật Đảo Người Cá')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Chiikawa: Bí Mật Đảo Người Cá', N'Hoạt hình, Phiêu lưu, Gia đình', 99,
            N'Nhóm Chiikawa lên đường tới hòn đảo của người cá trong chuyến phiêu lưu dành cho mọi lứa tuổi.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a83d7604f918994427126.webp',
            N'P', 1);


-- Tàu Buôn Người - khởi chiếu 28/08/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Tàu Buôn Người')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Tàu Buôn Người', N'Hành động, Giật gân', 95,
            N'Phim hành động giật gân trên một con tàu, nơi nhóm người bị giam giữ tìm cách nổi dậy.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a82a0452a09c551249958.webp',
            N'T18', 1);


-- Phim đã kết thúc đợt chiếu, để trạng thái ngừng chiếu nhằm kiểm tra bộ lọc "chỉ lấy phim đang chiếu".
-- Minions & Quái Vật - khởi chiếu 01/07/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Minions & Quái Vật')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Minions & Quái Vật', N'Hoạt hình, Hài', 90,
            N'Nhóm Minions nổi danh ở Hollywood rồi mất tất cả, vô tình thả quái vật ra khắp thế giới và phải tự dọn mớ hỗn loạn do mình gây ra.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a0191c8e9620912668089.webp',
            N'P', 0);

-- ---------------------------------------------------------------
-- 3. Phòng chiếu
--    Mô phỏng một cụm rạp thật: 5 phòng thường, 1 phòng PREMIUM, 2 phòng GOLD CLASS.
--    Số phòng không phải bịa ra: nó được suy từ chính lịch chiếu thật bên dưới —
--    xếp 47 suất sao cho không phòng nào có hai suất chồng giờ thì cần đúng 8 phòng.
-- ---------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Cinema 1')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Cinema 1', 10, 14);

IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Cinema 2')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Cinema 2', 10, 14);

IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Cinema 3')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Cinema 3', 10, 14);

IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Cinema 4')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Cinema 4', 10, 14);

IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Cinema 5')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Cinema 5', 10, 14);

IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Cinema 6 - PREMIUM')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Cinema 6 - PREMIUM', 6, 10);

IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Cinema 7 - GOLD CLASS')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Cinema 7 - GOLD CLASS', 4, 8);

IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Cinema 8 - GOLD CLASS')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Cinema 8 - GOLD CLASS', 4, 8);

-- ---------------------------------------------------------------
-- 4. Ghế - sinh tự động theo số hàng/cột của từng phòng
--    Hàng đặt tên A, B, C... ; cột đánh số từ 1.
--
--    Cách bố trí ghế mô phỏng rạp thật:
--    - Phòng thường: hàng cuối là ghế đôi (rạp hay gọi là Sweetbox),
--      hai hàng ngay trước đó là ghế VIP, còn lại là ghế thường.
--    - Phòng PREMIUM và GOLD CLASS: toàn bộ là ghế ngả cao cấp nên để hết loại VIP.
-- ---------------------------------------------------------------
DECLARE @roomId BIGINT, @roomName NVARCHAR(50), @totalRows INT, @totalColumns INT;
DECLARE @rowIndex INT, @columnIndex INT, @rowLabel NVARCHAR(5), @seatType NVARCHAR(20);
DECLARE @phongCaoCap BIT;

DECLARE room_cursor CURSOR FOR
    SELECT id, name, total_rows, total_columns FROM rooms;

OPEN room_cursor;
FETCH NEXT FROM room_cursor INTO @roomId, @roomName, @totalRows, @totalColumns;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @phongCaoCap = CASE
        WHEN @roomName LIKE N'%GOLD CLASS%' OR @roomName LIKE N'%PREMIUM%' THEN 1 ELSE 0 END;

    SET @rowIndex = 1;
    WHILE @rowIndex <= @totalRows
    BEGIN
        SET @rowLabel = CHAR(64 + @rowIndex);  -- 1 -> 'A', 2 -> 'B'...
        SET @seatType = CASE
            WHEN @phongCaoCap = 1            THEN 'VIP'
            WHEN @rowIndex = @totalRows      THEN 'COUPLE'      -- hàng cuối: ghế đôi
            WHEN @rowIndex > @totalRows - 3  THEN 'VIP'         -- hai hàng trước đó: VIP
            ELSE 'NORMAL'
        END;

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

    FETCH NEXT FROM room_cursor INTO @roomId, @roomName, @totalRows, @totalColumns;
END

CLOSE room_cursor;
DEALLOCATE room_cursor;

-- ---------------------------------------------------------------
-- 5. Suất chiếu - LỊCH CHIẾU THẬT
--    Toàn bộ 47 suất dưới đây là lịch chiếu có thật của CGV Vincom Đồng Khởi
--    (Tầng 3, Vincom Center Đồng Khởi, 72 Lê Thánh Tôn, Quận 1, TP.HCM)
--    trong ngày 20/09/2026, giữ nguyên giờ chiếu và loại phòng.
--
--    Giá vé cũng là giá công bố của chính rạp đó, vé người lớn:
--      - Phòng thường  135.000đ  (Thứ Sáu, Thứ Bảy, Chủ Nhật và ngày lễ;
--                                 ngày thường 115.000đ, Thứ Tư Vui Vẻ 79.000đ)
--      - PREMIUM       150.000đ
--      - GOLD CLASS    200.000đ
--    Rạp còn phụ thu ghế VIP +5.500đ và ghế đôi +26.000đ, nhưng dự án này tính
--    phụ thu theo tỉ lệ trong SeatPricingService (VIP +50%, ghế đôi gấp đôi)
--    nên cột base_price chỉ lưu giá ghế thường.
--
--    Lịch neo vào 0 giờ NGÀY MAI để suất chiếu luôn nằm ở tương lai, chạy lại
--    ngày nào cũng có dữ liệu hợp lệ mà không phải sửa tay ngày tháng.
-- ---------------------------------------------------------------
DECLARE @ngayMai DATETIME2 = DATEADD(DAY, 1, CAST(CAST(SYSDATETIME() AS DATE) AS DATETIME2));

-- phim, phòng, số phút kể từ 0 giờ ngày mai, giá vé ghế thường
DECLARE @plannedShowtimes TABLE (
    movieTitle  NVARCHAR(200),
    roomName    NVARCHAR(50),
    minuteOfDay INT,
    basePrice   DECIMAL(10, 2)
);

INSERT INTO @plannedShowtimes (movieTitle, roomName, minuteOfDay, basePrice) VALUES
    (N'Marine Yêu Dấu', N'Cinema 1', 690, 135000),
    (N'Bóng Ma Nhà Hát', N'Cinema 1', 820, 135000),
    (N'Marine Yêu Dấu', N'Cinema 1', 940, 135000),
    (N'Út Lan 2', N'Cinema 1', 1060, 135000),
    (N'Út Lan 2', N'Cinema 1', 1190, 135000),
    (N'Út Lan 2', N'Cinema 1', 1320, 135000),
    (N'Lên Hương', N'Cinema 2', 700, 135000),
    (N'Út Lan 2', N'Cinema 2', 840, 135000),
    (N'Út Lan 2', N'Cinema 2', 970, 135000),
    (N'Marine Yêu Dấu', N'Cinema 2', 1100, 135000),
    (N'Lên Hương', N'Cinema 2', 1230, 135000),
    (N'Lên Hương', N'Cinema 2', 1380, 135000),
    (N'Út Lan 2', N'Cinema 3', 710, 135000),
    (N'Lên Hương', N'Cinema 3', 850, 135000),
    (N'Lên Hương', N'Cinema 3', 1000, 135000),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 3', 1140, 135000),
    (N'Lên Hương', N'Cinema 3', 1260, 135000),
    (N'Út Lan 2', N'Cinema 3', 1400, 135000),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 4', 730, 135000),
    (N'Chiikawa: Bí Mật Đảo Người Cá', N'Cinema 4', 880, 135000),
    (N'Marine Yêu Dấu', N'Cinema 4', 1010, 135000),
    (N'Lên Hương', N'Cinema 4', 1150, 135000),
    (N'Lên Hương', N'Cinema 4', 1300, 135000),
    (N'Lên Hương', N'Cinema 5', 750, 135000),
    (N'Lên Hương', N'Cinema 5', 900, 135000),
    (N'Lên Hương', N'Cinema 5', 1050, 135000),
    (N'Lên Hương', N'Cinema 5', 1200, 135000),
    (N'Lên Hương', N'Cinema 5', 1340, 135000),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 6 - PREMIUM', 645, 150000),
    (N'Út Lan 2', N'Cinema 6 - PREMIUM', 760, 150000),
    (N'Quý Tử Vượt Giàu', N'Cinema 6 - PREMIUM', 890, 150000),
    (N'Lên Hương', N'Cinema 6 - PREMIUM', 1030, 150000),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 6 - PREMIUM', 1170, 150000),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 6 - PREMIUM', 1290, 150000),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 6 - PREMIUM', 1410, 150000),
    (N'Yêu Nhân Thần Thám: Kỳ Án Trường An', N'Cinema 7 - GOLD CLASS', 630, 200000),
    (N'Hope Vùng Tử Địa', N'Cinema 7 - GOLD CLASS', 770, 200000),
    (N'Bùa Yêu: Bí Mật Gia Tộc', N'Cinema 7 - GOLD CLASS', 960, 200000),
    (N'Tàu Buôn Người', N'Cinema 7 - GOLD CLASS', 1120, 200000),
    (N'Út Lan 2', N'Cinema 7 - GOLD CLASS', 1240, 200000),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - GOLD CLASS', 1370, 200000),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 740, 200000),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 860, 200000),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 975, 200000),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 1090, 200000),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 8 - GOLD CLASS', 1210, 200000),
    (N'Út Lan 2', N'Cinema 8 - GOLD CLASS', 1350, 200000);

INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price)
SELECT
    m.id,
    r.id,
    DATEADD(MINUTE, p.minuteOfDay, @ngayMai),
    -- Kết thúc = bắt đầu + thời lượng phim + 15 phút dọn phòng
    DATEADD(MINUTE, m.duration_min + 15, DATEADD(MINUTE, p.minuteOfDay, @ngayMai)),
    p.basePrice
FROM @plannedShowtimes p
JOIN movies m ON m.title = p.movieTitle
JOIN rooms  r ON r.name  = p.roomName
WHERE NOT EXISTS (
    SELECT 1 FROM showtimes s
    WHERE s.movie_id = m.id
      AND s.room_id = r.id
      AND s.start_time = DATEADD(MINUTE, p.minuteOfDay, @ngayMai)
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
