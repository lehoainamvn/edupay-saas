package com.edupay.dto.response;

import com.edupay.entity.FeeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO trả về thông tin chi tiết Lớp học cho Client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin lớp học trả về")
public class ClassResponse {

    @Schema(description = "ID lớp học")
    private UUID id;

    @Schema(description = "Tên lớp học", example = "Toán nâng cao 8A")
    private String name;

    @Schema(description = "Mô tả hoặc ghi chú lịch học", example = "Lớp bồi dưỡng học sinh giỏi Toán lớp 8 (Thứ 3 - Thứ 7)")
    private String description;

    @Schema(description = "Mã hình thức học phí (PER_LESSON / MONTHLY_PACKAGE)", example = "PER_LESSON")
    private FeeType feeType;

    @Schema(description = "Nhãn hiển thị hình thức học phí", example = "Theo buổi")
    private String feeTypeLabel;

    @Schema(description = "Đơn giá học phí mặc định (VNĐ)", example = "150000")
    private BigDecimal defaultFee;

    @Schema(description = "Trạng thái hoạt động (true = đang mở, false = đã lưu trữ/soft-delete)", example = "true")
    private Boolean isActive;

    @Schema(description = "Số lượng học sinh đang theo học trong lớp", example = "4")
    private long studentCount;

    @Schema(description = "Thời gian tạo")
    private Instant createdAt;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private Instant updatedAt;
}
