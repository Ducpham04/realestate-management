# Design system

Giao diện là một **sản phẩm vận hành** (dashboard, admin): người dùng vào để làm việc, không phải để được gây ấn tượng. Theo hướng dẫn của [Impeccable](https://github.com/pbakaus/impeccable) cho bề mặt "Operate": giữ khung ứng dụng và các control quen thuộc. Bản sắc chỉ nằm ở **chữ, bảng màu, mật độ thông tin và một điểm nhấn**.

## Chữ

- **Một họ chữ duy nhất: Be Vietnam Pro** (400 / 500 / 600), font tự host qua `@fontsource`, có đủ subset tiếng Việt. Họ chữ này được thiết kế riêng cho dấu tiếng Việt nên dấu chồng (ặ, ổ, ữ) không bị chật.
- `line-height` cơ sở là 1.55 để có chỗ cho dấu. Tiêu đề dùng `letter-spacing: -0.02em` và `text-wrap: balance`, đoạn văn dùng `text-wrap: pretty`.
- Phân cấp bằng **cỡ chữ và độ đậm**, không dùng gradient hay màu.
- Tiêu đề cột bảng viết thường theo câu, không IN HOA, vì chữ in hoa tiếng Việt làm dấu bị dồn.
- Bảng và chỉ số dùng `font-variant-numeric: tabular-nums` (class `.tabular`) để các con số thẳng cột.

| Vai trò | Class |
|---------|-------|
| Tiêu đề trang | `text-2xl font-semibold` |
| Tiêu đề khối | `font-semibold` (16px) |
| Nội dung, control | `text-sm` (14px, mật độ bảng) |
| Phụ, chú thích | `text-sm text-slate-500/600` |

## Màu

Mọi màu được viết bằng **OKLCH** trong `tailwind.config.js`, để các bước độ sáng đều nhau về cảm nhận.

- **`brand` (hue 185, xanh mòng két).** Chỉ dùng cho hành động, lựa chọn, focus và điều hướng. Vì hiếm nên màu này có sức nhấn.
- **`slate` là thang trung tính pha một chút hue của brand** (chroma ≤ 0.014), nên giao diện liền mạch mà không bị ám màu. Từ `slate-500` trở xuống đạt tỉ lệ tương phản ≥ 4.5:1 trên nền trắng, áp dụng cho chữ phụ và placeholder.
- **Trạng thái.** Màu trạng thái luôn đi kèm nhãn chữ (badge), không bao giờ dùng màu làm tín hiệu duy nhất. Màu trạng thái BĐS trong biểu đồ khác nhau cả về **độ sáng**, không chỉ về hue.
- `::selection`, `accent-color`, viền focus và thanh cuộn đều lấy từ bảng màu.

## Bố cục và bề mặt

- Khoảng cách theo thang 4px của Tailwind. Dùng `gap` cho nhịp giữa các phần tử ngang hàng.
- Card bo góc 12px (`rounded-xl`) và **chỉ dùng một tín hiệu nổi khối**: viền 1px, không đổ bóng. Viền 1px cộng bóng mềm sẽ tạo ra "ghost card". Không lồng card trong card.
- Chỉ số KPI nằm trong **một dải chia ô** (`gap-px`), không phải bốn card có ô icon màu.
- Biểu đồ: cột màu brand, đường hoa hồng màu chàm đậm. Lưới chỉ có đường ngang, không nét đứt.

## Chuyển động

- 150 ms cho phản hồi tức thời (hover, màu nút). 220 ms cho dialog xuất hiện với `cubic-bezier(0.16, 1, 0.3, 1)`. Chỉ animate `opacity` và `transform`.
- `prefers-reduced-motion`: dialog chỉ còn hiệu ứng mờ dần, các transition gần như tắt.

## Ngôn từ

- Nút trong hộp xác nhận nói rõ hành động, ví dụ "Xóa bất động sản", không dùng "OK" hay "Có". Nút thoát là "Quay lại".
- Trạng thái rỗng phân biệt *chưa có dữ liệu* (kèm nút tạo mới) với *không có kết quả theo bộ lọc* (kèm nút xóa bộ lọc).
- Dùng một danh từ cố định cho một khái niệm: *bất động sản*, *giao dịch*, *lịch hẹn*.

## Không dùng

Chữ gradient · glassmorphism, blur trang trí · viền trái màu trên card hoặc alert · bóng đổ cứng lệch · glow · nhãn kicker/eyebrow phía trên tiêu đề · emoji thay icon (chỉ dùng `lucide-react`) · gradient tím-xanh.
