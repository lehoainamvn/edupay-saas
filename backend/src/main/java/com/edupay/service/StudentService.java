package com.edupay.service;

import com.edupay.dto.request.StudentRequest;
import com.edupay.dto.response.StudentImportResultResponse;
import com.edupay.dto.response.StudentResponse;
import com.edupay.entity.StudentStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ Quản lý Học sinh & Import hàng loạt Excel/CSV (Multi-Tenant).
 */
public interface StudentService {

    /**
     * Lấy danh sách học sinh của Tenant hiện tại, hỗ trợ lọc theo lớp, trạng thái và từ khóa tìm kiếm.
     */
    List<StudentResponse> getStudents(UUID classId, StudentStatus status, String search);

    /**
     * Lấy chi tiết 1 học sinh theo ID (lọc theo tenant_id).
     */
    StudentResponse getStudentById(UUID id);

    /**
     * Thêm mới học sinh vào lớp (validate SĐT, kiểm tra trùng SĐT và trùng tên trong lớp).
     */
    StudentResponse createStudent(StudentRequest request);

    /**
     * Cập nhật thông tin học sinh theo ID.
     */
    StudentResponse updateStudent(UUID id, StudentRequest request);

    /**
     * Xóa mềm (Soft Delete) học sinh bằng cách chuyển status = DROPPED để giữ lịch sử điểm danh và hóa đơn.
     */
    void softDeleteStudent(UUID id);

    /**
     * Cập nhật nhanh trạng thái học tập (STUDYING, PAUSED, DROPPED).
     */
    StudentResponse updateStudentStatus(UUID id, StudentStatus status);

    /**
     * Import hàng loạt danh sách học sinh vào 1 lớp từ file Excel (.xlsx, .xls) hoặc CSV (.csv) bằng Apache POI.
     */
    StudentImportResultResponse importStudentsFromFile(UUID classId, MultipartFile file);

    /**
     * Sinh file Excel mẫu (.xlsx) bằng Apache POI để giáo viên tải về điền danh sách học sinh.
     */
    byte[] generateImportTemplateExcel();
}
