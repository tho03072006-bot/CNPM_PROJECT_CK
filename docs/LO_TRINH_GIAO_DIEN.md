# Lộ trình làm giao diện dùng chung (Module 4 - Thọ)

Mỗi ngày làm ĐÚNG 1 việc nhỏ nhưng **hoàn chỉnh** (code xong - chạy thử xong - commit xong),
không để việc dở dang qua ngày hôm sau.

## Định hướng thiết kế

Không khí **rạp chiếu phim** — phòng tối, màn hình sáng, đèn vàng, vé xé răng cưa — nhưng dùng
bộ màu lấy từ **logo trường (HCMUTE)**: xanh dương + trắng, hoạ tiết bánh răng - compa - sách mở.
Thêm tông **vàng hổ phách** làm màu điểm nhấn cho ra chất rạp phim.

Nguyên tắc không được phá:

- **Đẹp nhưng không làm sai logic.** Animation chỉ để dẫn mắt người dùng, không được làm
  chậm thao tác hay che mất thông tin.
- Mọi hiệu ứng đều phải tắt được: ai bật `prefers-reduced-motion` trong hệ điều hành thì
  trang tự động bỏ animation (người bị say chuyển động / rối loạn tiền đình cần điều này).
- **Hai chế độ sáng và tối**, người dùng tự bấm chọn và hệ thống nhớ lựa chọn đó. Chưa chọn
  bao giờ thì đi theo cài đặt của hệ điều hành.
- Chỉ dùng **biến CSS** trong `:root`, không gõ thẳng mã màu vào từng class → sau này đổi
  tông màu cả trang chỉ sửa 1 chỗ, và chế độ tối cũng chỉ là đổi giá trị biến.
- Không kéo thêm thư viện CSS ngoài (Bootstrap, Tailwind...): đề bài chấm kỹ năng tự viết,
  và thêm thư viện là thêm rủi ro xung đột khi 4 người cùng sửa.

## Lộ trình 7 ngày

| Ngày | Việc | Kết quả bàn giao | Ai dùng kết quả này |
|---|---|---|---|
| **1** | Hệ màu + typography + animation nền + 2 chế độ sáng/tối | `style.css` thành bộ design system, nút đổi sáng/tối ở header, header/footer/trang chủ/trang lỗi thiết kế lại | Cả 4 người |
| **2** | Bộ component dùng chung | Nút, thẻ, thẻ phim, vé, ô nhập liệu, bảng, nhãn trạng thái, sơ đồ ghế mẫu — kèm trang `/ui-kit` để cả nhóm xem và chép | Cả 4 người |
| 3 | Trang chủ | Hero + lưới phim đang chiếu, nối vào dữ liệu thật khi Module 1 xong | Tài |
| 4 | Sơ đồ ghế hoàn chỉnh | Trang chọn ghế, đếm ngược thời gian giữ ghế, tổng tiền | Thắng |
| 5 | Khu vực quản trị | Layout trang admin + bảng dữ liệu, phân trang, thanh lọc | Tài |
| 6 | Form & thanh toán | Đăng nhập / đăng ký / xác nhận thanh toán, kèm trạng thái lỗi và đang xử lý | Thanh |
| 7 | Rà soát cuối | Responsive trên điện thoại + kiểm tra độ tương phản màu, viền focus khi đi bằng bàn phím + viết hướng dẫn dùng design system | Cả 4 người |

## Tiến độ

- [x] **Ngày 1** — 17/09: hệ màu theo logo trường, typography, bộ animation nền, **hai chế độ
      sáng/tối có nút bấm và nhớ lựa chọn**, header + footer + trang chủ + trang lỗi thiết kế lại.
- [x] **Ngày 2** — 18/09: bộ component (nút, nhãn, thẻ, thẻ phim, vé, form, bảng, sơ đồ ghế,
      trạng thái đang tải) + trang `/ui-kit`.
- [x] **Ngày 7 (làm sớm)** — 18/09: rà soát responsive + độ tương phản màu + viền focus bàn phím.
      Kết quả đo được ghi ở mục "Kết quả rà soát" bên dưới.
- [ ] Ngày 3 — trang chủ nối dữ liệu phim thật *(chờ Tài làm xong `MovieService` — M1.1)*
- [ ] Ngày 4 — sơ đồ ghế hoàn chỉnh *(chờ Thắng làm xong `SeatService` — M2.1)*
- [ ] Ngày 5 — khu vực quản trị *(chờ Tài — M1.4)*
- [ ] Ngày 6 — form & thanh toán *(chờ Thanh — M3.5)*

Ngày 7 được làm sớm vì 4 ngày còn lại đều phải chờ dữ liệu của người khác, trong khi rà soát
chất lượng thì làm được ngay trên những gì đã có.

