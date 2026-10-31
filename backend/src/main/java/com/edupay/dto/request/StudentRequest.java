package com.edupay.dto.request;

import com.edupay.entity.StudentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO dùng để Thêm mới hoặc Cập nhật Học sinh.
 * TUYỆT ĐỐI KHÔNG nhận tenant_id từ Client payload (Multi-Tenant Isolation Law P0).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu yêu cầu thêm mới hoặc chỉnh sửa học sinh")
public class StudentRequest {

    @NotNull(message = "Lớp học không được để trống")
    @Schema(description = "ID lớp học của học sinh", example = "d0000000-0000-0000-0000-000000000001")
    private UUID classId;

    @NotBlank(message = "Họ và tên học sinh không được để trống")
    @Size(min = 2, max = 255, message = "Họ và tên học sinh phải từ 2 đến 255 ký tự")
    @Schema(description = "Họ và tên đầy đủ của học sinh", example = "Nguyễn Minh Anh")
    private String fullName;

    @Size(max = 100, message = "Biệt danh ở nhà không được vượt quá 100 ký tự")
    @Schema(description = "Biệt danh (tên ở nhà) hỗ trợ đối soát chuyển khoản", example = "Bé Bống")
    private String nickname;

    @Size(max = 255, message = "Họ tên phụ huynh không được vượt quá 255 ký tự")
    @Schema(description = "Họ và tên phụ huynh", example = "Nguyễn Văn Tuấn")
    private String parentName;

    @NotBlank(message = "Số điện thoại phụ huynh không được để trống")
    @Pattern(
            regexp = "^(0|\\+84)(3|5|7|8|9)[0-9]{8}$",
            message = "Số điện thoại phụ huynh không đúng định dạng di động Việt Nam (10 số, VD: 0912345678)"
    )
    @Schema(description = "Số điện thoại Zalo của phụ huynh (10 số)", example = "0912111222")
    private String parentPhone;

    @DecimalMin(value = "0.0", inclusive = true, message = "Học phí riêng không được nhỏ hơn 0")
    @Schema(description = "Mức học phí riêng (VNĐ) nếu khác đơn giá mặc định của lớp; để null nếu dùng mức mặc định của lớp", example = "150000")
    private BigDecimal customFee;

    @Schema(description = "Trạng thái theo học (STUDYING, PAUSED, DROPPED)", example = "STUDYING")
    private StudentStatus status;
}
