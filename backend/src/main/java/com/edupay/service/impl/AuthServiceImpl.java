package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.LoginRequest;
import com.edupay.dto.request.RefreshTokenRequest;
import com.edupay.dto.request.RegisterRequest;
import com.edupay.dto.response.AuthResponse;
import com.edupay.entity.RefreshToken;
import com.edupay.entity.User;
import com.edupay.exception.BusinessException;
import com.edupay.exception.DuplicateResourceException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.repository.RefreshTokenRepository;
import com.edupay.repository.UserRepository;
import com.edupay.security.JwtTokenProvider;
import com.edupay.service.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${app.jwt.expiration-ms}")
    private long accessExpirationMs;

    // ─── ĐĂNG KÝ ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Kiểm tra SĐT đã là chủ tài khoản (owner) chưa
        if (userRepository.existsOwnerByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Số điện thoại " + request.getPhone() + " đã được đăng ký");
        }

        // 2. Tạo Tenant mới bằng native SQL (để tránh tạo Tenant Entity phức tạp cho MVP)
        UUID tenantId = UUID.randomUUID();
        entityManager.createNativeQuery("""
                INSERT INTO tenants (id, name, phone, email, password_hash, subscription_plan, subscription_status, trial_ends_at, subscription_ends_at)
                VALUES (:id, :name, :phone, :email, :passwordHash, 'TRIAL', 'ACTIVE',
                        NOW() + INTERVAL '14 days', NOW() + INTERVAL '14 days')
                """)
                .setParameter("id", tenantId)
                .setParameter("name", request.getCenterName())
                .setParameter("phone", request.getPhone())
                .setParameter("email", null)
                .setParameter("passwordHash", passwordEncoder.encode(request.getPassword()))
                .executeUpdate();

        // 3. Tạo User owner
        // Note: tenantId là field của BaseEntity, Lombok @Builder không kế thừa → dùng setter
        User owner = User.builder()
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isOwner(true)
                .status("ACTIVE")
                .build();
        owner.setTenantId(tenantId);
        owner = userRepository.save(owner);

        // 4. Gán role OWNER cho user (native SQL để tránh tạo Role Entity)
        final UUID ownerId = owner.getId();
        entityManager.createNativeQuery("""
                INSERT INTO user_roles (tenant_id, user_id, role_id)
                SELECT :tenantId, :userId, r.id FROM roles r
                WHERE r.code = 'OWNER' AND r.tenant_id IS NULL
                ON CONFLICT (user_id, role_id) DO NOTHING
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("userId", ownerId)
                .executeUpdate();

        log.info("Đăng ký thành công — phone={}, tenantId={}, userId={}", request.getPhone(), tenantId, ownerId);

        // 5. Phát hành token
        return buildAuthResponse(owner, tenantId, List.of("OWNER"), "TRIAL", "ACTIVE");
    }

    // ─── ĐĂNG NHẬP ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Tìm user owner theo SĐT
        User user = userRepository.findByPhoneAndIsOwnerTrue(request.getPhone())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Số điện thoại hoặc mật khẩu không đúng"));

        // 2. Kiểm tra tài khoản còn hoạt động
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("ACCOUNT_SUSPENDED", "Tài khoản đã bị tạm khóa, vui lòng liên hệ hỗ trợ");
        }

        // 3. Xác thực mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Số điện thoại hoặc mật khẩu không đúng");
        }

        // 4. Cập nhật last_login_at
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // 5. Lấy subscription info của tenant
        Object[] tenantInfo = getTenantInfo(user.getTenantId());
        String subscriptionPlan   = (String) tenantInfo[0];
        String subscriptionStatus = (String) tenantInfo[1];

        // 6. Phát hành token
        log.info("Đăng nhập thành công — phone={}, userId={}", request.getPhone(), user.getId());
        return buildAuthResponse(user, user.getTenantId(), List.of("OWNER"), subscriptionPlan, subscriptionStatus);
    }

    // ─── REFRESH TOKEN ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // 1. Tìm refresh token trong DB
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token không hợp lệ"));

        // 2. Kiểm tra còn hợp lệ
        if (!stored.isValid()) {
            throw new BusinessException("REFRESH_TOKEN_EXPIRED", "Refresh token đã hết hạn hoặc bị thu hồi, vui lòng đăng nhập lại");
        }

        // 3. Revoke token cũ (rotation — bảo mật hơn)
        stored.setRevoked(true);
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        // 4. Phát hành Access Token + Refresh Token mới
        User user = stored.getUser();
        Object[] tenantInfo = getTenantInfo(stored.getTenantId());
        return buildAuthResponse(user, stored.getTenantId(), List.of("OWNER"),
                (String) tenantInfo[0], (String) tenantInfo[1]);
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
            log.info("Đăng xuất — userId={}", token.getUser().getId());
        });
    }

    @Override
    @Transactional
    public void logoutAll() {
        UUID userId = getUserIdFromContext();
        int revoked = refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        log.info("Đăng xuất tất cả thiết bị — userId={}, revoked={}", userId, revoked);
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user, UUID tenantId, List<String> roles,
                                           String subscriptionPlan, String subscriptionStatus) {
        // Tạo Access Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), tenantId, roles);
        long expiresAt = Instant.now().plusMillis(accessExpirationMs).toEpochMilli();

        // Tạo Refresh Token (UUID random, lưu DB)
        String rawRefreshToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tenantId(tenantId)
                .token(rawRefreshToken)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .accessTokenExpiresAt(expiresAt)
                .user(AuthResponse.UserInfo.builder()
                        .userId(user.getId())
                        .tenantId(tenantId)
                        .fullName(user.getFullName())
                        .phone(user.getPhone())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .isOwner(user.getIsOwner())
                        .roles(roles)
                        .subscriptionPlan(subscriptionPlan)
                        .subscriptionStatus(subscriptionStatus)
                        .build())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Object[] getTenantInfo(UUID tenantId) {
        Object result = entityManager.createNativeQuery(
                        "SELECT subscription_plan, subscription_status FROM tenants WHERE id = :id")
                .setParameter("id", tenantId)
                .getSingleResult();
        return (Object[]) result;
    }

    private UUID getUserIdFromContext() {
        // userId lấy từ SecurityContext (set bởi JwtAuthFilter)
        String principal = (String) org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return UUID.fromString(principal);
    }
}
