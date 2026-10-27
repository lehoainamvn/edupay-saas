package com.edupay.service;

import com.edupay.dto.request.LoginRequest;
import com.edupay.dto.request.RefreshTokenRequest;
import com.edupay.dto.request.RegisterRequest;
import com.edupay.dto.response.AuthResponse;

public interface AuthService {

    /** Đăng ký giáo viên mới — tạo Tenant + User owner + gán role OWNER */
    AuthResponse register(RegisterRequest request);

    /** Đăng nhập bằng SĐT + Password — trả về Access + Refresh Token */
    AuthResponse login(LoginRequest request);

    /** Đổi Refresh Token lấy Access Token mới */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /** Đăng xuất — revoke refresh token hiện tại */
    void logout(String refreshToken);

    /** Đăng xuất tất cả thiết bị — revoke toàn bộ refresh tokens của user */
    void logoutAll();
}
