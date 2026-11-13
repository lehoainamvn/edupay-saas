package com.edupay.repository;

import com.edupay.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    /** Lấy toàn bộ users trong 1 tenant sắp xếp theo ngày tạo mới nhất */
    List<User> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /** Lấy giáo viên/nhân viên trực thuộc (không phải owner) trong 1 tenant */
    List<User> findAllByTenantIdAndIsOwnerFalseOrderByCreatedAtDesc(UUID tenantId);

    /** Đếm giáo viên/nhân viên trực thuộc trong 1 tenant */
    long countByTenantIdAndIsOwnerFalse(UUID tenantId);

    /** Đếm tổng số user trong 1 tenant */
    long countByTenantId(UUID tenantId);

    /** Đếm tổng số giáo viên trực thuộc trên toàn hệ thống */
    long countByIsOwnerFalse();

    /** Lấy tài khoản owner đại diện cho tenant */
    Optional<User> findFirstByTenantIdAndIsOwnerTrue(UUID tenantId);

    /** Tìm user owner theo SĐT (dùng cho Login — phone là unique trong tenants) */
    Optional<User> findByPhoneAndIsOwnerTrue(String phone);

    /** Tìm user theo SĐT (hỗ trợ cả Owner lẫn Teacher/Staff) */
    Optional<User> findFirstByPhoneOrderByCreatedAtDesc(String phone);

    /** Tìm user theo ID + tenantId (an toàn, chống IDOR) */
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Tìm user theo SĐT trong 1 tenant */
    Optional<User> findByTenantIdAndPhone(UUID tenantId, String phone);

    /** Tìm bất kỳ user theo SĐT trong 1 tenant (kiểm tra trùng số khi đăng ký) */
    boolean existsByTenantIdAndPhone(UUID tenantId, String phone);

    /** Kiểm tra SĐT đã đăng ký làm tenant owner chưa (toàn hệ thống) */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.phone = :phone AND u.isOwner = true")
    boolean existsOwnerByPhone(@Param("phone") String phone);
}
