package com.edupay.dto.response;

import com.edupay.entity.FeeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bảng tính học phí xem trước cho cả lớp trong một kỳ học")
public class BillingPreviewResponse {

    @Schema(description = "ID lớp học")
    private UUID classId;

    @Schema(description = "Tên lớp học")
    private String className;

    @Schema(description = "Hình thức tính phí của lớp (PER_LESSON hoặc MONTHLY_PACKAGE)")
    private FeeType feeType;

    @Schema(description = "Kỳ thu phí (Định dạng YYYY-MM)")
    private String billingMonth;

    @Schema(description = "Tổng số học sinh trong lớp")
    private int totalStudents;

    @Schema(description = "Tổng số lượt buổi học có mặt của cả lớp")
    private int totalAttendedCount;

    @Schema(description = "Tổng tiền học phí dự kiến cả lớp")
    private BigDecimal totalCalculatedAmount;

    @Schema(description = "Giáo viên đã cấu hình thông tin ngân hàng hay chưa")
    private boolean hasBankConfigured;

    @Schema(description = "Tên ngân hàng nhận tiền")
    private String bankName;

    @Schema(description = "Số tài khoản ngân hàng")
    private String accountNumber;

    @Schema(description = "Tên chủ tài khoản")
    private String accountName;

    @Schema(description = "Danh sách chi tiết từng học sinh")
    private List<BillingPreviewItemResponse> items;
}
