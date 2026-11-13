package com.edupay.service.impl;

import com.edupay.dto.request.CreateCenterRequest;
import com.edupay.dto.request.UpdateCenterRequest;
import com.edupay.dto.response.CenterTeacherResponse;
import com.edupay.dto.response.TenantAdminResponse;
import com.edupay.entity.Role;
import com.edupay.entity.Tenant;
import com.edupay.entity.TenantTeacherPolicy;
import com.edupay.entity.User;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.*;
import com.edupay.service.AdminCenterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCenterServiceImpl implements AdminCenterService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final RoleRepository roleRepository;
    private final TenantTeacherPolicyRepository policyRepository;
    private final PasswordEncoder passwordEncoder;
    private final jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<TenantAdminResponse> getAllCenters(String status, String search) {
        List<Tenant> centers = tenantRepository.findAllByTenantTypeOrderByCreatedAtDesc("CENTER");
        Instant now = Instant.now();

        return centers.stream()
                .filter(c -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status) || c.getSubscriptionStatus().equalsIgnoreCase(status))
                .filter(c -> search == null || search.isBlank() || matchesSearch(c, search.trim().toLowerCase()))
                .map(c -> toCenterResponse(c, now))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantAdminResponse getCenterById(UUID centerId) {
        Tenant center = tenantRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("CENTER_NOT_FOUND", "Không tìm thấy Trung tâm với ID: " + centerId));
        return toCenterResponse(center, Instant.now());
    }

    @Override
    @Transactional
    public TenantAdminResponse createCenter(CreateCenterRequest request) {
        if (tenantRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new BusinessException("PHONE_ALREADY_EXISTS", "Số điện thoại này đã được đăng ký trên hệ thống");
        }

        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword().trim()
                : "Admin@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        int months = (request.getDurationMonths() != null && request.getDurationMonths() > 0) ? request.getDurationMonths() : 1;
        Instant now = Instant.now();
        Instant subscriptionEndsAt = now.plus(months * 30L, ChronoUnit.DAYS);

        // 1. Tạo Tenant kiểu CENTER
        Tenant tenant = Tenant.builder()
                .name(request.getName().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .passwordHash(encodedPassword)
                .subscriptionPlan(request.getSubscriptionPlan() != null ? request.getSubscriptionPlan().toUpperCase() : "MONTHLY")
                .subscriptionStatus("ACTIVE")
                .tenantType("CENTER")
                .trialEndsAt(now.plus(14, ChronoUnit.DAYS))
                .subscriptionEndsAt(subscriptionEndsAt)
                .build();
        Tenant savedTenant = tenantRepository.save(tenant);

        // 2. Tạo User Chủ trung tâm (Owner)
        User ownerUser = User.builder()
                .fullName(request.getRepresentativeName().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .passwordHash(encodedPassword)
                .isOwner(true)
                .status("ACTIVE")
                .failedLoginAttempts(0)
                .build();
        ownerUser.setTenantId(savedTenant.getId());
        User savedUser = userRepository.save(ownerUser);

        // 3. Gán Role OWNER và CENTER_ADMIN
        assignRoleToUser(savedTenant.getId(), savedUser.getId(), "OWNER");
        assignRoleToUser(savedTenant.getId(), savedUser.getId(), "CENTER_ADMIN");

        // 4. Tạo Policy cấu hình nội bộ mặc định cho Trung tâm
        TenantTeacherPolicy policy = TenantTeacherPolicy.builder()
                .tenantId(savedTenant.getId())
                .canEditStudentFee(false)
                .canCreateClass(false)
                .canCreateStudent(false)
                .canViewBilling(false)
                .build();
        policyRepository.save(policy);

        log.info("SuperAdmin đã tạo mới Trung tâm [{}] với SĐT chủ [{}]", savedTenant.getName(), savedTenant.getPhone());
        return toCenterResponse(savedTenant, now);
    }

    @Override
    @Transactional
    public TenantAdminResponse updateCenter(UUID centerId, UpdateCenterRequest request) {
        Tenant center = tenantRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("CENTER_NOT_FOUND", "Không tìm thấy Trung tâm với ID: " + centerId));

        center.setName(request.getName().trim());
        if (request.getEmail() != null) {
            center.setEmail(request.getEmail().trim());
        }
        if (request.getSubscriptionPlan() != null && !request.getSubscriptionPlan().isBlank()) {
            center.setSubscriptionPlan(request.getSubscriptionPlan().toUpperCase());
        }
        if (request.getSubscriptionStatus() != null && !request.getSubscriptionStatus().isBlank()) {
            center.setSubscriptionStatus(request.getSubscriptionStatus().toUpperCase());
        }
        if (request.getSubscriptionEndsAt() != null) {
            center.setSubscriptionEndsAt(request.getSubscriptionEndsAt());
        }

        Tenant saved = tenantRepository.save(center);

        // Cập nhật tên đại diện của Owner nếu có
        if (request.getRepresentativeName() != null && !request.getRepresentativeName().isBlank()) {
            userRepository.findFirstByTenantIdAndIsOwnerTrue(centerId).ifPresent(owner -> {
                owner.setFullName(request.getRepresentativeName().trim());
                userRepository.save(owner);
            });
        }

        log.info("SuperAdmin cập nhật thông tin Trung tâm [{}]", saved.getName());
        return toCenterResponse(saved, Instant.now());
    }

    @Override
    @Transactional
    public void deleteCenter(UUID centerId) {
        Tenant center = tenantRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("CENTER_NOT_FOUND", "Không tìm thấy Trung tâm với ID: " + centerId));

        tenantRepository.delete(center);
        log.info("SuperAdmin đã xóa Trung tâm [{}]", center.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CenterTeacherResponse> getCenterTeachers(UUID centerId) {
        List<User> teachers = userRepository.findAllByTenantIdAndIsOwnerFalseOrderByCreatedAtDesc(centerId);
        return teachers.stream().map(t -> {
            long assignedClasses = classRepository.findAllByTenantIdOrderByIsActiveDescCreatedAtDesc(centerId)
                    .stream()
                    .filter(c -> c.getTeacherId() != null && c.getTeacherId().equals(t.getId()))
                    .count();

            return CenterTeacherResponse.builder()
                    .id(t.getId())
                    .fullName(t.getFullName())
                    .phone(t.getPhone())
                    .email(t.getEmail())
                    .status(t.getStatus())
                    .assignedClassesCount(assignedClasses)
                    .createdAt(t.getCreatedAt())
                    .build();
        }).toList();
    }

    private void assignRoleToUser(UUID tenantId, UUID userId, String roleCode) {
        try {
            entityManager.createNativeQuery("""
                    INSERT INTO user_roles (id, tenant_id, user_id, role_id)
                    SELECT gen_random_uuid(), :tenantId, :userId, r.id
                    FROM roles r
                    WHERE r.code = :roleCode
                    ON CONFLICT (user_id, role_id) DO NOTHING
                    """)
                    .setParameter("tenantId", tenantId)
                    .setParameter("userId", userId)
                    .setParameter("roleCode", roleCode)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Không thể gán role [{}] cho user [{}]: {}", roleCode, userId, e.getMessage());
        }
    }

    private TenantAdminResponse toCenterResponse(Tenant c, Instant now) {
        long totalClasses = classRepository.findAllByTenantIdOrderByIsActiveDescCreatedAtDesc(c.getId()).size();
        long totalStudents = studentRepository.findAllByTenantIdOrderByCreatedAtDesc(c.getId()).size();
        long totalTeachers = userRepository.countByTenantIdAndIsOwnerFalse(c.getId());
        boolean isExpired = c.getSubscriptionEndsAt() != null && c.getSubscriptionEndsAt().isBefore(now);

        String repName = c.getName();
        String repPhone = c.getPhone();
        User owner = userRepository.findFirstByTenantIdAndIsOwnerTrue(c.getId()).orElse(null);
        if (owner != null) {
            repName = owner.getFullName();
            repPhone = owner.getPhone();
        }

        return TenantAdminResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .tenantType("CENTER")
                .subscriptionPlan(c.getSubscriptionPlan())
                .subscriptionStatus(c.getSubscriptionStatus())
                .subscriptionEndsAt(c.getSubscriptionEndsAt())
                .createdAt(c.getCreatedAt())
                .totalTeachers(totalTeachers)
                .totalClasses(totalClasses)
                .totalStudents(totalStudents)
                .isExpired(isExpired)
                .representativeName(repName)
                .representativePhone(repPhone)
                .build();
    }

    private boolean matchesSearch(Tenant c, String query) {
        String combined = (c.getName() + " " + c.getPhone() + " " + (c.getEmail() != null ? c.getEmail() : "")).toLowerCase();
        return combined.contains(query);
    }
}
