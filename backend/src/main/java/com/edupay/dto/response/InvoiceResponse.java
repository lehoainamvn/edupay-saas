package com.edupay.dto.response;

import com.edupay.entity.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết phiếu thu học phí")
public class InvoiceResponse {

    @Schema(description = "ID phiếu thu")
    private UUID id;

    @Schema(description = "ID lớp học")
    private UUID classId;

    @Schema(description = "Tên lớp học")
    private String className;

    @Schema(description = "ID học sinh")
    private UUID studentId;

    @Schema(description = "Họ và tên học sinh")
    private String studentName;

    @Schema(description = "Biệt danh ở nhà")
    private String nickname;

    @Schema(description = "Số điện thoại phụ huynh")
    private String parentPhone;

    @Schema(description = "Kỳ thu phí (YYYY-MM)")
    private String billingMonth;

    @Schema(description = "Số buổi có mặt")
    private Integer attendedCount;

    @Schema(description = "Đơn giá áp dụng")
    private BigDecimal unitPrice;

    @Schema(description = "Phụ phí")
    private BigDecimal extraFee;

    @Schema(description = "Giảm trừ")
    private BigDecimal discount;

    @Schema(description = "Tổng số tiền cần thanh toán")
    private BigDecimal totalAmount;

    @Schema(description = "Mã chuyển khoản đối soát Napas247")
    private String transferCode;

    @Schema(description = "Mã băm bảo mật tra cứu public")
    private String publicHash;

    @Schema(description = "Đường dẫn tra cứu công khai cho phụ huynh (/p/:hash)")
    private String publicUrl;

    @Schema(description = "Đường dẫn ảnh mã VietQR Napas247 động")
    private String vietQrUrl;

    @Schema(description = "Trạng thái phiếu thu (PENDING, PAID, CANCELLED)")
    private InvoiceStatus status;

    @Schema(description = "Thời điểm thanh toán")
    private Instant paidAt;

    @Schema(description = "Phương thức thanh toán (VIETQR, CASH, MANUAL)")
    private String paidMethod;

    @Schema(description = "Ghi chú phiếu thu")
    private String note;

    @Schema(description = "Thời điểm tạo phiếu thu")
    private Instant createdAt;
}

