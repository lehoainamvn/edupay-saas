Role: Principal SaaS Product & UI/UX Designer.
Task: Design a high-converting, tactile, Mobile-First B2B SaaS screen for "Quản lý Lớp học (Class Management)" inside the "EduPay" web application (Nền tảng Quản lý Điểm danh & Thu học phí VietQR tự động dành cho giáo viên dạy thêm và gia sư độc lập tại Việt Nam).
Viewport: Desktop (1440px width, 12-column grid, 80px margins, 24px gutters) + Mobile-First Responsive (390px, touch targets >= 48px, zero horizontal table scrolling).

Aesthetic & Design System Constraints (STRICTLY SYNCHRONIZED WITH EDUPAY LANDING PAGE):
- Style: Minimalist, clean, utilitarian, high-trust fintech & edtech product (inspired by Stripe, Linear, and Notion).
- Forbidden Clichés: NO neon purple/cyan floating blobs, NO heavy glassmorphism, NO wide horizontal tables on mobile, NO generic English Lorem Ipsum (100% authentic Vietnamese teacher terminology).
- Color Tokens:
  * Primary Canvas: #FAF8F9 (Warm Off-white/Stone 50) and #FFFFFF (Pure White elevated cards).
  * Secondary Surface: #F1F5F9 (Slate 100).
  * Text Primary: #0F172A (Deep Slate 900), Text Muted: #64748B (Slate 500).
  * Brand Accent: #2563EB (Electric Royal Blue 600) + #1D4ED8 (Hover State) + #EFF6FF (Soft Blue 50).
  * Semantic Status Tokens:
    - Active / Đang mở / Theo buổi: Background #DCFCE7, Text #166534, Border #86EFAC.
    - Trọn gói tháng / Chờ xử lý: Background #FEF9C3, Text #854D0E, Border #FDE047.
    - Lưu trữ (Soft Delete) / Báo lỗi form: Background #FEE2E2, Text #991B1B, Border #FCA5A5.
  * Border Token: 1px solid #E2E8F0. Card Radius: 18px. Modal Radius: 24px. Pill Radius: 999px.
  * Elevation Shadows: Card default `0 1px 3px rgba(0,0,0,0.05)`, Card hover `0 15px 35px rgba(37,99,235,0.08)`, Modal `0 22px 60px rgba(15,23,42,0.22)`.
- Typography: Be Vietnam Pro / Inter. Strict `font-variant-numeric: tabular-nums` on all tuition fees (150.000 đ, 1.200.000 đ), student counts, and KPI metrics.

Detailed Screen Architecture:

