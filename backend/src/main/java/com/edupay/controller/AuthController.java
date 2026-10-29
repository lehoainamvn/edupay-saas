package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.LoginRequest;
import com.edupay.dto.request.RefreshTokenRequest;
import com.edupay.dto.request.RegisterRequest;
import com.edupay.dto.response.AuthResponse;
import com.edupay.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập và quản lý phiên làm việc")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký giáo viên mới",
               description = "Tạo tài khoản giáo viên mới (Tenant owner). SĐT phải chưa đăng ký trong hệ thống.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse data = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Đăng ký thành công! Chào mừng bạn đến với EduPay."));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập",
               description = "Đăng nhập bằng số điện thoại và mật khẩu. Trả về Access Token (1h) và Refresh Token (30d).")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Đăng nhập thành công"));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới Access Token",
               description = "Dùng Refresh Token để lấy Access Token mới. Refresh Token cũ sẽ bị thu hồi (token rotation).")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse data = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Làm mới token thành công"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất",
               description = "Thu hồi Refresh Token hiện tại. Access Token sẽ tự hết hạn sau 1h.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(null, "Đăng xuất thành công"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Đăng xuất tất cả thiết bị",
               description = "Thu hồi toàn bộ Refresh Token của tài khoản. Yêu cầu đăng nhập lại trên mọi thiết bị.")
    public ResponseEntity<ApiResponse<Void>> logoutAll() {
        authService.logoutAll();
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã đăng xuất khỏi tất cả thiết bị"));
    }
}
