package com.edupay.dto.response;

import com.edupay.entity.FeeType;
import com.edupay.entity.StudentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO trả về thông tin chi tiết Học sinh kèm thông tin Lớp học.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin học sinh trả về")
public class StudentResponse {

    @Schema(description = "ID học sinh")
    private UUID id;

    @Schema(description = "ID lớp học")
    private UUID classId;

    @Schema(description = "Tên lớp học", example = "Toán nâng cao 8A")
    private String className;

    @Schema(description = "Hình thức thu học phí của lớp", example = "PER_LESSON")
    private FeeType classFeeType;

    @Schema(description = "Đơn giá mặc định của lớp (VNĐ)", example = "150000")
    private BigDecimal classDefaultFee;

    @Schema(description = "Họ và tên học sinh", example = "Nguyễn Minh Anh")
    private String fullName;

    @Schema(description = "Biệt danh ở nhà", example = "Bé Bống")
    private String nickname;

    @Schema(description = "Họ tên phụ huynh", example = "Nguyễn Văn Tuấn")
    private String parentName;

    @Schema(description = "Số điện thoại Zalo phụ huynh", example = "0912111222")
    private String parentPhone;

    @Schema(description = "Học phí riêng (VNĐ) nếu có", example = "150000")
    private BigDecimal customFee;

    @Schema(description = "Đơn giá thực tế áp dụng (ưu tiên customFee, nếu null lấy classDefaultFee)", example = "150000")
    private BigDecimal effectiveFee;

    @Schema(description = "Trạng thái học tập (STUDYING, PAUSED, DROPPED)", example = "STUDYING")
    private StudentStatus status;

    @Schema(description = "Nhãn trạng thái tiếng Việt", example = "Đang học")
    private String statusLabel;

    @Schema(description = "Thời gian tạo")
    private Instant createdAt;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private Instant updatedAt;
}
