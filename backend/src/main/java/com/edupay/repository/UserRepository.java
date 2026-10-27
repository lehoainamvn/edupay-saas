package com.edupay.repository;

import com.edupay.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Tìm user owner theo SĐT (dùng cho Login — phone là unique trong tenants) */
    Optional<User> findByPhoneAndIsOwnerTrue(String phone);

    /** Tìm user theo ID + tenantId (an toàn, chống IDOR) */
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Tìm bất kỳ user theo SĐT trong 1 tenant (kiểm tra trùng số khi đăng ký) */
    boolean existsByTenantIdAndPhone(UUID tenantId, String phone);

    /** Kiểm tra SĐT đã đăng ký làm tenant owner chưa (toàn hệ thống) */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.phone = :phone AND u.isOwner = true")
    boolean existsOwnerByPhone(@Param("phone") String phone);
}
