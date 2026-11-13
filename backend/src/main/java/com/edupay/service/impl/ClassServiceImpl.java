package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.ClassRequest;
import com.edupay.dto.response.ClassResponse;
import com.edupay.entity.ClassEntity;
import com.edupay.exception.BusinessException;
import com.edupay.exception.DuplicateResourceException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.mapper.ClassMapper;
import com.edupay.repository.ClassRepository;
import com.edupay.repository.TenantRepository;
import com.edupay.repository.TenantTeacherPolicyRepository;
import com.edupay.repository.UserRepository;
import com.edupay.service.ClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Triển khai nghiệp vụ Quản lý Lớp học.
 * Tuân thủ nghiêm ngặt luật Multi-Tenant P0:
 * - Luôn lấy tenantId từ TenantContext.getTenantId()
 * - Mọi thao tác đọc/ghi/xóa đều lọc theo tenantId
 * - Sử dụng Soft Delete (isActive = false) để bảo toàn lịch sử điểm danh & học phí
 * - TASK 3: Scoping theo giáo viên phụ trách (nếu là giáo viên phụ thuộc trung tâm)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantTeacherPolicyRepository policyRepository;
    private final ClassMapper classMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> getClasses(boolean includeArchived) {
        UUID tenantId = requireTenantId();
        UUID currentUserId = getCurrentUserId();

        List<ClassEntity> entities;
        if (isTeacherOnly(currentUserId)) {
            // Giáo viên phụ thuộc trung tâm: chỉ xem các lớp được phân công dạy
            entities = includeArchived
                    ? classRepository.findAllByTenantIdAndTeacherIdOrderByIsActiveDescCreatedAtDesc(tenantId, currentUserId)
                    : classRepository.findAllByTenantIdAndTeacherIdAndIsActiveTrueOrderByCreatedAtDesc(tenantId, currentUserId);
        } else {
            // Chủ trung tâm hoặc Gia sư tự do: xem toàn bộ các lớp của tenant
            entities = includeArchived
                    ? classRepository.findAllByTenantIdOrderByIsActiveDescCreatedAtDesc(tenantId)
                    : classRepository.findAllByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(tenantId);
        }

        return entities.stream()
                .map(entity -> enrichWithStudentCount(entity, tenantId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassResponse getClassById(UUID id) {
        UUID tenantId = requireTenantId();
        ClassEntity entity = findEntityByIdAndTenant(id, tenantId);
        return enrichWithStudentCount(entity, tenantId);
    }

    @Override
    @Transactional
    public ClassResponse createClass(ClassRequest request) {
        UUID tenantId = requireTenantId();
        UUID currentUserId = getCurrentUserId();

        if (isTeacherOnly(currentUserId)) {
            var tenantOpt = tenantRepository.findById(tenantId);
            if (tenantOpt.isPresent() && "CENTER".equalsIgnoreCase(tenantOpt.get().getTenantType())) {
                var policyOpt = policyRepository.findByTenantId(tenantId);
                if (policyOpt.isPresent() && Boolean.FALSE.equals(policyOpt.get().getCanCreateClass())) {
                    throw new BusinessException("PERMISSION_DENIED", "Bạn không có quyền tự mở lớp học theo quy định của trung tâm. Vui lòng liên hệ Quản lý Trung tâm.");
                }
            }
        }

        String trimmedName = request.getName().trim();

        if (classRepository.existsByTenantIdAndNameIgnoreCaseAndIsActiveTrue(tenantId, trimmedName)) {
            throw new DuplicateResourceException("Tên lớp học '" + trimmedName + "' đã tồn tại trong danh sách lớp đang mở của bạn");
        }

        ClassEntity entity = classMapper.toEntity(request);
        entity.setName(trimmedName);
        entity.setTenantId(tenantId);
        entity.setIsActive(Boolean.TRUE);
        if (request.getTeacherId() != null) {
            entity.setTeacherId(request.getTeacherId());
        }

        ClassEntity saved = classRepository.save(entity);
        log.info("Tenant [{}] created new class [{}] with id [{}]", tenantId, saved.getName(), saved.getId());

        return enrichWithStudentCount(saved, tenantId);
    }

    @Override
    @Transactional
    public ClassResponse updateClass(UUID id, ClassRequest request) {
        UUID tenantId = requireTenantId();
        ClassEntity entity = findEntityByIdAndTenant(id, tenantId);

        String trimmedName = request.getName().trim();
        if (classRepository.existsByTenantIdAndNameIgnoreCaseAndIdNotAndIsActiveTrue(tenantId, trimmedName, id)) {
            throw new DuplicateResourceException("Tên lớp học '" + trimmedName + "' đã được sử dụng cho một lớp khác");
        }

        classMapper.updateEntityFromRequest(request, entity);
        entity.setName(trimmedName);
        if (request.getTeacherId() != null) {
            entity.setTeacherId(request.getTeacherId());
        }

        ClassEntity updated = classRepository.save(entity);
        log.info("Tenant [{}] updated class id [{}]", tenantId, updated.getId());

        return enrichWithStudentCount(updated, tenantId);
    }

    @Override
    @Transactional
    public void softDeleteClass(UUID id) {
        UUID tenantId = requireTenantId();
        ClassEntity entity = findEntityByIdAndTenant(id, tenantId);

        if (Boolean.FALSE.equals(entity.getIsActive())) {
            log.debug("Class [{}] of tenant [{}] is already archived", id, tenantId);
            return;
        }

        entity.setIsActive(Boolean.FALSE);
        classRepository.save(entity);
        log.info("Tenant [{}] soft-deleted (archived) class id [{}] to preserve attendance & billing history", tenantId, id);
    }

    @Override
    @Transactional
    public ClassResponse restoreClass(UUID id) {
        UUID tenantId = requireTenantId();
        ClassEntity entity = findEntityByIdAndTenant(id, tenantId);

        if (classRepository.existsByTenantIdAndNameIgnoreCaseAndIdNotAndIsActiveTrue(tenantId, entity.getName(), id)) {
            throw new DuplicateResourceException("Không thể khôi phục vì đã có lớp đang hoạt động trùng tên '" + entity.getName() + "'");
        }

        entity.setIsActive(Boolean.TRUE);
        ClassEntity restored = classRepository.save(entity);
        log.info("Tenant [{}] restored archived class id [{}]", tenantId, id);

        return enrichWithStudentCount(restored, tenantId);
    }

    private ClassEntity findEntityByIdAndTenant(UUID id, UUID tenantId) {
        return classRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học trong tài khoản của bạn"));
    }

    private ClassResponse enrichWithStudentCount(ClassEntity entity, UUID tenantId) {
        ClassResponse response = classMapper.toResponse(entity);
        long studentCount = classRepository.countActiveStudentsByClassIdAndTenantId(entity.getId(), tenantId);
        response.setStudentCount(studentCount);
        if (entity.getTeacherId() != null) {
            userRepository.findById(entity.getTeacherId()).ifPresent(t -> {
                response.setTeacherName(t.getFullName());
                response.setTeacherPhone(t.getPhone());
            });
        }
        return response;
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Không xác định được thông tin giáo viên (Tenant Context)");
        }
        return tenantId;
    }

    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                return UUID.fromString(auth.getName());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isTeacherOnly(UUID userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(u -> !Boolean.TRUE.equals(u.getIsOwner()))
                .orElse(false);
    }
}
