package com.edupay.service;

import com.edupay.dto.request.ClassRequest;
import com.edupay.dto.response.ClassResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ Quản lý Lớp học (Multi-Tenant).
 */
public interface ClassService {

    /**
     * Lấy danh sách lớp học của Tenant hiện tại.
     *
     * @param includeArchived Nếu true sẽ lấy cả lớp đã xóa mềm (is_active = false), mặc định chỉ lấy lớp đang mở.
     */
    List<ClassResponse> getClasses(boolean includeArchived);

    /**
     * Lấy thông tin chi tiết 1 lớp học theo ID (lọc theo tenant_id).
     */
    ClassResponse getClassById(UUID id);

    /**
     * Tạo mới lớp học cho Tenant hiện tại.
     */
    ClassResponse createClass(ClassRequest request);

    /**
     * Cập nhật thông tin lớp học theo ID (lọc theo tenant_id).
     */
    ClassResponse updateClass(UUID id, ClassRequest request);

    /**
     * Xóa mềm (Soft Delete) lớp học bằng cách chuyển is_active = false để giữ nguyên lịch sử điểm danh và phiếu thu.
     */
    void softDeleteClass(UUID id);

    /**
     * Khôi phục lại lớp học đã xóa mềm (is_active = true).
     */
    ClassResponse restoreClass(UUID id);
}
