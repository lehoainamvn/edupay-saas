package com.edupay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuMatrixResponse {
    private List<RoleResponse> roles;
    private List<MenuDto> menus;
    // Map: roleId -> List of menuIds
    private Map<UUID, List<UUID>> roleMenuMap;
}

