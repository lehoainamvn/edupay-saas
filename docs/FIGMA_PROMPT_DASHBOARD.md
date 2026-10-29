Role: Principal SaaS Product & UI/UX Designer.
Task: Design an intuitive, high-efficiency, tactile B2B SaaS Teacher Dashboard ("Trang Chủ Sau Khi Đăng Nhập - Tổng quan /dashboard") for "EduPay" (Nền tảng Quản lý Điểm danh & Đối soát Học phí VietQR tự động dành cho giáo viên dạy thêm và gia sư độc lập tại Việt Nam).
Viewport: Desktop (1440px width, 12-column grid, 80px margins, 24px gutters) + Mobile-First Responsive (390px, touch targets >= 48px, zero horizontal scrolling).

================================================================================
🚨 BẮT BUỘC TUÂN THỦ LUẬT P0 SỐ 6 (REUSE-FIRST & ANTI-DUPLICATION LAW):
- KHÔNG ĐƯỢC TỰ Ý TẠO MỚI HOẶC THAY THẾ THANH NAVIGATION: Phải TÁI SỬ DỤNG 100% thanh App Topbar (NavbarComponent mode="app") đã được chuẩn hóa thống nhất toàn hệ thống. Tuyệt đối KHÔNG vẽ Sidebar trái, KHÔNG đổi bố cục thanh điều hướng.
- KHÔNG DÙNG CLICHÉ AI: Không vẽ neon gradient tím/cyan, không dùng glassmorphism quá đà, không dùng biểu đồ vô nghĩa dạng sóng lượn (squiggly lines).
- DỮ LIỆU THỰC TẾ 100% TIẾNG VIỆT: Sử dụng ngữ cảnh thực tế của giáo viên Việt Nam (Cô Nguyễn Thị Mai, Toán nâng cao 8A, VietQR Napas247, điểm danh Có mặt / Có phép / Vắng).
================================================================================

Design System Tokens (Chuẩn đồng bộ với Landing Page & Trang Quản lý Lớp học):
- Color Tokens:
  * Primary Canvas: #FAF8F9 (Warm Off-white/Stone 50).
  * Surface Cards: #FFFFFF (Pure White) with 1px border #E2E8F0 and soft shadow `0 1px 3px rgba(0,0,0,0.05)`.
  * Secondary Surface / Muted: #F8FAFC (Slate 50) and #F1F5F9 (Slate 100).
  * Text Primary: #0F172A (Deep Slate 900), Text Muted: #64748B (Slate 500).
  * Brand Accent: #2563EB (Electric Royal Blue 600) + #1D4ED8 (Hover) + #EFF6FF (Soft Blue 50).
  * Financial / Success Accent: #059669 (Emerald 600), #DCFCE7 (Badge Background), #86EFAC (Border).
  * Warning / Pending Accent: #D97706 (Amber 600) / #854D0E, #FEF9C3 (Badge Background), #FDE047 (Border).
  * Danger / Debt Accent: #DC2626 (Red 600) / #991B1B, #FEE2E2 (Badge Background), #FCA5A5 (Border).
- Typography: Be Vietnam Pro or Inter.
  * BẮT BUỘC: `font-variant-numeric: tabular-nums` cho toàn bộ số tiền học phí, số dư, sĩ số, ngày tháng để không bị giật số.
- Component Radius: Cards 16px/18px, Buttons/Pills 999px (Rounded-full) or 10px/12px, Modals 24px.
- Touch Target: Tối thiểu 48px trên mọi nút bấm ở giao diện Mobile.

--------------------------------------------------------------------------------
DETAILED SCREEN ARCHITECTURE (CẤU TRÚC CHI TIẾT MÀN HÌNH TỔNG QUAN / DASHBOARD)
--------------------------------------------------------------------------------

