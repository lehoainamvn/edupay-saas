package com.edupay.service.impl;

import com.edupay.dto.request.CreateUserRequest;
import com.edupay.dto.request.ResetPasswordRequest;
import com.edupay.dto.request.UpdateUserRequest;
import com.edupay.dto.response.AdminUserResponse;
import com.edupay.dto.response.PageResponse;
import com.edupay.entity.Role;
import com.edupay.entity.Tenant;
import com.edupay.entity.User;
import com.edupay.exception.BusinessException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.RoleRepository;
import com.edupay.repository.TenantRepository;
import com.edupay.repository.UserRepository;
import com.edupay.service.AdminUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private final EntityManager entityManager;

    private static final String SUPER_ADMIN_PHONE = "0937712338";
    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getAllUsers(int page, int size, String search, String role, String status, UUID tenantId) {
        List<User> allUsers = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        Map<UUID, String> tenantMap = tenantRepository.findAll().stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (a, b) -> a));

        List<AdminUserResponse> filtered = allUsers.stream()
                .filter(u -> matchesSearch(u, search))
                .filter(u -> matchesStatus(u, status))
                .filter(u -> tenantId == null || tenantId.equals(u.getTenantId()))
                .map(u -> toAdminUserResponse(u, tenantMap))
                .filter(u -> matchesRole(u, role))
                .collect(Collectors.toList());

        long totalElements = filtered.size();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        int fromIndex = Math.min(safePage * safeSize, (int) totalElements);
        int toIndex = Math.min(fromIndex + safeSize, (int) totalElements);

        List<AdminUserResponse> pageContent = filtered.subList(fromIndex, toIndex);
        return PageResponse.of(pageContent, totalElements, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng với ID: " + id));

        Map<UUID, String> tenantMap = tenantRepository.findAll().stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (a, b) -> a));
        return toAdminUserResponse(user, tenantMap);
    }

    @Override
    @Transactional
    public AdminUserResponse createUser(CreateUserRequest request) {
        UUID targetTenantId = request.getTenantId();
        if (targetTenantId == null) {
            targetTenantId = tenantRepository.findById(DEFAULT_TENANT_ID)
                    .map(Tenant::getId)
                    .orElseGet(() -> tenantRepository.findAll().stream().findFirst().map(Tenant::getId)
                            .orElse(DEFAULT_TENANT_ID));
        }

        if (userRepository.existsByTenantIdAndPhone(targetTenantId, request.getPhone().trim())) {
            throw new BusinessException("PHONE_ALREADY_EXISTS", "Số điện thoại " + request.getPhone() + " đã tồn tại trong trung tâm này");
        }

        User user = User.builder()
                .phone(request.getPhone().trim())
                .fullName(request.getFullName().trim())
                .email(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .isOwner(false)
                .build();
        user.setTenantId(targetTenantId);
        User savedUser = userRepository.save(user);

        // Gán Role
        String roleCode = (request.getRoleCode() != null && !request.getRoleCode().isBlank())
                ? request.getRoleCode().trim().toUpperCase()
                : "TEACHER";

        Role role = roleRepository.findByCodeAndTenantIdIsNull(roleCode)
                .orElseGet(() -> roleRepository.findByCodeAndTenantIdIsNull("TEACHER")
                        .orElseThrow(() -> new ResourceNotFoundException("ROLE_NOT_FOUND", "Không tìm thấy vai trò: " + roleCode)));

        final UUID finalTenantId = targetTenantId;
        entityManager.createNativeQuery("""
                INSERT INTO user_roles (tenant_id, user_id, role_id)
                VALUES (:tenantId, :userId, :roleId)
                ON CONFLICT (user_id, role_id) DO NOTHING
                """)
                .setParameter("tenantId", finalTenantId)
                .setParameter("userId", savedUser.getId())
                .setParameter("roleId", role.getId())
                .executeUpdate();

        log.info("SuperAdmin tạo người dùng mới: phone={}, fullName={}, tenantId={}, role={}",
                savedUser.getPhone(), savedUser.getFullName(), finalTenantId, roleCode);

        Map<UUID, String> tenantMap = tenantRepository.findAll().stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (a, b) -> a));
        return toAdminUserResponse(savedUser, tenantMap);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng với ID: " + id));

        user.setFullName(request.getFullName().trim());
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim().isEmpty() ? null : request.getEmail().trim());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            user.setStatus(request.getStatus().trim().toUpperCase());
        }

        // Cập nhật Roles nếu được cung cấp
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            entityManager.createNativeQuery("DELETE FROM user_roles WHERE user_id = :userId")
                    .setParameter("userId", id)
                    .executeUpdate();

            for (String rCode : request.getRoles()) {
                roleRepository.findByCodeAndTenantIdIsNull(rCode.trim().toUpperCase()).ifPresent(role -> {
                    entityManager.createNativeQuery("""
                            INSERT INTO user_roles (tenant_id, user_id, role_id)
                            VALUES (:tenantId, :userId, :roleId)
                            ON CONFLICT (user_id, role_id) DO NOTHING
                            """)
                            .setParameter("tenantId", user.getTenantId())
                            .setParameter("userId", id)
                            .setParameter("roleId", role.getId())
                            .executeUpdate();
                });
            }
        }

        User updatedUser = userRepository.save(user);
        log.info("SuperAdmin cập nhật thông tin người dùng: userId={}, fullName={}", id, updatedUser.getFullName());

        Map<UUID, String> tenantMap = tenantRepository.findAll().stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (a, b) -> a));
        return toAdminUserResponse(updatedUser, tenantMap);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserStatus(UUID id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng với ID: " + id));

        String upperStatus = status.trim().toUpperCase();
        if (!List.of("ACTIVE", "SUSPENDED").contains(upperStatus)) {
            throw new BusinessException("INVALID_STATUS", "Trạng thái không hợp lệ. Chỉ chấp nhận ACTIVE hoặc SUSPENDED.");
        }

        if (SUPER_ADMIN_PHONE.equals(user.getPhone()) && "SUSPENDED".equalsIgnoreCase(upperStatus)) {
            throw new BusinessException("CANNOT_SUSPEND_SUPERADMIN", "Không thể khóa tài khoản Quản trị viên tối cao");
        }

        user.setStatus(upperStatus);
        if ("ACTIVE".equalsIgnoreCase(upperStatus)) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        User updatedUser = userRepository.save(user);
        log.info("SuperAdmin cập nhật trạng thái người dùng: userId={}, status={}", id, upperStatus);

        Map<UUID, String> tenantMap = tenantRepository.findAll().stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (a, b) -> a));
        return toAdminUserResponse(updatedUser, tenantMap);
    }

    @Override
    @Transactional
    public void resetPassword(UUID id, ResetPasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng với ID: " + id));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        log.info("SuperAdmin đặt lại mật khẩu cho người dùng: userId={}, phone={}", id, user.getPhone());
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng với ID: " + id));

        if (SUPER_ADMIN_PHONE.equals(user.getPhone())) {
            throw new BusinessException("CANNOT_DELETE_SUPERADMIN", "Không thể xóa tài khoản Quản trị viên tối cao của hệ thống");
        }

        entityManager.createNativeQuery("DELETE FROM user_roles WHERE user_id = :userId")
                .setParameter("userId", id)
                .executeUpdate();

        userRepository.delete(user);
        log.info("SuperAdmin xóa người dùng: userId={}, phone={}, name={}", id, user.getPhone(), user.getFullName());
    }

    private AdminUserResponse toAdminUserResponse(User user, Map<UUID, String> tenantMap) {
        List<String> roles = Collections.emptyList();
        try {
            roles = roleRepository.findRoleCodesByUserId(user.getId());
        } catch (Exception e) {
            log.warn("Cannot find role codes for user {}: {}", user.getId(), e.getMessage());
        }

        String tenantName = user.getTenantId() != null
                ? tenantMap.getOrDefault(user.getTenantId(), "Hệ thống EduPay")
                : "Hệ thống EduPay";

        return AdminUserResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .tenantName(tenantName)
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .isOwner(user.getIsOwner() != null ? user.getIsOwner() : false)
                .roles(roles)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private boolean matchesSearch(User user, String search) {
        if (search == null || search.isBlank()) return true;
        String s = search.trim().toLowerCase();
        boolean nameMatch = user.getFullName() != null && user.getFullName().toLowerCase().contains(s);
        boolean phoneMatch = user.getPhone() != null && user.getPhone().contains(s);
        boolean emailMatch = user.getEmail() != null && user.getEmail().toLowerCase().contains(s);
        return nameMatch || phoneMatch || emailMatch;
    }

    private boolean matchesStatus(User user, String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return true;
        return status.trim().equalsIgnoreCase(user.getStatus());
    }

    private boolean matchesRole(AdminUserResponse user, String role) {
        if (role == null || role.isBlank() || "ALL".equalsIgnoreCase(role)) return true;
        if (user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(r -> r.equalsIgnoreCase(role.trim()));
    }
}
