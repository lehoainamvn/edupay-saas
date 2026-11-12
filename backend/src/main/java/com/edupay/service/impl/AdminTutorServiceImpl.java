package com.edupay.service.impl;

import com.edupay.dto.request.CreateTutorRequest;
import com.edupay.dto.request.UpdateTutorRequest;
import com.edupay.dto.response.TenantAdminResponse;
import com.edupay.entity.Tenant;
import com.edupay.entity.User;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.ClassRepository;
import com.edupay.repository.StudentRepository;
import com.edupay.repository.TenantRepository;
import com.edupay.repository.UserRepository;
import com.edupay.service.AdminTutorService;
import jakarta.persistence.EntityManager;
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
public class AdminTutorServiceImpl implements AdminTutorService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<TenantAdminResponse> getAllTutors(String status, String search) {
        List<Tenant> tutors = tenantRepository.findAllByTenantTypeOrderByCreatedAtDesc("SOLO_TUTOR");
        Instant now = Instant.now();

        return tutors.stream()
                .filter(t -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status) || t.getSubscriptionStatus().equalsIgnoreCase(status))
                .filter(t -> search == null || search.isBlank() || matchesSearch(t, search.trim().toLowerCase()))
                .map(t -> toTutorResponse(t, now))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantAdminResponse getTutorById(UUID tutorId) {
        Tenant tutor = tenantRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("TUTOR_NOT_FOUND", "Không tìm thấy Gia sư với ID: " + tutorId));
        return toTutorResponse(tutor, Instant.now());
    }

    @Override
    @Transactional
    public TenantAdminResponse createTutor(CreateTutorRequest request) {
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

        // 1. Tạo Tenant kiểu SOLO_TUTOR
        Tenant tenant = Tenant.builder()
                .name(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .passwordHash(encodedPassword)
                .subscriptionPlan(request.getSubscriptionPlan() != null ? request.getSubscriptionPlan().toUpperCase() : "MONTHLY")
                .subscriptionStatus("ACTIVE")
                .tenantType("SOLO_TUTOR")
                .trialEndsAt(now.plus(14, ChronoUnit.DAYS))
                .subscriptionEndsAt(subscriptionEndsAt)
                .build();
        Tenant savedTenant = tenantRepository.save(tenant);

        // 2. Tạo User Gia sư (isOwner = true)
        User tutorUser = User.builder()
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .passwordHash(encodedPassword)
                .isOwner(true)
                .status("ACTIVE")
                .failedLoginAttempts(0)
                .build();
        tutorUser.setTenantId(savedTenant.getId());
        User savedUser = userRepository.save(tutorUser);

        // 3. Gán Role OWNER và SOLO_TUTOR
        assignRole(savedTenant.getId(), savedUser.getId(), "OWNER");
        assignRole(savedTenant.getId(), savedUser.getId(), "SOLO_TUTOR");

        log.info("SuperAdmin đã tạo mới Gia sư tự do [{}] với SĐT [{}]", savedTenant.getName(), savedTenant.getPhone());
        return toTutorResponse(savedTenant, now);
    }

    @Override
    @Transactional
    public TenantAdminResponse updateTutor(UUID tutorId, UpdateTutorRequest request) {
        Tenant tutor = tenantRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("TUTOR_NOT_FOUND", "Không tìm thấy Gia sư với ID: " + tutorId));

        tutor.setName(request.getFullName().trim());
        if (request.getEmail() != null) {
            tutor.setEmail(request.getEmail().trim());
        }
        if (request.getSubscriptionPlan() != null && !request.getSubscriptionPlan().isBlank()) {
            tutor.setSubscriptionPlan(request.getSubscriptionPlan().toUpperCase());
        }
        if (request.getSubscriptionStatus() != null && !request.getSubscriptionStatus().isBlank()) {
            tutor.setSubscriptionStatus(request.getSubscriptionStatus().toUpperCase());
        }
        if (request.getSubscriptionEndsAt() != null) {
            tutor.setSubscriptionEndsAt(request.getSubscriptionEndsAt());
        }

        Tenant saved = tenantRepository.save(tutor);

        // Cập nhật tên của User Owner
        userRepository.findFirstByTenantIdAndIsOwnerTrue(tutorId).ifPresent(u -> {
            u.setFullName(request.getFullName().trim());
            userRepository.save(u);
        });

        log.info("SuperAdmin đã cập nhật Gia sư [{}]", saved.getName());
        return toTutorResponse(saved, Instant.now());
    }

    @Override
    @Transactional
    public void deleteTutor(UUID tutorId) {
        Tenant tutor = tenantRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("TUTOR_NOT_FOUND", "Không tìm thấy Gia sư với ID: " + tutorId));

        tenantRepository.delete(tutor);
        log.info("SuperAdmin đã xóa Gia sư tự do [{}]", tutor.getName());
    }

    private void assignRole(UUID tenantId, UUID userId, String roleCode) {
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

    private TenantAdminResponse toTutorResponse(Tenant t, Instant now) {
        long totalClasses = classRepository.findAllByTenantIdOrderByIsActiveDescCreatedAtDesc(t.getId()).size();
        long totalStudents = studentRepository.findAllByTenantIdOrderByCreatedAtDesc(t.getId()).size();
        boolean isExpired = t.getSubscriptionEndsAt() != null && t.getSubscriptionEndsAt().isBefore(now);

        String repName = t.getName();
        String repPhone = t.getPhone();
        User owner = userRepository.findFirstByTenantIdAndIsOwnerTrue(t.getId()).orElse(null);
        if (owner != null) {
            repName = owner.getFullName();
            repPhone = owner.getPhone();
        }

        return TenantAdminResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .phone(t.getPhone())
                .email(t.getEmail())
                .tenantType("SOLO_TUTOR")
                .subscriptionPlan(t.getSubscriptionPlan())
                .subscriptionStatus(t.getSubscriptionStatus())
                .subscriptionEndsAt(t.getSubscriptionEndsAt())
                .createdAt(t.getCreatedAt())
                .totalTeachers(0)
                .totalClasses(totalClasses)
                .totalStudents(totalStudents)
                .isExpired(isExpired)
                .representativeName(repName)
                .representativePhone(repPhone)
                .build();
    }

    private boolean matchesSearch(Tenant t, String query) {
        String combined = (t.getName() + " " + t.getPhone() + " " + (t.getEmail() != null ? t.getEmail() : "")).toLowerCase();
        return combined.contains(query);
    }
}
