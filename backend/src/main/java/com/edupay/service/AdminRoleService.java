package com.edupay.service;

import com.edupay.dto.request.UpdateRolePermissionsRequest;
import com.edupay.dto.response.PermissionGroupResponse;
import com.edupay.dto.response.RoleResponse;

import java.util.List;
import java.util.UUID;

public interface AdminRoleService {

    List<RoleResponse> getAllRoles();

    List<PermissionGroupResponse> getAllPermissionsGrouped();

    RoleResponse updateRolePermissions(UUID roleId, UpdateRolePermissionsRequest request);
}

