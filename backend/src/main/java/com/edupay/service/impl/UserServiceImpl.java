package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.config.TenantContextFilter;
import com.edupay.dto.request.UpdateProfileRequest;
import com.edupay.dto.response.UserProfileResponse;
import com.edupay.entity.User;
import com.edupay.exception.DuplicateResourceException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.UserRepository;
import com.edupay.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        UUID userId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));

        Object[] tenantInfo = getTenantInfo(tenantId);
        String plan = tenantInfo != null && tenantInfo.length > 0 ? (String) tenantInfo[0] : "TRIAL";
        String status = tenantInfo != null && tenantInfo.length > 1 ? (String) tenantInfo[1] : "ACTIVE";

        return buildProfileResponse(user, tenantId, plan, status);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        UUID userId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));

        // Kiểm tra xem số điện thoại mới có bị trùng với user khác không
        if (!user.getPhone().equals(request.getPhone())) {
            // Nếu là owner, kiểm tra toàn hệ thống
            if (Boolean.TRUE.equals(user.getIsOwner()) && userRepository.existsOwnerByPhone(request.getPhone())) {
                throw new DuplicateResourceException("Số điện thoại " + request.getPhone() + " đã được đăng ký bởi tài khoản khác");
            } else if (userRepository.existsByTenantIdAndPhone(tenantId, request.getPhone())) {
                throw new DuplicateResourceException("Số điện thoại " + request.getPhone() + " đã tồn tại trong trung tâm");
            }
            user.setPhone(request.getPhone());
        }

        user.setFullName(request.getFullName().trim());
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim().isEmpty() ? null : request.getEmail().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim().isEmpty() ? null : request.getAvatarUrl().trim());
        }

        userRepository.save(user);

        // Nếu là owner, cập nhật đồng bộ sang bảng tenants
        if (Boolean.TRUE.equals(user.getIsOwner())) {
            try {
                entityManager.createNativeQuery("""
                        UPDATE tenants SET phone = :phone, email = :email WHERE id = :tenantId
                        """)
                        .setParameter("phone", user.getPhone())
                        .setParameter("email", user.getEmail())
                        .setParameter("tenantId", tenantId)
                        .executeUpdate();
            } catch (Exception ex) {
                log.warn("Không thể cập nhật thông tin tenant: {}", ex.getMessage());
            }
        }

        Object[] tenantInfo = getTenantInfo(tenantId);
        String plan = tenantInfo != null && tenantInfo.length > 0 ? (String) tenantInfo[0] : "TRIAL";
        String status = tenantInfo != null && tenantInfo.length > 1 ? (String) tenantInfo[1] : "ACTIVE";

        log.info("Cập nhật hồ sơ thành công cho userId={}, tenantId={}", userId, tenantId);
        return buildProfileResponse(user, tenantId, plan, status);
    }

    private UserProfileResponse buildProfileResponse(User user, UUID tenantId, String plan, String status) {
        List<String> roles = Boolean.TRUE.equals(user.getIsOwner())
                ? List.of("OWNER", "ROLE_TEACHER")
                : List.of("ROLE_TEACHER");

        return UserProfileResponse.builder()
                .userId(user.getId())
                .tenantId(tenantId)
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .isOwner(user.getIsOwner())
                .roles(roles)
                .subscriptionPlan(plan)
                .subscriptionStatus(status)
                .build();
    }

    private UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            try {
                return UUID.fromString((String) auth.getPrincipal());
            } catch (Exception e) {
                log.warn("Principal is not a valid UUID: {}", auth.getPrincipal());
            }
        }
        // Fallback for dev mode
        return TenantContextFilter.DEV_DEFAULT_USER_ID;
    }

    private UUID getCurrentTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = TenantContextFilter.DEV_DEFAULT_TENANT_ID;
        }
        return tenantId;
    }

    private Object[] getTenantInfo(UUID tenantId) {
        try {
            Object result = entityManager.createNativeQuery(
                            "SELECT subscription_plan, subscription_status FROM tenants WHERE id = :id")
                    .setParameter("id", tenantId)
                    .getSingleResult();
            return (Object[]) result;
        } catch (Exception e) {
            log.warn("Không tìm thấy tenant info cho tenantId={}: {}", tenantId, e.getMessage());
            return new Object[]{"TRIAL", "ACTIVE"};
        }
    }
}

