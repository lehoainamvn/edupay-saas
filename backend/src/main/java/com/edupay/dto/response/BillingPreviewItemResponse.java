package com.edupay.dto.response;

import com.edupay.entity.InvoiceStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu tính học phí xem trước của một học sinh trong lớp")
public class BillingPreviewItemResponse {

    @Schema(description = "ID học sinh")
    private UUID studentId;

    @Schema(description = "Họ và tên học sinh")
    private String fullName;

    @Schema(description = "Biệt danh / tên ở nhà")
    private String nickname;

    @Schema(description = "Số điện thoại phụ huynh")
    private String parentPhone;

    @Schema(description = "Số buổi có mặt thực tế trong tháng")
    private Integer attendedCount;

    @Schema(description = "Đơn giá áp dụng (học phí riêng hoặc đơn giá mặc định của lớp)")
    private BigDecimal unitPrice;

    @Schema(description = "Học phí cơ bản (attendedCount * unitPrice hoặc giá gói tháng)")
    private BigDecimal baseAmount;

    @Schema(description = "Phụ phí (sách vở, tài liệu...)")
    private BigDecimal extraFee;

    @Schema(description = "Giảm trừ (học bổng, miễn giảm...)")
    private BigDecimal discount;

    @Schema(description = "Tổng tiền tạm tính (baseAmount + extraFee - discount)")
    private BigDecimal totalAmount;

    @JsonProperty("isLocked")
    @Schema(description = "Cờ khóa (true nếu kỳ học này đã xuất phiếu thu PENDING/PAID)")
    private Boolean isLocked;

    @Schema(description = "ID phiếu thu hiện tại (nếu đã xuất)")
    private UUID existingInvoiceId;

    @Schema(description = "Trạng thái phiếu thu hiện tại (PENDING, PAID, CANCELLED)")
    private InvoiceStatus existingInvoiceStatus;

    @Schema(description = "Mã chuyển khoản hiện tại (nếu đã xuất)")
    private String existingTransferCode;

    @Schema(description = "Mã public hash hiện tại (nếu đã xuất)")
    private String existingPublicHash;

    @JsonProperty("isLocked")
    public Boolean getIsLocked() {
        return this.isLocked;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }

    public Boolean isLocked() {
        return Boolean.TRUE.equals(this.isLocked);
    }
}
