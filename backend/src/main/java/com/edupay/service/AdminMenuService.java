package com.edupay.service;

import com.edupay.dto.request.CreateMenuRequest;
import com.edupay.dto.request.UpdateMenuRequest;
import com.edupay.dto.request.UpdateRoleMenusRequest;
import com.edupay.dto.response.MenuDto;
import com.edupay.dto.response.RoleMenuMatrixResponse;

import java.util.List;
import java.util.UUID;

public interface AdminMenuService {

    List<MenuDto> getAllMenus();

    MenuDto createMenu(CreateMenuRequest request);

    MenuDto updateMenu(UUID menuId, UpdateMenuRequest request);

    void deleteMenu(UUID menuId);

    RoleMenuMatrixResponse getRoleMenuMatrix();

    void updateRoleMenus(UpdateRoleMenusRequest request);
}

