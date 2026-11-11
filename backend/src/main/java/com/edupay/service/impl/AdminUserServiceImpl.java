package com.edupay.service.impl;

import com.edupay.dto.request.CreateUserRequest;
import com.edupay.dto.request.ResetPasswordRequest;
import com.edupay.dto.request.UpdateUserRequest;
import com.edupay.dto.response.AdminUserResponse;
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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
    public Page<AdminUserResponse> getAllUsers(int page, int size, String search, String role, String status, UUID tenantId) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Tìm kiếm theo Tên, SĐT, Email
            if (search != null && !search.isBlank()) {
                String s = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), s);
                Predicate phoneMatch = cb.like(root.get("phone"), s);
                Predicate emailMatch = cb.like(cb.lower(cb.coalesce(root.get("email"), "")), s);
                predicates.add(cb.or(nameMatch, phoneMatch, emailMatch));
            }

            // 2. Lọc theo trạng thái
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim().toUpperCase()));
            }

            // 3. Lọc theo Tenant
            if (tenantId != null) {
                predicates.add(cb.equal(root.get("tenantId"), tenantId));
            }

            // 4. Lọc theo Vai trò (Role)
            if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
                List<UUID> matchingUserIds = roleRepository.findUserIdsByRoleCode(role.trim().toUpperCase());
                if (matchingUserIds.isEmpty()) {
                    predicates.add(cb.disjunction()); // Không có user nào
                } else {
                    predicates.add(root.get("id").in(matchingUserIds));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> usersPage = userRepository.findAll(spec, pageable);
        return usersPage.map(this::toAdminUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng với ID: " + id));
        return toAdminUserResponse(user);
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

        return toAdminUserResponse(savedUser);
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
        return toAdminUserResponse(updatedUser);
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
        return toAdminUserResponse(updatedUser);
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

    private AdminUserResponse toAdminUserResponse(User user) {
        List<String> roles = roleRepository.findRoleCodesByUserId(user.getId());
        String tenantName = tenantRepository.findById(user.getTenantId())
                .map(Tenant::getName)
                .orElse("Hệ thống EduPay");

        return AdminUserResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .tenantName(tenantName)
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .isOwner(user.getIsOwner())
                .roles(roles)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
