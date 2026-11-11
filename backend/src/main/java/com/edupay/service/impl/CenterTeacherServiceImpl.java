package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.CreateCenterTeacherRequest;
import com.edupay.dto.request.UpdateCenterTeacherRequest;
import com.edupay.dto.response.CenterTeacherResponse;
import com.edupay.entity.User;
import com.edupay.exception.BusinessException;
import com.edupay.exception.DuplicateResourceException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.ClassRepository;
import com.edupay.repository.RoleRepository;
import com.edupay.repository.UserRepository;
import com.edupay.security.SecuritySanitizer;
import com.edupay.service.AuditLogService;
import com.edupay.service.CenterTeacherService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CenterTeacherServiceImpl implements CenterTeacherService {

    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<CenterTeacherResponse> getTeachers() {
        UUID tenantId = requireTenantId();

        List<User> teachers = userRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .filter(u -> !Boolean.TRUE.equals(u.getIsOwner()))
                .toList();

        return teachers.stream()
                .map(t -> mapToResponse(t, tenantId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CenterTeacherResponse getTeacherById(UUID id) {
        UUID tenantId = requireTenantId();
        User teacher = findTeacherInTenant(id, tenantId);
        return mapToResponse(teacher, tenantId);
    }

    @Override
    @Transactional
    public CenterTeacherResponse createTeacher(CreateCenterTeacherRequest request) {
        UUID tenantId = requireTenantId();

        // 1. Kiểm tra SĐT đã tồn tại trong cùng Tenant chưa
        if (userRepository.findByTenantIdAndPhone(tenantId, request.getPhone()).isPresent()) {
            throw new DuplicateResourceException("Số điện thoại " + request.getPhone() + " đã tồn tại trong trung tâm của bạn");
        }

        String cleanFullName = SecuritySanitizer.sanitizePlainText(request.getFullName());
        if (cleanFullName == null || cleanFullName.isBlank()) {
            cleanFullName = request.getFullName().trim();
        }

        // 2. Tạo User giáo viên mới
        User teacher = User.builder()
                .phone(request.getPhone().trim())
                .fullName(cleanFullName)
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .isOwner(false)
                .build();
        teacher.setTenantId(tenantId);

        teacher = userRepository.save(teacher);
        log.info("Trung tâm [{}] đã thêm giáo viên mới [{}] phone={}", tenantId, teacher.getFullName(), teacher.getPhone());

        // 3. Gán Role TEACHER cho giáo viên
        final UUID teacherId = teacher.getId();
        entityManager.createNativeQuery("""
                INSERT INTO user_roles (tenant_id, user_id, role_id)
                SELECT :tenantId, :userId, r.id FROM roles r
                WHERE r.code = 'TEACHER' AND r.tenant_id IS NULL
                ON CONFLICT (user_id, role_id) DO NOTHING
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("userId", teacherId)
                .executeUpdate();

        auditLogService.logInfo(tenantId, teacherId, "CENTER_TEACHER_CREATED", "USER", teacherId.toString(), null, null);

        return mapToResponse(teacher, tenantId);
    }

    @Override
    @Transactional
    public CenterTeacherResponse updateTeacher(UUID id, UpdateCenterTeacherRequest request) {
        UUID tenantId = requireTenantId();
        User teacher = findTeacherInTenant(id, tenantId);

        String cleanFullName = SecuritySanitizer.sanitizePlainText(request.getFullName());
        if (cleanFullName != null && !cleanFullName.isBlank()) {
            teacher.setFullName(cleanFullName);
        }
        if (request.getEmail() != null) {
            teacher.setEmail(request.getEmail().trim());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            teacher.setStatus(request.getStatus().toUpperCase());
        }

        User updated = userRepository.save(teacher);
        log.info("Trung tâm [{}] đã cập nhật thông tin giáo viên [{}]", tenantId, updated.getId());

        return mapToResponse(updated, tenantId);
    }

    @Override
    @Transactional
    public CenterTeacherResponse updateTeacherStatus(UUID id, String status) {
        UUID tenantId = requireTenantId();
        User teacher = findTeacherInTenant(id, tenantId);

        String newStatus = status.equalsIgnoreCase("ACTIVE") ? "ACTIVE" : "SUSPENDED";
        teacher.setStatus(newStatus);
        User updated = userRepository.save(teacher);

        log.info("Trung tâm [{}] đã chuyển trạng thái giáo viên [{}] sang [{}]", tenantId, id, newStatus);
        return mapToResponse(updated, tenantId);
    }

    @Override
    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        UUID tenantId = requireTenantId();
        User teacher = findTeacherInTenant(id, tenantId);

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BusinessException("INVALID_PASSWORD", "Mật khẩu mới phải có tối thiểu 6 ký tự");
        }

        teacher.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        teacher.setFailedLoginAttempts(0);
        teacher.setLockedUntil(null);
        userRepository.save(teacher);

        log.info("Trung tâm [{}] đã đặt lại mật khẩu cho giáo viên [{}]", tenantId, id);
    }

    @Override
    @Transactional
    public void deleteTeacher(UUID id) {
        UUID tenantId = requireTenantId();
        User teacher = findTeacherInTenant(id, tenantId);

        // Kiểm tra xem giáo viên có đang phụ trách lớp học nào không
        long activeClasses = classRepository.countByTenantIdAndTeacherIdAndIsActiveTrue(tenantId, id);
        if (activeClasses > 0) {
            throw new BusinessException("TEACHER_HAS_ACTIVE_CLASSES",
                    "Không thể xóa giáo viên đang phụ trách " + activeClasses + " lớp học. Vui lòng chuyển giao lớp trước.");
        }

        // Chuyển sang trạng thái SUSPENDED hoặc xóa
        teacher.setStatus("SUSPENDED");
        userRepository.save(teacher);
        log.info("Trung tâm [{}] đã tạm dừng/xóa giáo viên [{}]", tenantId, id);
    }

    private User findTeacherInTenant(UUID id, UUID tenantId) {
        return userRepository.findByIdAndTenantId(id, tenantId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsOwner()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo viên trong trung tâm của bạn"));
    }

    private CenterTeacherResponse mapToResponse(User user, UUID tenantId) {
        long classesCount = classRepository.countByTenantIdAndTeacherIdAndIsActiveTrue(tenantId, user.getId());
        return CenterTeacherResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.getStatus())
                .assignedClassesCount(classesCount)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("TENANT_REQUIRED", "Không tìm thấy thông tin trung tâm (Tenant Context)");
        }
        return tenantId;
    }
}

