package com.edupay.dto.response;

import com.edupay.entity.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin phiếu thu công khai cho Phụ huynh tra cứu không cần đăng nhập (/p/:hash)")
public class PublicInvoiceResponse {

    @Schema(description = "Mã băm tra cứu công khai")
    private String publicHash;

    @Schema(description = "Tên lớp học")
    private String className;

    @Schema(description = "Họ và tên học sinh")
    private String studentName;

    @Schema(description = "Tên ở nhà / Biệt danh")
    private String nickname;

    @Schema(description = "Kỳ thu học phí (YYYY-MM)")
    private String billingMonth;

    @Schema(description = "Số buổi học thực tế của con")
    private Integer attendedCount;

    @Schema(description = "Đơn giá học phí")
    private BigDecimal unitPrice;

    @Schema(description = "Phụ phí (sách vở, tài liệu)")
    private BigDecimal extraFee;

    @Schema(description = "Khoản giảm trừ")
    private BigDecimal discount;

    @Schema(description = "Tổng tiền học phí cần đóng")
    private BigDecimal totalAmount;

    @Schema(description = "Mã chuyển khoản chính xác Napas247")
    private String transferCode;

    @Schema(description = "Trạng thái thanh toán (PENDING: Chờ thanh toán, PAID: Đã thanh toán)")
    private InvoiceStatus status;

    @Schema(description = "Thời điểm thanh toán nếu đã nộp")
    private Instant paidAt;

    @Schema(description = "Ghi chú phiếu thu của giáo viên")
    private String note;

    @Schema(description = "Mã định danh ngân hàng (BIN)")
    private String bankBin;

    @Schema(description = "Tên ngân hàng")
    private String bankName;

    @Schema(description = "Số tài khoản nhận tiền của giáo viên")
    private String accountNumber;

    @Schema(description = "Tên chủ tài khoản ngân hàng")
    private String accountName;

    @Schema(description = "Đường dẫn ảnh mã QR động VietQR Napas247")
    private String vietQrUrl;

    @Schema(description = "Danh sách ngày các buổi học con đã tham gia trong tháng")
    private List<LocalDate> attendedDates;
}
