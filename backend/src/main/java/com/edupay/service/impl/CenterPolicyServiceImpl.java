package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.UpdateTenantTeacherPolicyRequest;
import com.edupay.dto.response.TenantTeacherPolicyDto;
import com.edupay.entity.TenantTeacherPolicy;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.TenantRepository;
import com.edupay.repository.TenantTeacherPolicyRepository;
import com.edupay.service.CenterPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CenterPolicyServiceImpl implements CenterPolicyService {

    private final TenantTeacherPolicyRepository policyRepository;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional(readOnly = true)
    public TenantTeacherPolicyDto getMyPolicy() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return TenantTeacherPolicyDto.builder()
                    .canCreateClass(true)
                    .canCreateStudent(true)
                    .canEditStudentFee(true)
                    .canViewBilling(true)
                    .build();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = false;
        if (auth != null) {
            isPrivileged = auth.getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().contains("SUPER_ADMIN") ||
                    a.getAuthority().contains("CENTER_ADMIN") ||
                    a.getAuthority().contains("OWNER"));
        }

        var tenantOpt = tenantRepository.findById(tenantId);
        boolean isCenter = tenantOpt.isPresent() && "CENTER".equalsIgnoreCase(tenantOpt.get().getTenantType());

        if (!isCenter || isPrivileged) {
            return TenantTeacherPolicyDto.builder()
                    .tenantId(tenantId)
                    .canCreateClass(true)
                    .canCreateStudent(true)
                    .canEditStudentFee(true)
                    .canViewBilling(true)
                    .build();
        }

        return getTenantPolicy(tenantId);
    }

    @Override
    @Transactional
    public TenantTeacherPolicyDto getTenantPolicy(UUID tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("TENANT_NOT_FOUND", "Không tìm thấy cơ sở với ID: " + tenantId);
        }

        TenantTeacherPolicy policy = policyRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    // Nếu chưa có, tự tạo mặc định an toàn (toàn bộ false)
                    TenantTeacherPolicy newPolicy = TenantTeacherPolicy.builder()
                            .tenantId(tenantId)
                            .canEditStudentFee(false)
                            .canCreateClass(false)
                            .canCreateStudent(false)
                            .canViewBilling(false)
                            .build();
                    return policyRepository.save(newPolicy);
                });

        return toDto(policy);
    }

    @Override
    @Transactional
    public TenantTeacherPolicyDto updateTenantPolicy(UUID tenantId, UpdateTenantTeacherPolicyRequest request) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("TENANT_NOT_FOUND", "Không tìm thấy cơ sở với ID: " + tenantId);
        }

        TenantTeacherPolicy policy = policyRepository.findByTenantId(tenantId)
                .orElseGet(() -> TenantTeacherPolicy.builder().tenantId(tenantId).build());

        if (request.getCanEditStudentFee() != null) {
            policy.setCanEditStudentFee(request.getCanEditStudentFee());
        }
        if (request.getCanCreateClass() != null) {
            policy.setCanCreateClass(request.getCanCreateClass());
        }
        if (request.getCanCreateStudent() != null) {
            policy.setCanCreateStudent(request.getCanCreateStudent());
        }
        if (request.getCanViewBilling() != null) {
            policy.setCanViewBilling(request.getCanViewBilling());
        }

        TenantTeacherPolicy saved = policyRepository.save(policy);
        log.info("Cập nhật chính sách giáo viên nội bộ cho Tenant [{}]: fee={}, class={}, student={}, billing={}",
                tenantId, saved.getCanEditStudentFee(), saved.getCanCreateClass(), saved.getCanCreateStudent(), saved.getCanViewBilling());

        return toDto(saved);
    }

    private TenantTeacherPolicyDto toDto(TenantTeacherPolicy p) {
        return TenantTeacherPolicyDto.builder()
                .id(p.getId())
                .tenantId(p.getTenantId())
                .canEditStudentFee(Boolean.TRUE.equals(p.getCanEditStudentFee()))
                .canCreateClass(Boolean.TRUE.equals(p.getCanCreateClass()))
                .canCreateStudent(Boolean.TRUE.equals(p.getCanCreateStudent()))
                .canViewBilling(Boolean.TRUE.equals(p.getCanViewBilling()))
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt())
                .build();
    }
}

