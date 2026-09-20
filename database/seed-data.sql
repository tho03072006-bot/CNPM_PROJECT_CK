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
-- đều lấy theo CGV Vincom Đồng Khởi (Quận 1, TP.HCM), lịch 5 ngày 20-24/09/2026. Ảnh poster để ở dạng đường dẫn tới
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


-- Tế Nhi Cải Mệnh - khởi chiếu 18/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Tế Nhi Cải Mệnh')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Tế Nhi Cải Mệnh', N'Kinh dị', 104,
            N'Người đàn ông ngập trong nợ cờ bạc tìm tới tà thuật cấm kỵ để đổi lấy tiền, và tai hoạ ập xuống cả gia đình ngay sau đó.',
            N'https://cdn.moveek.com/storage/media/cache/full/6aa4d2eb9ebdd653449192.webp',
            N'T18', 1);


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
            N'Phim hành động viễn tưởng dài 157 phút, có suất chiếu phụ đề cả tiếng Việt lẫn tiếng Anh.',
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


-- Bát Tiên Truy Tìm Lưu Ly Đăng - khởi chiếu 11/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Bát Tiên Truy Tìm Lưu Ly Đăng')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Bát Tiên Truy Tìm Lưu Ly Đăng', N'Hoạt hình, Hài, Phiêu lưu, Giả tưởng', 144,
            N'Phim hoạt hình phiêu lưu lấy cảm hứng từ truyền thuyết Bát Tiên đi tìm chiếc đèn lưu ly.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a98eb9dce992412855239.webp',
            N'K', 1);


-- Hòn Đảo Quên Lãng - khởi chiếu 25/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Hòn Đảo Quên Lãng')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Hòn Đảo Quên Lãng', N'Hoạt hình, Hài, Phiêu lưu, Gia đình', 109,
            N'Phim hoạt hình phiêu lưu trên một hòn đảo bị lãng quên, dành cho cả gia đình.',
            N'https://cdn.moveek.com/storage/media/cache/full/6a545e65722cc932065499.webp',
            N'K', 1);


-- Trại Buôn Người - khởi chiếu 25/09/2026
IF NOT EXISTS (SELECT 1 FROM movies WHERE title = N'Trại Buôn Người')
    INSERT INTO movies (title, genre, duration_min, description, poster_url, age_rating, is_active)
    VALUES (N'Trại Buôn Người', N'Hành động, Giật gân', 135,
            N'Để cứu em gái sa bẫy buôn người ở biên giới miền Tây, một thanh niên cùng bạn thân bị bắt làm nô dịch trong sào huyệt lừa đảo.',
            N'https://cdn.moveek.com/storage/media/cache/full/6aab64f6218f3539064479.webp',
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
--    Mô phỏng một cụm rạp thật: 6 phòng thường, 1 phòng PREMIUM, 2 phòng GOLD CLASS.
--    Số phòng không phải bịa: nó suy ra từ chính lịch chiếu thật bên dưới. Xếp 242 suất
--    của 5 ngày sao cho không phòng nào có hai suất chồng giờ thì cần đúng 9 phòng —
--    ngày đông nhất (52 suất) là ngày quyết định con số này.
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

IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Cinema 6')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Cinema 6', 10, 14);

IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Cinema 7 - PREMIUM')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Cinema 7 - PREMIUM', 6, 10);

IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Cinema 8 - GOLD CLASS')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Cinema 8 - GOLD CLASS', 4, 8);

