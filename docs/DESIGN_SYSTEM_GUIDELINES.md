# 📐 BỘ QUY CHUẨN THIẾT KẾ ĐỒNG BỘ (DESIGN SYSTEM & UI GUIDELINES) — EDUPAY SAAS

> **Dành cho:** Toàn bộ Developer (Frontend), Designer (Figma) và AI Coding Agent.  
> **Mục tiêu:** Đảm bảo toàn bộ các màn hình (Landing Page, Dashboard, Điểm danh, Lập phiếu thu, Cổng tra cứu VietQR) đạt tính **đồng bộ tuyệt đối** về Kiểu chữ (Typography), Màu sắc (Color Tokens), Khung/Bo góc (Radius/Shadow) và Biểu tượng (Iconography).

---

## 1. 🎯 TRIẾT LÝ THIẾT KẾ (DESIGN PHILOSOPHY)

* **Phong cách cốt lõi:** Tối giản (Minimalist), Thực dụng (Utilitarian), Đáng tin cậy (Fintech-grade trust), lấy cảm hứng từ *Linear, Stripe và Notion*.
* **Đối tượng phục vụ:** Giáo viên dạy kèm, gia sư độc lập và phụ huynh học sinh tại Việt Nam. Giao diện phải cực kỳ rõ ràng, dễ nhìn dưới ánh sáng phòng học, không gây rối mắt.
* **❌ CÁC YẾU TỐ BỊ NGHIÊM CẤM (Anti-AI Clichés):**
  * Không dùng gradient neon tím/hồng chói lóa.
  * Không lạm dụng hiệu ứng kính mờ (Glassmorphism) quá đà làm giảm độ tương phản.
  * Không dùng các hộp bento box hình vuông đối xứng với biểu đồ sóng vô nghĩa.
  * Không dùng chữ giả *Lorem Ipsum* — Mọi thiết kế phải dùng câu chữ tiếng Việt thực tế.

---

## 2. 🔤 QUY CHUẨN KIỂU CHỮ (TYPOGRAPHY)

* **Phông chữ chuẩn:** **`Inter`** hoặc **`Be Vietnam Pro`** (Cả 2 đều có sẵn trên Google Fonts, hiển thị dấu tiếng Việt chuẩn xác không bị vỡ glyph).
* **Quy chuẩn hiển thị số tiền:** Bắt buộc áp dụng thuộc tính `font-variant-numeric: tabular-nums` cho số tiền, học phí, số buổi để các con số thẳng hàng khi hiển thị theo bảng hoặc danh sách.

| Cấp bậc | Cỡ chữ (Font Size) | Trọng số (Weight) | Khoảng cách dòng (Line Height) | Letter Spacing | Ứng dụng thực tế |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Display / Hero** | `44px` – `64px` | ExtraBold (800) | `1.05` | `-0.05em` | Tiêu đề chính Hero Banner |
| **Heading 1** | `34px` – `46px` | Bold (700) | `1.15` | `-0.04em` | Tiêu đề các Section lớn |
| **Heading 2** | `22px` – `31px` | SemiBold (600) | `1.25` | `-0.035em` | Tiêu đề tính năng, tên lớp học |
| **Heading 3** | `17px` – `19px` | SemiBold (600) | `1.3` | `-0.03em` | Tên học sinh, tên gói cước |
| **Body (Thường)** | `15px` – `16px` | Regular (400) | `1.65` | `normal` | Đoạn văn bản mô tả, giải thích |
| **Meta / Phụ** | `13px` – `14px` | Medium (500) | `1.5` | `normal` | Biệt danh ở nhà, SĐT, ngày học |
| **Micro / Tag** | `10px` – `12px` | Bold (700) | `1.2` | `0.1em` | Huy hiệu (Badge), tag "Có mặt", mã phiếu |

---

## 3. 🎨 BẢNG MÀU CHUẨN (SEMANTIC COLOR TOKENS)

Mọi mã màu phải tuân thủ chuẩn CSS Variables và Tailwind Tokens dưới đây:

### 3.1. Màu nền & Bề mặt (Surfaces)
```css
--canvas: #FAF8F9;   /* Nền chính toàn trang (Warm Stone 50 dịu mắt) */
--white:  #FFFFFF;   /* Nền thẻ Card, Modal, Biên lai trắng sáng */
--soft:   #F1F5F9;   /* Nền thứ cấp cho bảng, khung phụ (Slate 100) */
--border: #E2E8F0;   /* Màu viền phân cách 1px (Slate 200) */
```

### 3.2. Màu chữ & Văn bản (Ink)
```css
--ink:   #0F172A;    /* Chữ chính (Deep Slate 900) - Tương phản cao nhất */
--muted: #64748B;    /* Chữ phụ, mô tả, nhãn phụ (Slate 500) */
--subtle:#94A3B8;    /* Placeholder, icon phụ, thời gian (Slate 400) */
```

### 3.3. Màu thương hiệu (Brand Accent)
```css
--blue:      #2563EB; /* Xanh hoàng gia (Tailwind Blue 600) - Nút chính, liên kết */
--blue-dark: #1D4ED8; /* Màu hover của nút chính (Blue 700) */
--blue-soft: #EFF6FF; /* Nền nhạt cho tag, badge đang chọn (Blue 50) */
```

