package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.UpdateProfileRequest;
import com.edupay.dto.response.UserProfileResponse;
import com.edupay.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Hồ sơ Người dùng (User Profile)", description = "API quản lý và cập nhật thông tin cá nhân của giáo viên")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Lấy thông tin hồ sơ của giáo viên", description = "Truy vấn họ và tên, số điện thoại, email và gói cước tài khoản")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        UserProfileResponse data = userService.getProfile();
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin hồ sơ cá nhân thành công"));
    }

    @PutMapping("/profile")
    @Operation(summary = "Cập nhật thông tin hồ sơ giáo viên", description = "Cho phép thay đổi họ và tên, số điện thoại, email của giáo viên")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse data = userService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật hồ sơ cá nhân thành công"));
    }
}