IF NOT EXISTS (SELECT 1 FROM rooms WHERE name = N'Cinema 9 - GOLD CLASS')
    INSERT INTO rooms (name, total_rows, total_columns) VALUES (N'Cinema 9 - GOLD CLASS', 4, 8);

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
-- 5. Suất chiếu - LỊCH CHIẾU THẬT CỦA 5 NGÀY LIÊN TIẾP
--
--    Toàn bộ 242 suất dưới đây là lịch chiếu có thật của CGV Vincom Đồng Khởi
--    (Tầng 3, Vincom Center Đồng Khởi, 72 Lê Thánh Tôn, Quận 1, TP.HCM)
--    trong 5 ngày 20/09 đến 24/09/2026, giữ nguyên giờ chiếu và loại phòng.
--
--    Mỗi ngày một lịch khác nhau, đúng như rạp thật: phim mới vào lịch ở ngày sau
--    (Trại Buôn Người và Hòn Đảo Quên Lãng chỉ có suất từ ngày thứ 5), phim cũ
--    giảm dần số suất, và giờ chiếu ngày thường khác ngày cuối tuần.
--
--    GIÁ VÉ tính ngay trong câu lệnh theo đúng quy tắc của rạp, dựa vào thứ trong
--    tuần mà suất chiếu rơi vào:
--      - Phòng thường: Thứ Hai / Ba / Năm 115.000đ, Thứ Tư Vui Vẻ 79.000đ,
--                      Thứ Sáu / Bảy / Chủ Nhật và ngày lễ 135.000đ
--      - PREMIUM 150.000đ, GOLD CLASS 200.000đ (hai mức này rạp chỉ công bố một giá,
--        không đổi theo ngày)
--    Nhờ tính trong SQL nên chạy seed vào ngày nào thì giá vẫn khớp với thứ của
--    ngày đó, không bao giờ lệch.
--
--    Rạp còn phụ thu ghế VIP +5.500đ và ghế đôi +26.000đ, nhưng dự án tính phụ thu
--    theo tỉ lệ trong SeatPricingService (VIP +50%, ghế đôi gấp đôi) nên cột
--    base_price chỉ lưu giá ghế thường.
--
--    Lịch neo vào 0 giờ NGÀY MAI nên mọi suất luôn nằm ở tương lai.
-- ---------------------------------------------------------------
DECLARE @ngayMai DATETIME2 = DATEADD(DAY, 1, CAST(CAST(SYSDATETIME() AS DATE) AS DATETIME2));

-- phim, phòng, ngày thứ mấy kể từ ngày mai (0..4), số phút kể từ 0 giờ ngày đó
DECLARE @plannedShowtimes TABLE (
    movieTitle  NVARCHAR(200),
    roomName    NVARCHAR(50),
    dayOffset   INT,
    minuteOfDay INT
);

