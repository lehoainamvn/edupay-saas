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
import com.edupay.service.ClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final ClassMapper classMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> getClasses(boolean includeArchived) {
        UUID tenantId = requireTenantId();

        List<ClassEntity> entities = includeArchived
                ? classRepository.findAllByTenantIdOrderByIsActiveDescCreatedAtDesc(tenantId)
                : classRepository.findAllByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(tenantId);

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
        String trimmedName = request.getName().trim();

        if (classRepository.existsByTenantIdAndNameIgnoreCaseAndIsActiveTrue(tenantId, trimmedName)) {
            throw new DuplicateResourceException("Tên lớp học '" + trimmedName + "' đã tồn tại trong danh sách lớp đang mở của bạn");
        }

        ClassEntity entity = classMapper.toEntity(request);
        entity.setName(trimmedName);
        entity.setTenantId(tenantId);
        entity.setIsActive(Boolean.TRUE);

        ClassEntity saved = classRepository.save(entity);
        log.info("Tenant [{}] created new class [{}] with id [{}]", tenantId, saved.getName(), saved.getId());

        ClassResponse response = classMapper.toResponse(saved);
        response.setStudentCount(0L);
        return response;
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
        return response;
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Không xác định được thông tin giáo viên (Tenant Context)");
        }
        return tenantId;
    }
}
