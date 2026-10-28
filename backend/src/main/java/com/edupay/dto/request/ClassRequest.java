package com.edupay.dto.request;

import com.edupay.entity.FeeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO dùng để Tạo mới hoặc Cập nhật Lớp học.
 * TUYỆT ĐỐI KHÔNG nhận tenant_id từ Client payload (Multi-Tenant Isolation Law P0).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu yêu cầu tạo mới hoặc chỉnh sửa lớp học")
public class ClassRequest {

    @NotBlank(message = "Tên lớp học không được để trống")
    @Size(min = 2, max = 255, message = "Tên lớp học phải từ 2 đến 255 ký tự")
    @Schema(description = "Tên lớp học", example = "Toán nâng cao 8A")
    private String name;

    @Size(max = 1000, message = "Mô tả lịch học không được vượt quá 1000 ký tự")
    @Schema(description = "Mô tả hoặc lịch học trong tuần", example = "Thứ 3 & Thứ 7 (18:00 - 19:30)")
    private String description;

    @NotNull(message = "Hình thức học phí không được để trống")
    @Schema(description = "Hình thức thu học phí: PER_LESSON (Theo buổi) hoặc MONTHLY_PACKAGE (Trọn tháng)", example = "PER_LESSON")
    private FeeType feeType;

    @NotNull(message = "Đơn giá mặc định không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Đơn giá học phí mặc định không được nhỏ hơn 0")
    @Schema(description = "Đơn giá học phí mặc định (VNĐ)", example = "150000")
    private BigDecimal defaultFee;
}
