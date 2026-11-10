# Hướng Dẫn Kiểm Thử End-to-End & Kế Hoạch UAT Cùng Giáo Viên Thực Tế

Tài liệu chuẩn hóa quy trình kiểm thử toàn trình (End-to-End Testing) và kịch bản chạy thử nghiệm thực tế (User Acceptance Testing - UAT) cho nền tảng **EduPay SaaS**.

---

## PHẦN 1: KỊCH BẢN KIỂM THỬ END-TO-END (10 BƯỚC TOÀN TRÌNH)

### Luồng nghiệp vụ cốt lõi:
```
[1. Đăng ký & Login] 
       │
       ▼
[2. Cài đặt Ngân hàng VietQR] 
       │
       ▼
[3. Tạo Lớp học & Học sinh] ── (Import Excel / Thêm trực tiếp)
       │
       ▼
[4. Điểm danh 1 chạm] ── (Có mặt / Có phép / Nghỉ không phép)
       │
       ▼
[5. Tính Học phí Tự động] ── (Sinh URL VietQR Napas247 chuẩn mã định danh)
       │
       ▼
[6. Gửi Link Phiếu Thu qua Zalo] ── (Deep-link & Tự động copy lời nhắc lịch sự)
       │
       ▼
[7. Phụ Huynh Mở Web Phiếu Thu] ── (/p/:hash: Quét QR, Tải ảnh, Xác nhận đã CK)
       │
       ▼
[8. Đối Soát & Gạch Nợ] ── (Auto Webhook SePay/Casso & Nút Gạch nợ thủ công)
       │
       ▼
[9. Dashboard & Báo Cáo] ── (KPI thu hồi & Biểu đồ doanh thu 6 tháng)
       │
       ▼
[10. Quản Trị SuperAdmin] ── (Theo dõi MRR, Khóa/Mở, Gia hạn +1 Tháng/+1 Năm)
```

---

### Chi Tiết Từng Bước Kiểm Thử (Test Steps & Expected Results)

| Bước | Hành động kiểm thử | Dữ liệu đầu vào mẫu | Kết quả mong đợi (Expected Result) | Trạng thái |
| :--- | :--- | :--- | :--- | :---: |
| **1** | **Đăng ký tài khoản Giáo viên** | SĐT: `0987654321`, Tên: `Thầy Nguyễn Văn A`, Mật khẩu: `EduPay@2026` | Đăng ký thành công, tự động cấp gói Dùng thử 14 ngày, chuyển hướng vào `/dashboard`. | ✅ ĐẠT |
| **2** | **Cài đặt TK Ngân hàng VietQR** | MBBank (BIN `970422`), STK: `0987654321`, Chủ TK: `NGUYEN VAN A` | Lưu thông tin vào DB. Mã VietQR sau này tự động điền STK & Chủ TK này. | ✅ ĐẠT |
| **3** | **Tạo Lớp học & Thêm Học sinh** | Tên lớp: `Toán 12 VIP`, Đơn giá: `100.000đ/buổi`. Upload file `danh-sach-hoc-sinh.xlsx` | 15 học sinh được import hàng loạt; lọc trùng SĐT, tự động gán mã học sinh (`HS01`, `HS02`...). | ✅ ĐẠT |
| **4** | **Điểm danh 1 chạm** | Chọn lớp `Toán 12 VIP`, ngày dạy hôm nay | Bấm chọn: 13 Có mặt, 1 Có phép, 1 Không phép. Trạng thái lưu tức thì (< 300ms). | ✅ ĐẠT |
| **5** | **Tính học phí & Tạo phiếu thu** | Bấm "Tính học phí tháng 11/2026" cho lớp `Toán 12 VIP` | Hệ thống nhân số buổi thực tế. Sinh mã CK `HP-11-TOAN12VIP-HS01` và URL ảnh VietQR chuẩn Napas247. | ✅ ĐẠT |
| **6** | **Gửi link Zalo cá nhân hóa** | Bấm "Nhắc học phí" tại danh sách phiếu thu | Lời nhắc tự động copy vào Clipboard: *"Dạ em gửi phiếu thu học phí tháng 11 của bé..."*. Bấm "Mở Zalo" mở thẳng chat với phụ huynh. | ✅ ĐẠT |
| **7** | **Phụ huynh mở phiếu thu công khai** | Truy cập link `/p/{hash}` trên điện thoại | Hiển thị chi tiết số buổi học, số tiền; hiển thị mã QR to rõ nét; bấm "Tải QR" tải ảnh về máy; bấm "Tôi đã chuyển khoản" hiện xác nhận thành công. | ✅ ĐẠT |
| **8A** | **Tự động gạch nợ qua Webhook** | Webhook SePay POST nội dung: `MBVCB... HP-11-TOAN12VIP-HS01`, số tiền: `800.000` | Signature hợp lệ; hệ thống khớp đúng phiếu thu, chuyển trạng thái sang `PAID`, ghi log vào `payment_transactions`, chống trùng lặp lặp lại (idempotent). | ✅ ĐẠT |
| **8B** | **Xác nhận đã thu thủ công** | Phụ huynh đóng tiền mặt; GV bấm "Xác nhận đã thu" 1 chạm | Phiếu đổi sang `Đã thu` ngay lập tức, lưu audit log: ai xác nhận, thời điểm xác nhận. | ✅ ĐẠT |
| **9** | **Xem Dashboard & Báo cáo** | Mở `/dashboard` | KPI Dự thu, Đã thu, Chưa nộp cập nhật số liệu mới nhất; biểu đồ 6 tháng hiển thị 2 cột Dự kiến và Thực thu cùng % thu hồi. | ✅ ĐẠT |
| **10** | **SuperAdmin Platform** | Truy cập `/admin/platform` | Xem MRR, danh sách giáo viên thuê, bấm "+1 Tháng" hoặc "+1 Năm" gia hạn thành công; nếu khóa tài khoản, middleware chặn truy cập trả mã `SUBSCRIPTION_SUSPENDED`. | ✅ ĐẠT |

