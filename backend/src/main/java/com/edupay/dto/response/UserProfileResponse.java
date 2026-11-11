package com.edupay.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết hồ sơ giáo viên")
public class UserProfileResponse {

    @Schema(description = "ID người dùng")
    private UUID userId;

    @Schema(description = "ID Tenant")
    private UUID tenantId;

    @Schema(description = "Họ và tên giáo viên", example = "Lê Hoài Nam")
    private String fullName;

    @Schema(description = "Số điện thoại", example = "0912345678")
    private String phone;

    @Schema(description = "Email", example = "nam@gmail.com")
    private String email;

    @Schema(description = "URL ảnh đại diện")
    private String avatarUrl;

    @Schema(description = "Chủ tài khoản (Owner)")
    private Boolean isOwner;

    @Schema(description = "Danh sách vai trò", example = "[\"ROLE_TEACHER\", \"OWNER\"]")
    private List<String> roles;

    @Schema(description = "Gói cước đăng ký", example = "TRIAL")
    private String subscriptionPlan;

    @Schema(description = "Trạng thái gói cước", example = "ACTIVE")
    private String subscriptionStatus;
}