1. STICKY APP TOPBAR (TÁI SỬ DỤNG 100% NAVBARCOMPONENT MODE="APP"):
   - Dimensions: Height 72px, sticky top, background: rgba(250, 248, 249, 0.94) with backdrop blur (12px), border-bottom: 1px solid #E2E8F0.
   - Left Brand:
     * EduPay Logo: Rounded square badge (32x32px, #2563EB background, white graduation cap SVG icon).
     * Wordmark: "EduPay" (Bold 20px, #0F172A) + Pill badge "SaaS v1.0" (#F1F5F9 background, #64748B text).
   - Center Navigation Links (Flex row, gap 6px, vertically centered):
     * "Tổng quan" -> [ACTIVE STATE: Pill background #EFF6FF, text #2563EB, font-weight 700].
     * "Lớp học" -> Normal state (#475569 text, hover #0F172A).
     * "Học sinh" -> Normal state.
     * "Điểm danh 1 chạm" -> Normal state.
     * "Học phí VietQR" -> Normal state.
   - Right User Profile Chip (42px height, pill container, 1px solid #E2E8F0, white background):
     * Green pulsing dot (8px, #10B981) biểu thị tài khoản đang hoạt động & webhook VietQR online.
     * Text column: "Cô Nguyễn Thị Mai" (Bold 13px #0F172A) / "Gói thuê tháng" (Regular 11px #64748B).
     * Chevron-down icon (#64748B).

2. HERO GREETING & CONTEXT BANNER (KHỐI CHÀO & NHẮC VIỆC TRỌNG TÂM):
   - Kicker Eyebrow: Pill badge with blue dot: "⚡ BẢNG ĐIỀU HÀNH GIẢNG DẠY · THÁNG 10/2026".
   - Greeting Heading (H1, 32px Bold): "Chào Cô Mai 👋, hôm nay có 2 ca dạy cần điểm danh!"
   - Subtitle: "Thứ Năm, ngày 29/10/2026 · Hệ thống đã đối soát tự động 18/24 học sinh qua VietQR Napas247."
   - Quick Primary Action Button (Right aligned on desktop, 48px height, rounded-full, #2563EB fill with white text):
     "📱 Điểm danh ca dạy hôm nay →" (Dẫn thẳng đến màn hình điểm danh ca gần nhất).

3. 4 FINANCIAL & OPERATIONAL KPI CARDS (BỘ 4 CHỈ SỐ QUẢN TRỊ - TABULAR NUMERALS):
   (Bố cục Grid 4 cột đều nhau trên Desktop, 2 cột trên Tablet, 1 cột cuộn mượt trên Mobile).
   
   - Card 1: DỰ THU THÁNG NÀY (Tổng học phí dự kiến)
     * Header: Icon ví tiền (#EFF6FF tròn) + Label "Dự thu tháng này" (14px #64748B).
     * Value: "24.500.000 đ" (26px ExtraBold #2563EB, tabular-nums).
     * Footer Info: Pill "● 24 học sinh · 3 lớp học" (#F1F5F9 text #475569).
   
   - Card 2: ĐÃ THU THỰC TẾ (Tiền đã vào tài khoản ngân hàng)
     * Header: Icon tick xanh bảo chứng (#DCFCE7) + Label "Đã thu thực tế" (14px #64748B).
     * Value: "18.200.000 đ" (26px ExtraBold #059669, tabular-nums).
     * Footer Info: Progress bar mini xanh lá + Badge "74.3% tiến độ (18 giao dịch VietQR)".
   
   - Card 3: CHƯA NỘP / CẦN THU (Số tiền phụ huynh đang nợ)
     * Header: Icon đồng hồ cát vàng (#FEF9C3) + Label "Chưa nộp / Đang chờ" (14px #64748B).
     * Value: "6.300.000 đ" (26px ExtraBold #D97706, tabular-nums).
     * Footer Info: "6 học sinh chưa nộp" + Text link "Nhắc nợ Zalo →" (#2563EB font-weight 600).
   
   - Card 4: SĨ SỐ & TỶ LỆ CHUYÊN CẦN (Chỉ số lớp học)
     * Header: Icon người dùng xanh dương + Label "Tỷ lệ chuyên cần tháng" (14px #64748B).
     * Value: "98.2%" (26px ExtraBold #0F172A, tabular-nums).
     * Footer Info: "142 lượt có mặt · 2 nghỉ có phép · 0 vắng không phép".

4. MAIN WORKSPACE (BỐ CỤC 2 CỘT CHÍNH: 8 CỘT TRÁI / 4 CỘT PHẢI):

   <<< CỘT TRÁI (8 CỘT - KHỐI TÁC VỤ HÀNG NGÀY & GIAO DỊCH VIETQR) >>>

   A. KHỐI "LỊCH DẠY VÀ ĐIỂM DANH NHANH HÔM NAY" (Card White, 18px radius):
      - Header Card: Icon lịch học + Title "Lịch dạy hôm nay (2 ca)" + Pill ngày "Thứ Năm, 29/10/2026".
      - Ca 1 (SẮP DIỄN RA / CẦN ĐIỂM DANH):
        * Card con nổi bật viền xanh dương nhẹ (#EFF6FF nền, 1px solid #BFDBFE):
        * Tên lớp: "Toán nâng cao 8A" (18px Bold #0F172A).
        * Thời gian: "17:30 - 19:00 (90 phút)" | Phòng học: "Tại nhà Cô Mai".
        * Sĩ số: "12 học sinh" | Hình thức: "Thu theo buổi (150.000 đ/buổi)".
        * Status Pill: "Chưa điểm danh" (Màu Amber #854D0E, nền #FEF9C3).
        * Action Button: Blue CTA Button (44px height, rounded-full) "⚡ Bắt đầu điểm danh 1 chạm (30s) →".
      - Ca 2 (CA TỐI):
        * Card con phẳng (#FFFFFF, viền #E2E8F0):
        * Tên lớp: "Luyện thi vào 10 - Nhóm Chuyên" (18px Bold #0F172A).
        * Thời gian: "19:30 - 21:00 (90 phút)".
        * Sĩ số: "12 học sinh" | Hình thức: "Thu theo buổi (180.000 đ/buổi)".
        * Status Pill: "Ca tối · 19:30" (#F1F5F9 text #64748B).
        * Action Button: Outlined Button "Xem danh sách lớp".

   B. KHỐI "NHẬT KÝ ĐỐI SOÁT VIETQR NAPAS247 MỚI NHẤT" (Bảng giao dịch tự động gạch nợ):
      - Header Card: Icon ngân hàng Napas247 + Title "Giao dịch học phí gần đây" + Live status indicator "● Webhook kết nối 24/7".
      - Sub-header: "Tự động phát hiện chuyển khoản đúng cú pháp, gạch nợ tức thì và gửi tin Zalo xác nhận cho phụ huynh."
      - Data Table / Transaction List:
        * Giao dịch 1:
          - Học sinh: "Trần Minh Quân (Bé Tít) - Lớp 8A"
          - Thời gian: "10:15 Hôm nay"
          - Số tiền: "+1.200.000 đ" (Xanh lá #059669 Bold tabular-nums, ngân hàng MB Bank).
          - Cú pháp CK: "HOCPHI QUAN TOAN8A"
          - Trạng thái: Badge "✓ Đã gạch nợ tự động" (#DCFCE7, text #166534).
        * Giao dịch 2:
          - Học sinh: "Lê Hoàng Yến Nhi - Lớp Luyện thi vào 10"
          - Thời gian: "08:42 Hôm nay"
          - Số tiền: "+1.440.000 đ" (Xanh lá #059669 Bold tabular-nums, Vietcombank).
          - Cú pháp CK: "EDUPAY YEN NHI 10"
          - Trạng thái: Badge "✓ Đã gạch nợ tự động" (#DCFCE7).
        * Giao dịch 3:
          - Học sinh: "Nguyễn Hải Đăng - Lớp Toán 6B"
          - Thời gian: "Hôm qua 21:30"
          - Số tiền: "+1.200.000 đ" (Tiền mặt cô thu trực tiếp).
          - Trạng thái: Badge "Đã thu tiền mặt" (#F1F5F9, text #475569).
      - Table Footer: "Xem tất cả 18 giao dịch tháng 10 →" (Text button #2563EB font-weight 600).

   <<< CỘT PHẢI (4 CỘT - PHÍM TẮT TÁC VỤ & CẢNH BÁO NHẮC NỢ) >>>

   A. WIDGET "TÁC VỤ NHANH 1-CHẠM" (Quick Touch Actions):
      - 4 nút bấm lớn, chuẩn xúc giác, dễ chạm trên điện thoại (min-height 52px):
        1. [Icon +] "Tạo lớp học mới" (Outlined button nền trắng, viền #E2E8F0).
        2. [Icon người dùng] "Thêm học sinh vào lớp".
        3. [Icon QR/Receipt] "Lập phiếu thu VietQR tháng" (Highlight nhẹ viền xanh).
        4. [Icon Excel] "Xuất báo cáo sao kê (Excel/PDF)".

   B. WIDGET "CẢNH BÁO HỌC PHÍ CHƯA ĐÓNG & NGHỈ HỌC" (Attention Needed):
      - Alert Box với viền cam nhạt (#FEF3C7 nền, #FDE68A viền):
      - Tiêu đề: "⚠️ Cần chú ý (3 việc)"
      - Danh sách:
        * Mục 1: "Nguyễn Quốc Bảo (Lớp 8A)": Nợ 600.000 đ (Đã nghỉ 2 buổi gần nhất) -> Nút con: "Gửi Zalo mẹ Bảo".
        * Mục 2: "Phạm Thu Hương (Lớp Vào 10)": Chưa đóng học phí T10 (1.440.000 đ) -> Nút con: "Gửi lại mã QR".
        * Mục 3: "Đã đủ 8 buổi tháng 10": 12 học sinh lớp 8A sẵn sàng xuất phiếu thu tháng.

   C. WIDGET "TÀI KHOẢN NHẬN TIỀN VIETQR ĐÃ LIÊN KẾT" (Verified Bank Card Preview):
      - Card mô phỏng thẻ ngân hàng sang trọng, tone Deep Slate (#0F172A) với chữ bạc/trắng:
        * Logo Napas247 + VietinBank (hoặc MBBank).
        * Số tài khoản: "**** **** 6868" (Tabular-nums).
        * Tên chủ thẻ: "NGUYEN THI MAI".
        * Trạng thái liên kết: Green dot + "Đã bật thông báo số dư qua Webhook".
        * Ghi chú: "100% tiền chuyển thẳng vào tài khoản của Cô Mai, EduPay không giữ tiền trung gian."

--------------------------------------------------------------------------------
MOBILE RESPONSIVE BEHAVIOR (MÀN HÌNH DI ĐỘNG 390px):
--------------------------------------------------------------------------------
- Topbar: Giữ nguyên EduPay logo + avatar Cô Mai; Center links tự động chuyển thành hamburger menu hoặc bottom navigation bar nếu cần thiết, nhưng vẫn dùng chung component.
- 4 KPI Cards chuyển thành Carousel trượt ngang hoặc danh sách 2x2 rút gọn, số tiền vẫn hiển thị tabular-nums rõ ràng.
- Khối "Điểm danh hôm nay" nhảy lên vị trí ưu tiên số 1 ngay dưới KPI để giáo viên mở điện thoại là chạm điểm danh được ngay trong 30 giây.
- Tất cả các nút hành động (Điểm danh ngay, Gửi mã QR) đạt chiều cao tối thiểu 48px - 52px, dễ bấm bằng một ngón tay cái.