1. Sticky App Topbar (Height: 72px, blur backdrop: rgba(250,248,249,0.88), 1px bottom border #E2E8F0):
   - Left: Brand Logo "EduPay" (Blue 32x32px rounded square with white graduation cap stroke SVG + bold wordmark) + Pill badge "SaaS v1.0".
   - Center Navigation Pills: "Tổng quan", "Lớp học" (Active state: #EFF6FF background, #2563EB bold text), "Học sinh", "Điểm danh 1 chạm", "Học phí VietQR".
   - Right Tenant Badge: Green pulsing dot + "Cô Nguyễn Thị Mai · Gói Thuê Tháng (Đã xác thực)".

2. Page Header & Primary Action Bar:
   - Eyebrow Pill Badge: Green dot + "QUẢN LÝ LỚP HỌC · MULTI-TENANT ISOLATION".
   - Page Title (H1, 36px ExtraBold): "Danh sách lớp giảng dạy".
   - Subtitle (15px Slate 500): "Thiết lập hình thức thu học phí Theo buổi hoặc Trọn tháng cùng đơn giá mặc định để hệ thống tự động nhân số buổi điểm danh và sinh phiếu thu VietQR cuối kỳ."
   - Right Primary CTA Button (52px height, rounded-full, #2563EB fill): "+ Tạo lớp học mới".

3. 4-Card Summary KPI Strip (Tabular Numerals):
   - Card 1 (LỚP ĐANG MỞ): "3 lớp hoạt động" — Tự động đồng bộ danh sách điểm danh.
   - Card 2 (TỔNG SĨ SỐ ĐANG HỌC): "24 học sinh" (#2563EB highlight) — Phân tách bảo mật theo giáo viên.
   - Card 3 (HÌNH THỨC HỌC PHÍ): "2 theo buổi · 1 trọn tháng" — Chuẩn đối soát VietQR Napas247.
   - Card 4 (LƯU TRỮ LỊCH SỬ): "1 lớp đã đóng" — Bảo toàn 100% lịch sử điểm danh & biên lai.

4. Search & Filter Toolbar Card (White surface, 18px radius):
   - Left: Search input (48px height) with magnifier icon: "Tìm theo tên lớp hoặc lịch học (VD: Toán 8A, Thứ 3...)".
   - Middle Segmented Tabs (44px height):
     * "Đang mở (3)" [Active]
     * "Đã lưu trữ (1)" [Soft-deleted classes]
     * "Tất cả (4)"
   - Right Filter Pills: "Mọi hình thức" | "Theo buổi" | "Trọn tháng".

5. Large Tactile Class Card Grid (Card UI to dễ bấm — 3 columns on Desktop, 1 column vertical stack on Mobile):
   - Card 1: "Toán nâng cao 8A"
     * Top Row: Badge "⚡ Thu theo buổi" (#EFF6FF / #2563EB) + Status Pill "● Đang học" (#DCFCE7 / #166534).
     * Title: "Toán nâng cao 8A" (21px Bold) + Subtitle: "Lớp bồi dưỡng học sinh giỏi Toán lớp 8 · Thứ 3 & Thứ 7 (18:00 - 19:30)".
     * Highlighted Tuition Box (#F8FAFC background, 14px radius):
       - Left: ĐƠN GIÁ MẶC ĐỊNH: "150.000 đ / buổi" (22px ExtraBold #2563EB, tabular-nums).
       - Right: SĨ SỐ HIỆN TẠI: "4 học sinh".
     * Bottom Touch Action Row (All buttons min-height 48px, 10px gap):
       - Primary Button (#2563EB): "Điểm danh ngay →"
       - Secondary Outlined Button: "✎ Chỉnh sửa"
       - Soft Delete Button (#FEE2E2 border, #991B1B text): "Lưu trữ"
   - Card 2: "Luyện thi vào 10 - Nhóm Chuyên"
     * Badge "⚡ Thu theo buổi", Đơn giá: "180.000 đ / buổi", Sĩ số: "12 học sinh", Lịch: "Thứ 2, Thứ 5 & Chủ Nhật (17:30 - 19:30)".
   - Card 3: "Toán Tư Duy Lớp 6B (Cơ bản)"
     * Badge "📦 Trọn gói tháng" (#FEF9C3 / #854D0E), Đơn giá: "1.200.000 đ / tháng", Sĩ số: "8 học sinh", Lịch: "Thứ 4 & Thứ 6 (18:00 - 19:30)".
   - Card 4 (Archived / Soft-Deleted State): "Khóa Hè Toán 7 Lên 8 (2026)"
     * Dashed border, muted background #F8FAFC, Badge "Lưu trữ · Giữ lịch sử".
     * Notice banner: "✓ Lịch sử điểm danh & phiếu thu được bảo toàn".
     * Action buttons: "✎ Xem / Sửa" + "↺ Khôi phục lớp" (Green button).

6. Interactive Modal Popup: "Tạo lớp học mới / Chỉnh sửa lớp học" (580px max-width, 24px radius, elevated shadow):
   - Modal Header: Kicker "THIẾT LẬP LỚP HỌC MỚI", Title "Tạo lớp học mới", Close button (44x44px circle "✕").
   - Field 1: "Tên lớp học *" — Input 48px height, placeholder "VD: Toán nâng cao 8A, Luyện thi vào 10 Nhóm B...", with inline red validation message state below input.
   - Field 2: "Hình thức thu học phí *" — 2 Large Tactile Radio Cards side-by-side:
     * Card A (Selected - #EFF6FF fill, 1.5px solid #2563EB border): "⚡ Theo buổi — Tính theo số buổi thực tế (Cuối tháng tự nhân: Số buổi Có mặt × Đơn giá buổi)".
     * Card B: "📦 Trọn tháng — Thu trọn gói cố định tháng (Giữ nguyên mức học phí cố định mỗi kỳ thu tiền)".
   - Field 3: "Đơn giá mặc định (VNĐ / buổi) *" — Number input (48px height, tabular-nums, "150.000 đ / buổi") + Quick 1-Tap Preset Chips row: `[100.000 đ]` `[120.000 đ]` `[150.000 đ - Active]` `[180.000 đ]` `[200.000 đ]` `[250.000 đ]`.
   - Field 4: "Lịch học trong tuần & Ghi chú" — Textarea input.
   - Live VietQR Billing Formula Preview Box (#F0FDF4 green background, #BBF7D0 border):
     * Label: "MÔ PHỎNG PHIẾU THU VIETQR CUỐI THÁNG"
     * Text: "Học sinh đi học 8 buổi × 150.000 đ = 1.200.000 đ"
   - Modal Footer: "Hủy bỏ" (Secondary pill button, 48px) + "Tạo lớp học ngay →" (Primary blue pill button, 48px).

7. Soft Delete Confirmation Modal ("Lưu trữ an toàn · Giữ lịch sử"):
   - Explains clearly that archiving sets `is_active = false` without deleting historical attendance records or VietQR invoices, with "Giữ lớp đang mở" and "Xác nhận lưu trữ lớp" buttons.
