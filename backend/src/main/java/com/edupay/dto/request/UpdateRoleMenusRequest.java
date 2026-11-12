package com.edupay.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class UpdateRoleMenusRequest {

    @NotNull(message = "Mã vai trò không được để trống")
    private UUID roleId;

    @NotNull(message = "Danh sách menuIds không được để trống")
    private List<UUID> menuIds;
}