### 3.4. Màu trạng thái nghiệp vụ (Bắt buộc dùng cho Điểm danh & Học phí)
| Nghiệp vụ | Mã màu nền | Mã màu chữ | Mã viền (Border) | Ứng dụng |
| :--- | :--- | :--- | :--- | :--- |
| 🟢 **Có mặt / Đã thanh toán** | `#DCFCE7` (Green 100) | `#166534` (Green 800) | `#86EFAC` | Điểm danh có mặt, phiếu thu đã trả, tick thành công |
| 🟡 **Nghỉ phép / Chờ nộp tiền** | `#FEF9C3` (Amber 100) | `#854D0E` (Amber 800) | `#FDE047` | Điểm danh có phép, phiếu thu PENDING |
| 🔴 **Vắng / Quá hạn / Cách cũ** | `#FEE2E2` (Rose 100) | `#991B1B` (Rose 800) | `#FCA5A5` | Điểm danh vắng, hủy phiếu thu, dấu ✕ |

---

## 4. 📦 KHUNG VIỀN, BO GÓC & BÓNG ĐỔ (RADIUS & ELEVATION)

### Bo góc (Border Radius):
* **Pill / Nút tròn (`999px`):** Dùng cho nút hành động chính (CTA Button), thẻ trạng thái (Status Pills), badge "SaaS v1.0", badge "Đã xác thực".
* **Nút bấm vuông nhỏ (`8px`):** Dùng cho các nút đổi trạng thái Có mặt/Có phép/Vắng, ô nhập liệu form (`input`).
* **Thẻ nghiệp vụ (`14px` – `18px`):** Dùng cho Thẻ học sinh, Thẻ gói giá, Thẻ so sánh cách cũ/cách mới.
* **Khung bao quát / Preview (`24px` – `30px`):** Dùng cho Banner CTA cuối trang, Khung preview điện thoại.

### Bóng đổ (Elevation):
* **Subtle Elevation (Thẻ thông thường):**  
  `box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);`
* **Card nổi bật (Gói phổ biến / Phiếu thu):**  
  `box-shadow: 0 15px 35px rgba(37, 99, 235, 0.08);`
* **Khung Modal / Màn hình Preview:**  
  `box-shadow: 0 22px 60px rgba(15, 23, 42, 0.13);`

---

## 5. 🏷️ HỆ THỐNG BIỂU TƯỢNG ĐỒNG BỘ (ICONOGRAPHY SYSTEM)

Toàn bộ icon trong EduPay bắt buộc phải tuân theo phong cách **Outline Stroke (Nét vẽ thanh mảnh, đồng bộ với Lucide / Heroicons)**:

* **Quy chuẩn SVG Stroke:**
  * `fill: none;`
  * `stroke: currentColor;`
  * `stroke-width: 2px;`
  * `stroke-linecap: round;`
  * `stroke-linejoin: round;`

* **Kích thước Icon tiêu chuẩn:**
  * Icon trong nút bấm: `16px × 16px`
  * Icon avatar / trạng thái tròn: `22px × 22px` đến `32px × 32px`
  * Logo Mark: `31px × 31px` (Icon Mũ cử nhân bên trong `22px`)

* **Bộ Icon quy chuẩn cho từng tính năng:**
  1. **Logo EduPay (Mũ cử nhân):**  
     `<path d="m3 10 11-6 11 6-11 6L3 10Z"/><path d="M7 13.5V19c3.8 3.2 10.2 3.2 14 0v-5.5"/>`
  2. **Thành công / Xác nhận:** Dấu kiểm (`✓`) đặt trong vòng tròn màu xanh (`#DCFCE7`).
  3. **Đóng / Cách cũ / Xóa:** Dấu gạch chéo (`✕` hoặc `×`) đặt trong vòng tròn xám (`#E2E8F0`).
  4. **Tiếp tục / Điều hướng:** Mũi tên chỉ sang phải (`→`).
  5. **Video / Demo:** Nút tam giác Play (`▶`).
  6. **VietQR / Thanh toán:** Biểu tượng ma trận mã QR 2 chiều Napas247.

---

## 6. 📱 TIÊU CHUẨN THAO TÁC CẢM ỨNG (MOBILE-FIRST TOUCH UX)

* **Vùng bấm cảm ứng (Touch Target):** Mọi nút bấm, ô nhập hoặc thẻ tương tác trên màn hình điện thoại **bắt buộc phải có chiều cao tối thiểu $\ge$ 44px $\times$ 44px**, kích thước khuyến nghị là **48px**.
* **Khoảng cách giữa các nút:** Tối thiểu **8px** để tránh bấm nhầm giữa [Có mặt] và [Vắng].
* **Phản hồi tức thì (Optimistic UI):** Khi giáo viên chạm vào trạng thái điểm danh, giao diện phải đổi màu ngay lập tức `< 50ms` mà không chờ đợi phản hồi từ Backend.

---

## 7. 📋 MẪU THÀNH PHẦN (COMPONENT PATTERN CODES)

### 7.1. Nút bấm chính (Primary Button)
```html
<button class="button">
  Tạo tài khoản dạy học ngay <span aria-hidden="true">→</span>
</button>
```
```css
.button {
  border: 1px solid #2563EB;
  background: #2563EB;
  color: #FFFFFF;
  min-height: 52px;
  padding: 0 22px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,.08);
  transition: all .2s ease;
}
.button:hover {
  background: #1D4ED8;
  transform: translateY(-1px);
}
```

### 7.2. Thẻ Học Sinh & Điểm Danh 3 Trạng Thái
```html
<div class="student-row">
  <span class="avatar">MA</span>
  <div class="student-info">
    <strong>Nguyễn Minh Anh</strong>
    <small>Bé Bống · PH: 0912.345.678</small>
  </div>
  <div class="attendance-toggles">
    <button class="pill present">Có mặt</button>
    <button class="pill excused">Có phép</button>
    <button class="pill absent">Vắng</button>
  </div>
</div>
```