---

## PHẦN 2: CHECKLIST KIỂM THỬ TRÊN THIẾT BỊ DI ĐỘNG (iOS & Android)

Để đảm bảo giáo viên thao tác mượt mà ngay tại lớp học, ứng dụng đã được tối ưu hóa responsive theo các tiêu chí:

### 1. Kích thước màn hình kiểm thử:
- **iPhone SE / iPhone 8**: `375px × 667px` (Màn hình nhỏ, kiểm tra tràn viền và vỡ layout).
- **iPhone 13 / 14 / 15**: `390px × 844px` (Kích thước phổ biến nhất trên iOS).
- **Samsung Galaxy S22 / A54**: `412px × 915px` (Màn hình Android tiêu chuẩn).
- **iPhone 15 Pro Max**: `430px × 932px` (Màn hình lớn).

### 2. Tiêu chí tương tác chạm (Touch & Usability):
- [x] **Touch Target**: Chiều cao các nút bấm chính (Điểm danh, Nhắc Zalo, Gạch nợ) tối thiểu 44px (chuẩn Apple HIG) và 48px (chuẩn Google Material Design).
- [x] **Font chữ**: Cỡ chữ nội dung >= 14px, số tiền in đậm rõ ràng với font `tnum` (tabular numerals) chống lệch dòng.
- [x] **Deep-link Zalo**: Bấm nút "Mở Zalo" trên điện thoại tự động kích hoạt ứng dụng Zalo Mobile (`zalo.me/{phone}`) hoặc mở trình duyệt không bị treo.
- [x] **Tải mã VietQR**: Phụ huynh bấm "Tải ảnh QR" trên iPhone Safari và Android Chrome lưu được file ảnh `.png` về thư viện ảnh để quét bằng App Ngân hàng.
- [x] **Rate Limit API công khai**: Endpoint `/api/public/invoices/{hash}` giới hạn 30 req/phút/IP, ngăn chặn quét tự động hoặc tấn công DoS.

---

## PHẦN 3: KẾ HOẠCH UAT THỰC TẾ CÙNG 2-3 GIÁO VIÊN (5-7 NGÀY)

### 1. Mục tiêu UAT:
- Đưa EduPay vào phục vụ trực tiếp tại 2-3 lớp học thêm thực tế (Toán, Tiếng Anh, Luyện thi).
- Thu thập phản hồi về độ tiện dụng khi thao tác trên điện thoại trong lúc giải lao giữa giờ.
- Kiểm tra tốc độ gửi link Zalo cho phụ huynh và tỷ lệ phụ huynh thanh toán qua VietQR.

### 2. Hồ sơ Giáo viên tham gia UAT:
1. **Cô Mai (Dạy Tiếng Anh THCS - 3 lớp, 45 học sinh)**: Thường xuyên thu học phí theo tháng, hay bị phụ huynh quên nộp.
2. **Thầy Tuấn (Dạy Toán THPT - 2 lớp, 30 học sinh)**: Thu học phí theo số buổi thực tế học sinh có mặt, cần tính năng điểm danh chính xác.
3. **Cô Lan (Dạy Luyện chữ / Tiểu học - 4 lớp, 25 học sinh)**: Ít rành công nghệ, cần giao diện cực kỳ đơn giản, nút bấm to.

### 3. Lịch trình 7 ngày triển khai UAT:

```
Ngày 1: Hướng dẫn cài đặt & Thiết lập tài khoản ngân hàng + Lớp học (15 phút)
Ngày 2-3: Giáo viên thực hiện Điểm danh 1 chạm sau mỗi ca dạy
Ngày 4: Tạo phiếu thu kỳ học phí & Thử nghiệm tính năng "Nhắc nợ Zalo hàng loạt"
Ngày 5-6: Phụ huynh nhận link /p/:hash, quét mã VietQR Napas247 chuyển khoản
Ngày 7: Đối soát số tiền thực thu trên Dashboard, phỏng vấn đánh giá nghiệm thu
```

### 4. Bảng câu hỏi khảo sát giáo viên (Thang điểm 1 - 5):
1. Thao tác điểm danh học sinh trên điện thoại có nhanh và dễ bấm không?
2. Tin nhắn nhắc nợ soạn sẵn gửi qua Zalo có đủ lịch sự và thuận tiện không?
3. Phụ huynh có gặp khó khăn khi mở link phiếu thu hoặc quét mã QR không?
4. Tỷ lệ phụ huynh nộp đúng hạn có tăng lên so với cách ghi chép sổ tay truyền thống không?
5. Thầy/Cô có cảm thấy an tâm khi hệ thống tự động gạch nợ và đối soát số dư không?

### 5. Cam kết hỗ trợ trong giai đoạn UAT:
- Đội ngũ kỹ thuật túc trực phản hồi trong vòng **15 phút**.
- Mọi lỗi cản trở nghiệp vụ (P0 Blocker) được vá và deploy nóng trong vòng **2 giờ**.
