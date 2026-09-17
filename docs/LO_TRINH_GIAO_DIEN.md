# Lo trinh lam giao dien dung chung (Module 4 - Tho)

Moi ngay lam DUNG 1 viec nho nhung **hoan chinh** (code xong - chay thu xong - commit xong),
khong de viec do dang qua ngay hom sau.

## Dinh huong thiet ke

Lay y tuong tu **logo truong (HCMUTE)**: mau chu dao **xanh duong + trang**, hoa tiet
**banh rang - compa - sach mo** (ky thuat, chinh xac, tri thuc). Them tong **vang ho phach**
cho ra chat rap chieu phim (ve, den vang, ghe nhung).

Nguyen tac khong duoc pha:
- **Dep nhung khong lam sai logic.** Animation chi de dan mat nguoi dung, khong duoc lam
  cham thao tac hay che mat thong tin.
- Moi hieu ung deu phai tat duoc: ai bat `prefers-reduced-motion` trong he dieu hanh thi
  trang tu dong bo animation (nguoi bi say chuyen dong / roi loan tien dinh can dieu nay).
- Chi dung **bien CSS** trong `:root`, khong go thang ma mau vao tung class -> sau nay doi
  tong mau ca trang chi sua 1 cho.
- Khong keo them thu vien CSS ngoai (Bootstrap, Tailwind...): de bai cham ky nang tu viet,
  va them thu vien la them rui ro xung dot khi 4 nguoi cung sua.

## Lo trinh 7 ngay

| Ngay | Viec | Ket qua ban giao | Ai dung ket qua nay |
|---|---|---|---|
| **1** | **He mau + typography + animation nen** | `style.css` thanh mot bo design system: bien mau theo logo truong, thang spacing, bo shadow, font, hieu ung vao trang. Header/footer thiet ke lai | Ca 4 nguoi |
| 2 | Bo component dung chung | Nut, the (card), o nhap lieu, bang, nhan trang thai, hop thoai - kem trang demo `/ui-kit` de ca nhom xem va copy | Ca 4 nguoi |
| 3 | Trang chu | Hero + luoi phim dang chieu (con dung du lieu gia cho den khi Tai xong Module 1) | Tai |
| 4 | So do ghe (seat-map) | CSS + animation chon ghe, trang thai trong / dang giu / da ban, chu thich mau | Thang |
| 5 | Khu vuc quan tri | Layout trang admin + bang du lieu, phan trang, thanh loc | Tai |
| 6 | Form & thanh toan | Dang nhap / dang ky / xac nhan thanh toan, kem trang thai loi va dang xu ly | Thanh |
| 7 | Ra soat cuoi | Responsive tren dien thoai + kiem tra do tuong phan mau, vien focus khi di ban phim + viet huong dan dung design system | Ca 4 nguoi |

## Tien do

- [x] **Ngay 1** - 17/09: he mau theo logo truong, typography, bo animation nen, header + footer moi,
      trang chu va trang loi ap dung giao dien moi.
- [ ] Ngay 2 - bo component + trang `/ui-kit`
- [ ] Ngay 3 - trang chu hoan chinh
- [ ] Ngay 4 - so do ghe
- [ ] Ngay 5 - khu vuc quan tri
- [ ] Ngay 6 - form & thanh toan
- [ ] Ngay 7 - ra soat responsive + tuong phan mau

## Bang mau (Ngay 1 da chot)

| Bien | Ma mau | Dung o dau |
|---|---|---|
| `--brand-900` | `#0a1a3c` | Nen header/footer, chu tieu de dam |
| `--brand-700` | `#123a7a` | Nen phu, hover |
| `--brand-500` | `#1b5fd0` | Mau chinh: nut, link, vien focus |
| `--brand-300` | `#7aa9f0` | Nhan, vien nhat |
| `--accent-500` | `#f0a828` | Diem nhan: gia ve, sao danh gia, ghe VIP |
| `--seat-held` | `#f0a828` | Ghe dang co nguoi giu |
| `--seat-booked` | `#94a0b8` | Ghe da ban |
| `--ok-500` / `--danger-500` | `#1c8a52` / `#c8362f` | Thong bao thanh cong / loi |

Mau chu tren nen da duoc kiem tra do tuong phan dat chuan WCAG AA (>= 4.5:1) o Ngay 7.
