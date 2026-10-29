package com.edupay.repository;

import com.edupay.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository quản lý Lớp học.
 * BẮT BUỘC: Mọi câu truy vấn đều phải lọc theo tenantId để đảm bảo cô lập dữ liệu Multi-Tenant (P0).
 */
@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, UUID> {

    /**
     * Lấy danh sách các lớp đang hoạt động (chưa bị soft-delete) của Tenant.
     */
    List<ClassEntity> findAllByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId);

    /**
     * Lấy toàn bộ danh sách lớp (bao gồm cả lớp đã lưu trữ/soft-delete) của Tenant.
     */
    List<ClassEntity> findAllByTenantIdOrderByIsActiveDescCreatedAtDesc(UUID tenantId);

    /**
     * Tìm kiếm lớp học theo ID và TenantId.
     */
    Optional<ClassEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Kiểm tra trùng tên lớp học đang hoạt động trong cùng 1 Tenant khi tạo mới.
     */
    boolean existsByTenantIdAndNameIgnoreCaseAndIsActiveTrue(UUID tenantId, String name);

    /**
     * Kiểm tra trùng tên lớp học đang hoạt động trong cùng 1 Tenant khi cập nhật.
     */
    boolean existsByTenantIdAndNameIgnoreCaseAndIdNotAndIsActiveTrue(UUID tenantId, String name, UUID id);

    /**
     * Đếm số lượng lớp đang hoạt động của Tenant (dùng để kiểm tra giới hạn gói dịch vụ TRIAL/MONTHLY/ANNUAL).
     */
    long countByTenantIdAndIsActiveTrue(UUID tenantId);

    /**
     * Đếm số học sinh đang theo học (STUDYING) của 1 lớp thuộc Tenant.
     */
    @Query(value = "SELECT COUNT(*) FROM students s WHERE s.class_id = :classId AND s.tenant_id = :tenantId AND s.status = 'STUDYING'", nativeQuery = true)
    long countActiveStudentsByClassIdAndTenantId(@Param("classId") UUID classId, @Param("tenantId") UUID tenantId);
}