## Kết quả rà soát (18/09)

Đo trên trang `/` và `/ui-kit`, cả hai chế độ sáng và tối:

| Hạng mục | Kết quả |
|---|---|
| Tràn ngang ở khổ điện thoại 375px | Không có, ở cả 2 trang |
| Vùng chạm của ghế trên điện thoại | 32×32px — đạt mức tối thiểu |
| Độ tương phản màu, chế độ sáng | **20/20 đạt** chuẩn WCAG AA, thấp nhất 4.89:1 |
| Độ tương phản màu, chế độ tối | **20/20 đạt** chuẩn WCAG AA, thấp nhất 5.84:1 |
| Viền focus khi đi bằng phím Tab | Thấy rõ trên mọi nút, link và ô nhập liệu |

### Năm lỗi tương phản đã sửa trong đợt rà soát này

Cả năm lỗi này nhìn bằng mắt đều thấy "hơi khó đọc" chứ không ai nghĩ là sai chuẩn —
phải đo bằng số mới lòi ra:

| Chỗ | Trước | Sau | Cách sửa |
|---|---|---|---|
| Chữ trên nút vàng (chế độ tối) | 1.18:1 | 8.43:1 | Bỏ selector `[data-theme="dark"] a`, cho màu link đi qua biến `--link` |
| Nhãn vàng `.badge-accent` | 3.43:1 | 4.89:1 | Thêm `--accent-600` đậm hơn, chỉ dùng làm màu chữ |
| Nhãn xanh lá `.badge-ok` | 3.94:1 | 5.5:1 | Thêm `--ok-700` |
| Ghế đã bán | 3.01:1 | 5.07:1 | Đổi `--seat-booked-bg` từ `#8a95ad` sang `#646e8a` |
| Chữ báo lỗi trong form (chế độ tối) | 3.40:1 | ~7:1 | Chế độ tối dùng đỏ nhạt `#ff9d97` |
| Ghế đang chọn (chế độ tối) | 4.00:1 | 5.84:1 | Bỏ `#3d7ce8`, dùng chung `#1b5fd0` với chế độ sáng |

Và một lỗi về viền focus: ô nhập liệu để `outline: none`, chỉ dựa vào quầng `box-shadow`
màu `--tint-brand` — ở chế độ tối quầng này chỉ tương phản **1.1:1** với nền ô, tức là
người dùng bàn phím không thấy mình đang đứng ở ô nào. Đã thêm `.form-control:focus-visible`
có viền vàng đậm.

## Bảng màu

| Biến | Chế độ sáng | Chế độ tối | Dùng ở đâu |
|---|---|---|---|
| `--brand-900` | `#0a1a3c` | `#0a1a3c` | Nền header/footer, chữ trên nút vàng |
| `--brand-500` | `#1b5fd0` | `#1b5fd0` | Màu chính: nút, viền focus |
| `--brand-300` | `#7aa9f0` | `#7aa9f0` | Màn chiếu, nhãn nhạt |
| `--accent-500` | `#f0a828` | `#f0a828` | Điểm nhấn: nút đặt vé, ghế đang giữ, đèn viền hero |
| `--bg` | `#eef1f8` | `#070b16` | Nền trang |
| `--surface` | `#ffffff` | `#101829` | Nền thẻ, bảng, ô nhập liệu |
| `--text` | `#131a2b` | `#e9eefb` | Chữ chính |
| `--text-muted` | `#58627d` | `#9aa8c6` | Chữ phụ |
| `--link` | `#1b5fd0` | `#7aa9f0` | Màu link |

## Bài học rút ra khi làm (ghi lại để không dẫm lại)

**Đừng viết `[data-theme="dark"] a { color: ... }`.** Selector đó có độ ưu tiên (0,1,1), cao hơn
`.btn-accent` (0,1,0), nên nó đè luôn màu chữ của mọi nút viết bằng thẻ `<a>`. Hậu quả: nút vàng
"Xem phim đang chiếu" ở chế độ tối có chữ xanh nhạt trên nền vàng, tỉ số tương phản tụt còn
**1.18:1** (chuẩn tối thiểu là 4.5:1) — nhìn bằng mắt thường vẫn thấy "hơi khó đọc" chứ không
nghĩ là lỗi. Cách đúng: cho màu link đi qua biến `--link`, rồi chỉ viết `a { color: var(--link); }`
với độ ưu tiên (0,0,1) — thua mọi class nút.

Bài học chung: **đổi màu theo chế độ thì đổi GIÁ TRỊ BIẾN, đừng thêm selector mới.**
Và phải đo tỉ số tương phản bằng số, đừng tin mắt.
