package com.edupay.repository;

import com.edupay.entity.Student;
import com.edupay.entity.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository quản lý Học sinh.
 * BẮT BUỘC: Mọi câu truy vấn đều phải lọc theo tenantId (Multi-Tenant Isolation Law P0).
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    List<Student> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<Student> findAllByTenantIdAndClassIdOrderByCreatedAtDesc(UUID tenantId, UUID classId);

    Optional<Student> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Kiểm tra trùng học sinh (cùng SĐT phụ huynh và cùng họ tên, hoặc cùng SĐT trong lớp đang học) khi thêm mới.
     */
    boolean existsByTenantIdAndClassIdAndParentPhoneAndStatusNot(
            UUID tenantId, UUID classId, String parentPhone, StudentStatus excludedStatus);

    /**
     * Kiểm tra trùng SĐT phụ huynh trong cùng lớp khi cập nhật thông tin học sinh.
     */
    boolean existsByTenantIdAndClassIdAndParentPhoneAndIdNotAndStatusNot(
            UUID tenantId, UUID classId, String parentPhone, UUID id, StudentStatus excludedStatus);

    /**
     * Kiểm tra trùng họ tên + biệt danh trong cùng lớp.
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM Student s
            WHERE s.tenantId = :tenantId
              AND s.classId = :classId
              AND LOWER(s.fullName) = LOWER(:fullName)
              AND COALESCE(LOWER(s.nickname), '') = LOWER(COALESCE(:nickname, ''))
              AND s.status <> 'DROPPED'
            """)
    boolean existsDuplicateNameInClass(
            @Param("tenantId") UUID tenantId,
            @Param("classId") UUID classId,
            @Param("fullName") String fullName,
            @Param("nickname") String nickname);
}
