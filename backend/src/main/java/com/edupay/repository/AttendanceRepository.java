package com.edupay.repository;

import com.edupay.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository quản lý Điểm danh.
 * BẮT BUỘC: Mọi truy vấn đều phải lọc theo tenantId (Multi-Tenant Isolation P0).
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    /**
     * Lấy danh sách điểm danh của lớp trong 1 ngày cụ thể.
     */
    List<Attendance> findAllByTenantIdAndClassIdAndLessonDate(UUID tenantId, UUID classId, LocalDate lessonDate);

    /**
     * Lấy toàn bộ lượt điểm danh của lớp trong khoảng thời gian (dùng cho ma trận lịch sử tháng).
     */
    List<Attendance> findAllByTenantIdAndClassIdAndLessonDateBetweenOrderByLessonDateAsc(
            UUID tenantId, UUID classId, LocalDate startDate, LocalDate endDate);

    /**
     * Tìm kiếm bản ghi điểm danh theo ID và TenantId.
     */
    Optional<Attendance> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Tìm kiếm bản ghi điểm danh theo học sinh và ngày học.
     */
    Optional<Attendance> findByTenantIdAndStudentIdAndLessonDate(UUID tenantId, UUID studentId, LocalDate lessonDate);

    /**
     * Kiểm tra xem ngày đó lớp đã điểm danh chưa.
     */
    boolean existsByTenantIdAndClassIdAndLessonDate(UUID tenantId, UUID classId, LocalDate lessonDate);

    /**
     * Xóa toàn bộ buổi điểm danh của lớp trong 1 ngày (dùng khi hủy/xóa buổi học).
     */
    void deleteAllByTenantIdAndClassIdAndLessonDate(UUID tenantId, UUID classId, LocalDate lessonDate);
}

