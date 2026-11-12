package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.CreateMenuRequest;
import com.edupay.dto.request.UpdateMenuRequest;
import com.edupay.dto.request.UpdateRoleMenusRequest;
import com.edupay.dto.response.MenuDto;
import com.edupay.dto.response.RoleMenuMatrixResponse;
import com.edupay.service.AdminMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/admin/menus", "/api/admin/menus"})
@RequiredArgsConstructor
@Tag(name = "SuperAdmin Quản lý Menu Động & Phân quyền Menu", description = "CRUD các mục Menu toàn hệ thống và ma trận phân quyền Menu theo vai trò")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminMenuController {

    private final AdminMenuService adminMenuService;

    @GetMapping
    @Operation(summary = "Lấy toàn bộ danh sách Menu hệ thống")
    public ResponseEntity<ApiResponse<List<MenuDto>>> getAllMenus() {
        List<MenuDto> data = adminMenuService.getAllMenus();
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách Menu thành công"));
    }

    @PostMapping
    @Operation(summary = "Thêm một Menu/Tính năng mới vào hệ thống")
    public ResponseEntity<ApiResponse<MenuDto>> createMenu(@Valid @RequestBody CreateMenuRequest request) {
        MenuDto data = adminMenuService.createMenu(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Tạo Menu mới thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Chỉnh sửa thông tin Menu")
    public ResponseEntity<ApiResponse<MenuDto>> updateMenu(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMenuRequest request) {
        MenuDto data = adminMenuService.updateMenu(id, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật Menu thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa một Menu khỏi hệ thống")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(@PathVariable UUID id) {
        adminMenuService.deleteMenu(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa Menu thành công"));
    }

    @GetMapping("/role-matrix")
    @Operation(summary = "Lấy ma trận phân quyền Menu theo vai trò", description = "Trả về danh sách vai trò, danh mục menu và checklist các menu được cấp phép")
    public ResponseEntity<ApiResponse<RoleMenuMatrixResponse>> getRoleMenuMatrix() {
        RoleMenuMatrixResponse data = adminMenuService.getRoleMenuMatrix();
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy ma trận phân quyền Menu thành công"));
    }

    @PutMapping("/role-matrix")
    @Operation(summary = "Cập nhật quyền truy cập Menu cho một Vai trò")
    public ResponseEntity<ApiResponse<Void>> updateRoleMenus(@Valid @RequestBody UpdateRoleMenusRequest request) {
        adminMenuService.updateRoleMenus(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Cập nhật phân quyền Menu cho vai trò thành công"));
    }
}

