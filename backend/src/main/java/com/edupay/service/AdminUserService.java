package com.edupay.service;

import com.edupay.dto.request.CreateUserRequest;
import com.edupay.dto.request.ResetPasswordRequest;
import com.edupay.dto.request.UpdateUserRequest;
import com.edupay.dto.response.AdminUserResponse;
import com.edupay.dto.response.PageResponse;

import java.util.UUID;

public interface AdminUserService {

    PageResponse<AdminUserResponse> getAllUsers(int page, int size, String search, String role, String status, UUID tenantId);

    AdminUserResponse getUserById(UUID id);

    AdminUserResponse createUser(CreateUserRequest request);

    AdminUserResponse updateUser(UUID id, UpdateUserRequest request);

    AdminUserResponse updateUserStatus(UUID id, String status);

    void resetPassword(UUID id, ResetPasswordRequest request);

    void deleteUser(UUID id);
}

