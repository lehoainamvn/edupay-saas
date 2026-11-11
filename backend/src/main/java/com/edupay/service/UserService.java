package com.edupay.service;

import com.edupay.dto.request.UpdateProfileRequest;
import com.edupay.dto.response.UserProfileResponse;

public interface UserService {

    /** Lấy thông tin hồ sơ của giáo viên đang đăng nhập */
    UserProfileResponse getProfile();

    /** Cập nhật thông tin hồ sơ cá nhân */
    UserProfileResponse updateProfile(UpdateProfileRequest request);
}

