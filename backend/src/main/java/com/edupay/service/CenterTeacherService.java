package com.edupay.service;

import com.edupay.dto.request.CreateCenterTeacherRequest;
import com.edupay.dto.request.UpdateCenterTeacherRequest;
import com.edupay.dto.response.CenterTeacherResponse;

import java.util.List;
import java.util.UUID;

public interface CenterTeacherService {

    /**
     * Lấy danh sách giáo viên trực thuộc trung tâm hiện tại (kèm số lớp đang phụ trách).
     */
    List<CenterTeacherResponse> getTeachers();

    /**
     * Lấy chi tiết một giáo viên trong trung tâm.
     */
    CenterTeacherResponse getTeacherById(UUID id);

    /**
     * Thêm giáo viên mới vào trung tâm.
     */
    CenterTeacherResponse createTeacher(CreateCenterTeacherRequest request);

    /**
     * Cập nhật thông tin giáo viên trong trung tâm.
     */
    CenterTeacherResponse updateTeacher(UUID id, UpdateCenterTeacherRequest request);

    /**
     * Khóa hoặc mở khóa giáo viên trong trung tâm.
     */
    CenterTeacherResponse updateTeacherStatus(UUID id, String status);

    /**
     * Đặt lại mật khẩu cho giáo viên trong trung tâm.
     */
    void resetPassword(UUID id, String newPassword);

    /**
     * Xóa giáo viên khỏi trung tâm.
     */
    void deleteTeacher(UUID id);
}