INSERT INTO @plannedShowtimes (movieTitle, roomName, dayOffset, minuteOfDay) VALUES
    -- ngày thứ 1 trong lịch (hôm nay)
    (N'Marine Yêu Dấu', N'Cinema 1', 0, 690),
    (N'Bóng Ma Nhà Hát', N'Cinema 1', 0, 820),
    (N'Marine Yêu Dấu', N'Cinema 1', 0, 940),
    (N'Út Lan 2', N'Cinema 1', 0, 1060),
    (N'Út Lan 2', N'Cinema 1', 0, 1190),
    (N'Út Lan 2', N'Cinema 1', 0, 1320),
    (N'Lên Hương', N'Cinema 2', 0, 700),
    (N'Út Lan 2', N'Cinema 2', 0, 840),
    (N'Út Lan 2', N'Cinema 2', 0, 970),
    (N'Marine Yêu Dấu', N'Cinema 2', 0, 1100),
    (N'Lên Hương', N'Cinema 2', 0, 1230),
    (N'Lên Hương', N'Cinema 2', 0, 1380),
    (N'Út Lan 2', N'Cinema 3', 0, 710),
    (N'Lên Hương', N'Cinema 3', 0, 850),
    (N'Lên Hương', N'Cinema 3', 0, 1000),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 3', 0, 1140),
    (N'Lên Hương', N'Cinema 3', 0, 1260),
    (N'Út Lan 2', N'Cinema 3', 0, 1400),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 4', 0, 730),
    (N'Chiikawa: Bí Mật Đảo Người Cá', N'Cinema 4', 0, 880),
    (N'Marine Yêu Dấu', N'Cinema 4', 0, 1010),
    (N'Lên Hương', N'Cinema 4', 0, 1150),
    (N'Lên Hương', N'Cinema 4', 0, 1300),
    (N'Lên Hương', N'Cinema 5', 0, 750),
    (N'Lên Hương', N'Cinema 5', 0, 900),
    (N'Lên Hương', N'Cinema 5', 0, 1050),
    (N'Lên Hương', N'Cinema 5', 0, 1200),
    (N'Lên Hương', N'Cinema 5', 0, 1340),
    (N'Út Lan 2', N'Cinema 7 - PREMIUM', 0, 760),
    (N'Quý Tử Vượt Giàu', N'Cinema 7 - PREMIUM', 0, 890),
    (N'Lên Hương', N'Cinema 7 - PREMIUM', 0, 1030),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 0, 1170),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 0, 1290),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 0, 1410),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 0, 740),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 0, 860),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 0, 975),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 0, 1090),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 8 - GOLD CLASS', 0, 1210),
    (N'Út Lan 2', N'Cinema 8 - GOLD CLASS', 0, 1350),
    (N'Hope Vùng Tử Địa', N'Cinema 9 - GOLD CLASS', 0, 770),
    (N'Bùa Yêu: Bí Mật Gia Tộc', N'Cinema 9 - GOLD CLASS', 0, 960),
    (N'Tàu Buôn Người', N'Cinema 9 - GOLD CLASS', 0, 1120),
    (N'Út Lan 2', N'Cinema 9 - GOLD CLASS', 0, 1240),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 9 - GOLD CLASS', 0, 1370),
    -- ngày thứ 2 trong lịch (ngày mai)
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 1', 1, 600),
    (N'Marine Yêu Dấu', N'Cinema 1', 1, 720),
    (N'Lên Hương', N'Cinema 1', 1, 850),
    (N'Lên Hương', N'Cinema 1', 1, 1000),
    (N'Út Lan 2', N'Cinema 1', 1, 1140),
    (N'Út Lan 2', N'Cinema 1', 1, 1270),
    (N'Út Lan 2', N'Cinema 1', 1, 1400),
    (N'Chiikawa: Bí Mật Đảo Người Cá', N'Cinema 2', 1, 610),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 2', 1, 730),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 2', 1, 870),
    (N'Út Lan 2', N'Cinema 2', 1, 1010),
    (N'Lên Hương', N'Cinema 2', 1, 1150),
    (N'Lên Hương', N'Cinema 2', 1, 1300),
    (N'Lên Hương', N'Cinema 3', 1, 615),
    (N'Bóng Ma Nhà Hát', N'Cinema 3', 1, 800),
    (N'Marine Yêu Dấu', N'Cinema 3', 1, 930),
    (N'Marine Yêu Dấu', N'Cinema 3', 1, 1050),
    (N'Lên Hương', N'Cinema 3', 1, 1180),
    (N'Út Lan 2', N'Cinema 3', 1, 1320),
    (N'Marine Yêu Dấu', N'Cinema 4', 1, 670),
    (N'Út Lan 2', N'Cinema 4', 1, 825),
    (N'Út Lan 2', N'Cinema 4', 1, 950),
    (N'Lên Hương', N'Cinema 4', 1, 1080),
    (N'Lên Hương', N'Cinema 4', 1, 1220),
    (N'Lên Hương', N'Cinema 4', 1, 1360),
    (N'Út Lan 2', N'Cinema 5', 1, 690),
    (N'Lên Hương', N'Cinema 5', 1, 900),
    (N'Út Lan 2', N'Cinema 5', 1, 1060),
    (N'Út Lan 2', N'Cinema 5', 1, 1200),
    (N'Bát Tiên Truy Tìm Lưu Ly Đăng', N'Cinema 5', 1, 1340),
    (N'Lên Hương', N'Cinema 6', 1, 750),
    (N'Yêu Nhân Thần Thám: Kỳ Án Trường An', N'Cinema 7 - PREMIUM', 1, 650),
    (N'Tàu Buôn Người', N'Cinema 7 - PREMIUM', 1, 790),
    (N'Chiikawa: Bí Mật Đảo Người Cá', N'Cinema 7 - PREMIUM', 1, 910),
    (N'Tế Nhi Cải Mệnh', N'Cinema 7 - PREMIUM', 1, 1040),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 1, 1170),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 1, 1290),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 1, 1410),
    (N'Hope Vùng Tử Địa', N'Cinema 8 - GOLD CLASS', 1, 630),
    (N'Bùa Yêu: Bí Mật Gia Tộc', N'Cinema 8 - GOLD CLASS', 1, 820),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 1, 975),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 1, 1090),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 1, 1210),
    (N'Hope Vùng Tử Địa', N'Cinema 8 - GOLD CLASS', 1, 1330),
    (N'Tế Nhi Cải Mệnh', N'Cinema 9 - GOLD CLASS', 1, 640),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 9 - GOLD CLASS', 1, 770),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 9 - GOLD CLASS', 1, 890),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 9 - GOLD CLASS', 1, 1010),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 9 - GOLD CLASS', 1, 1130),
    (N'Lên Hương', N'Cinema 9 - GOLD CLASS', 1, 1250),
    (N'Lên Hương', N'Cinema 9 - GOLD CLASS', 1, 1390),
    -- ngày thứ 3 trong lịch (ngày kia)
    (N'Marine Yêu Dấu', N'Cinema 1', 2, 600),
    (N'Marine Yêu Dấu', N'Cinema 1', 2, 720),
    (N'Lên Hương', N'Cinema 1', 2, 850),
    (N'Lên Hương', N'Cinema 1', 2, 990),
    (N'Marine Yêu Dấu', N'Cinema 1', 2, 1130),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 1', 2, 1260),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 1', 2, 1380),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 2', 2, 610),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 2', 2, 730),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 2', 2, 870),
    (N'Út Lan 2', N'Cinema 2', 2, 1010),
    (N'Lên Hương', N'Cinema 2', 2, 1140),
    (N'Út Lan 2', N'Cinema 2', 2, 1280),
    (N'Út Lan 2', N'Cinema 2', 2, 1410),
    (N'Út Lan 2', N'Cinema 3', 2, 615),
    (N'Lên Hương', N'Cinema 3', 2, 750),
    (N'Lên Hương', N'Cinema 3', 2, 900),
    (N'Lên Hương', N'Cinema 3', 2, 1050),
    (N'Út Lan 2', N'Cinema 3', 2, 1190),
    (N'Bát Tiên Truy Tìm Lưu Ly Đăng', N'Cinema 3', 2, 1320),
    (N'Út Lan 2', N'Cinema 4', 2, 670),
    (N'Bóng Ma Nhà Hát', N'Cinema 4', 2, 800),
    (N'Marine Yêu Dấu', N'Cinema 4', 2, 920),
    (N'Marine Yêu Dấu', N'Cinema 4', 2, 1060),
    (N'Lên Hương', N'Cinema 4', 2, 1200),
    (N'Lên Hương', N'Cinema 4', 2, 1340),
    (N'Lên Hương', N'Cinema 5', 2, 680),
    (N'Út Lan 2', N'Cinema 5', 2, 825),
    (N'Út Lan 2', N'Cinema 5', 2, 950),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 5', 2, 1080),
    (N'Út Lan 2', N'Cinema 5', 2, 1220),
    (N'Út Lan 2', N'Cinema 5', 2, 1350),
    (N'Chiikawa: Bí Mật Đảo Người Cá', N'Cinema 7 - PREMIUM', 2, 640),
    (N'Tế Nhi Cải Mệnh', N'Cinema 7 - PREMIUM', 2, 760),
    (N'Quý Tử Vượt Giàu', N'Cinema 7 - PREMIUM', 2, 890),
    (N'Tế Nhi Cải Mệnh', N'Cinema 7 - PREMIUM', 2, 1040),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 2, 1170),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 2, 1290),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 2, 1410),
    (N'Tàu Buôn Người', N'Cinema 8 - GOLD CLASS', 2, 620),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 2, 740),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 2, 860),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 2, 975),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 2, 1090),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 2, 1210),
    (N'Hope Vùng Tử Địa', N'Cinema 8 - GOLD CLASS', 2, 1330),
    (N'Yêu Nhân Thần Thám: Kỳ Án Trường An', N'Cinema 9 - GOLD CLASS', 2, 630),
    (N'Hope Vùng Tử Địa', N'Cinema 9 - GOLD CLASS', 2, 770),
    (N'Bùa Yêu: Bí Mật Gia Tộc', N'Cinema 9 - GOLD CLASS', 2, 960),
    (N'Tàu Buôn Người', N'Cinema 9 - GOLD CLASS', 2, 1120),
    (N'Lên Hương', N'Cinema 9 - GOLD CLASS', 2, 1240),
    (N'Lên Hương', N'Cinema 9 - GOLD CLASS', 2, 1390),
    -- ngày thứ 4 trong lịch (ngày thứ tư)
    (N'Marine Yêu Dấu', N'Cinema 1', 3, 600),
    (N'Marine Yêu Dấu', N'Cinema 1', 3, 720),
    (N'Lên Hương', N'Cinema 1', 3, 850),
    (N'Lên Hương', N'Cinema 1', 3, 1000),
    (N'Út Lan 2', N'Cinema 1', 3, 1140),
    (N'Út Lan 2', N'Cinema 1', 3, 1270),
    (N'Út Lan 2', N'Cinema 1', 3, 1400),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 2', 3, 610),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 2', 3, 730),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 2', 3, 870),
    (N'Út Lan 2', N'Cinema 2', 3, 1010),
    (N'Lên Hương', N'Cinema 2', 3, 1150),
    (N'Lên Hương', N'Cinema 2', 3, 1300),
    (N'Út Lan 2', N'Cinema 3', 3, 615),
    (N'Lên Hương', N'Cinema 3', 3, 750),
    (N'Lên Hương', N'Cinema 3', 3, 900),
    (N'Lên Hương', N'Cinema 3', 3, 1050),
    (N'Marine Yêu Dấu', N'Cinema 3', 3, 1190),
    (N'Bát Tiên Truy Tìm Lưu Ly Đăng', N'Cinema 3', 3, 1320),
    (N'Út Lan 2', N'Cinema 4', 3, 670),
    (N'Bóng Ma Nhà Hát', N'Cinema 4', 3, 800),
    (N'Chiikawa: Bí Mật Đảo Người Cá', N'Cinema 4', 3, 930),
    (N'Marine Yêu Dấu', N'Cinema 4', 3, 1060),
    (N'Lên Hương', N'Cinema 4', 3, 1200),
    (N'Lên Hương', N'Cinema 4', 3, 1340),
    (N'Lên Hương', N'Cinema 5', 3, 680),
    (N'Út Lan 2', N'Cinema 5', 3, 825),
    (N'Út Lan 2', N'Cinema 5', 3, 950),
    (N'Út Lan 2', N'Cinema 5', 3, 1080),
    (N'Út Lan 2', N'Cinema 5', 3, 1220),
    (N'Út Lan 2', N'Cinema 5', 3, 1360),
    (N'Quý Tử Vượt Giàu', N'Cinema 7 - PREMIUM', 3, 650),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 3, 790),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 3, 910),
    (N'Tế Nhi Cải Mệnh', N'Cinema 7 - PREMIUM', 3, 1030),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 3, 1170),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 3, 1290),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 3, 1410),
    (N'Tàu Buôn Người', N'Cinema 8 - GOLD CLASS', 3, 620),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 3, 740),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 3, 860),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 3, 975),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 3, 1090),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 8 - GOLD CLASS', 3, 1210),
    (N'Hope Vùng Tử Địa', N'Cinema 8 - GOLD CLASS', 3, 1350),
    (N'Yêu Nhân Thần Thám: Kỳ Án Trường An', N'Cinema 9 - GOLD CLASS', 3, 630),
    (N'Hope Vùng Tử Địa', N'Cinema 9 - GOLD CLASS', 3, 770),
    (N'Bùa Yêu: Bí Mật Gia Tộc', N'Cinema 9 - GOLD CLASS', 3, 960),
    (N'Tàu Buôn Người', N'Cinema 9 - GOLD CLASS', 3, 1120),
    (N'Lên Hương', N'Cinema 9 - GOLD CLASS', 3, 1240),
    (N'Lên Hương', N'Cinema 9 - GOLD CLASS', 3, 1390),
    -- ngày thứ 5 trong lịch (ngày thứ năm)
    (N'Marine Yêu Dấu', N'Cinema 1', 4, 600),
    (N'Marine Yêu Dấu', N'Cinema 1', 4, 720),
    (N'Lên Hương', N'Cinema 1', 4, 850),
    (N'Lên Hương', N'Cinema 1', 4, 1000),
    (N'Hòn Đảo Quên Lãng', N'Cinema 1', 4, 1185),
    (N'Lên Hương', N'Cinema 1', 4, 1340),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 2', 4, 610),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 2', 4, 730),
    (N'Nghỉ Hè Sợ Nghỉ Hưu', N'Cinema 2', 4, 870),
    (N'Hòn Đảo Quên Lãng', N'Cinema 2', 4, 1010),
    (N'Lên Hương', N'Cinema 2', 4, 1200),
    (N'Út Lan 2', N'Cinema 2', 4, 1360),
    (N'Út Lan 2', N'Cinema 3', 4, 615),
    (N'Lên Hương', N'Cinema 3', 4, 750),
    (N'Lên Hương', N'Cinema 3', 4, 900),
    (N'Lên Hương', N'Cinema 3', 4, 1050),
    (N'Trại Buôn Người', N'Cinema 3', 4, 1210),
    (N'Trại Buôn Người', N'Cinema 3', 4, 1370),
    (N'Hòn Đảo Quên Lãng', N'Cinema 4', 4, 660),
    (N'Bóng Ma Nhà Hát', N'Cinema 4', 4, 800),
    (N'Marine Yêu Dấu', N'Cinema 4', 4, 920),
    (N'Marine Yêu Dấu', N'Cinema 4', 4, 1060),
    (N'Lên Hương', N'Cinema 5', 4, 680),
    (N'Út Lan 2', N'Cinema 5', 4, 820),
    (N'Út Lan 2', N'Cinema 5', 4, 950),
    (N'Út Lan 2', N'Cinema 5', 4, 1080),
    (N'Chiikawa: Bí Mật Đảo Người Cá', N'Cinema 7 - PREMIUM', 4, 640),
    (N'Tế Nhi Cải Mệnh', N'Cinema 7 - PREMIUM', 4, 760),
    (N'Quý Tử Vượt Giàu', N'Cinema 7 - PREMIUM', 4, 890),
    (N'Út Lan 2', N'Cinema 7 - PREMIUM', 4, 1035),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 7 - PREMIUM', 4, 1380),
    (N'Tàu Buôn Người', N'Cinema 8 - GOLD CLASS', 4, 620),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 4, 740),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 4, 860),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 4, 980),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 8 - GOLD CLASS', 4, 1100),
    (N'Út Lan 2', N'Cinema 8 - GOLD CLASS', 4, 1220),
    (N'Trại Buôn Người', N'Cinema 8 - GOLD CLASS', 4, 1400),
    (N'Yêu Nhân Thần Thám: Kỳ Án Trường An', N'Cinema 9 - GOLD CLASS', 4, 630),
    (N'Hope Vùng Tử Địa', N'Cinema 9 - GOLD CLASS', 4, 770),
    (N'Bùa Yêu: Bí Mật Gia Tộc', N'Cinema 9 - GOLD CLASS', 4, 960),
    (N'Trại Buôn Người', N'Cinema 9 - GOLD CLASS', 4, 1120),
    (N'Vùng Đất Quỷ Dữ 2026', N'Cinema 9 - GOLD CLASS', 4, 1280);

INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price)
SELECT
    m.id,
    r.id,
    DATEADD(MINUTE, p.minuteOfDay, DATEADD(DAY, p.dayOffset, @ngayMai)),
    -- Kết thúc = bắt đầu + thời lượng phim + 15 phút dọn phòng
    DATEADD(MINUTE, m.duration_min + 15,
            DATEADD(MINUTE, p.minuteOfDay, DATEADD(DAY, p.dayOffset, @ngayMai))),
    CASE
        WHEN r.name LIKE N'%GOLD CLASS%' THEN 200000
        WHEN r.name LIKE N'%PREMIUM%'    THEN 150000
        -- DATEDIFF(DAY, 0, ...) % 7 cho 0 = Thứ Hai ... 6 = Chủ Nhật,
        -- không phụ thuộc cài đặt DATEFIRST của máy chủ.
        ELSE CASE DATEDIFF(DAY, 0, DATEADD(DAY, p.dayOffset, @ngayMai)) % 7
                 WHEN 2 THEN 79000    -- Thứ Tư Vui Vẻ
                 WHEN 4 THEN 135000   -- Thứ Sáu
                 WHEN 5 THEN 135000   -- Thứ Bảy
                 WHEN 6 THEN 135000   -- Chủ Nhật
                 ELSE 115000          -- Thứ Hai, Thứ Ba, Thứ Năm
             END
    END
FROM @plannedShowtimes p
JOIN movies m ON m.title = p.movieTitle
JOIN rooms  r ON r.name  = p.roomName
WHERE NOT EXISTS (
    SELECT 1 FROM showtimes s
    WHERE s.movie_id = m.id
      AND s.room_id = r.id
      AND s.start_time = DATEADD(MINUTE, p.minuteOfDay, DATEADD(DAY, p.dayOffset, @ngayMai))
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
