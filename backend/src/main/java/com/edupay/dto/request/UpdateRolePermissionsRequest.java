package com.edupay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRolePermissionsRequest {

    @NotNull(message = "Danh sách quyền hạn không được null")
    private List<String> permissionCodes;
}
