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
import com.edupay.repository.RoleRepository;
import com.edupay.repository.UserRepository;
import com.edupay.security.JwtTokenProvider;
import com.edupay.security.SecuritySanitizer;
import com.edupay.security.TokenBlacklist;
import com.edupay.service.AuditLogService;
import com.edupay.service.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklist tokenBlacklist;
    private final AuditLogService auditLogService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${app.jwt.expiration-ms}")
    private long accessExpirationMs;

    // ─── Brute-force & Anti-timing attack config ─────────────────────────────
    private static final int    MAX_FAILED_ATTEMPTS = 5;
    private static final long   LOCKOUT_MINUTES     = 15L;
    // Hash BCrypt mẫu để chống Timing Attack (CWE-208) khi kiểm tra tài khoản không tồn tại
    private static final String DUMMY_BCRYPT_HASH   = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhiMkWb/g3e99J/K5n58q7aZ3dG31uQO";

    // ─── ĐĂNG KÝ ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Kiểm tra SĐT đã là chủ tài khoản (owner) chưa
        if (userRepository.existsOwnerByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Số điện thoại " + request.getPhone() + " đã được đăng ký");
        }

        // Làm sạch dữ liệu đầu vào chống Stored XSS
        String cleanFullName = SecuritySanitizer.sanitizePlainText(request.getFullName());
        String cleanCenterName = SecuritySanitizer.sanitizePlainText(request.getCenterName());
        if (cleanFullName == null || cleanFullName.isBlank()) {
            cleanFullName = request.getFullName().trim();
        }
        if (cleanCenterName == null || cleanCenterName.isBlank()) {
            cleanCenterName = request.getCenterName().trim();
        }

        // 2. Tạo Tenant mới bằng native SQL (để tránh tạo Tenant Entity phức tạp cho MVP)
        UUID tenantId = UUID.randomUUID();
        entityManager.createNativeQuery("""
                INSERT INTO tenants (id, name, phone, email, password_hash, subscription_plan, subscription_status, trial_ends_at, subscription_ends_at)
                VALUES (:id, :name, :phone, :email, :passwordHash, 'TRIAL', 'ACTIVE',
                        NOW() + INTERVAL '14 days', NOW() + INTERVAL '14 days')
                """)
                .setParameter("id", tenantId)
                .setParameter("name", cleanCenterName)
                .setParameter("phone", request.getPhone())
                .setParameter("email", null)
                .setParameter("passwordHash", passwordEncoder.encode(request.getPassword()))
                .executeUpdate();

        // 3. Tạo User owner
        // Note: tenantId là field của BaseEntity, Lombok @Builder không kế thừa → dùng setter
        User owner = User.builder()
                .phone(request.getPhone())
                .fullName(cleanFullName)
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
        auditLogService.logInfo(tenantId, ownerId, "USER_REGISTERED", "USER", ownerId.toString(), null, null);

        // 5. Phát hành token
        return buildAuthResponse(owner, tenantId, List.of("OWNER"), "TRIAL", "ACTIVE", "SOLO_TUTOR");
    }

    // ─── ĐĂNG NHẬP ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Tìm user theo SĐT (kèm cơ chế chống Timing Attack / User Enumeration)
        User user = userRepository.findFirstByPhoneOrderByCreatedAtDesc(request.getPhone())
                .orElse(null);

        if (user == null) {
            // Chống timing attack (CWE-208): chạy BCrypt verification với dummy hash
            // để thời gian phản hồi giữa SĐT có tồn tại và không tồn tại luôn bằng nhau (~80-100ms)
            passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH);
            throw new BusinessException("INVALID_CREDENTIALS", "Số điện thoại hoặc mật khẩu không đúng");
        }

        // 2. Kiểm tra tài khoản có đang bị khóa do brute-force không
        if (user.getLockedUntil() != null) {
            if (Instant.now().isBefore(user.getLockedUntil())) {
                long remainingMinutes = ChronoUnit.MINUTES.between(Instant.now(), user.getLockedUntil()) + 1;
                log.warn("Tài khoản bị khóa do brute-force: phone={}, lockedUntil={}", request.getPhone(), user.getLockedUntil());
                throw new BusinessException("ACCOUNT_LOCKED",
                        "Tài khoản tạm thời bị khóa do đăng nhập sai quá nhiều lần. " +
                        "Vui lòng thử lại sau " + remainingMinutes + " phút.");
            } else {
                // Thời gian khóa đã hết hạn -> mở khóa và cấp lại 5 lượt thử mới
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                log.info("Thời gian khóa brute-force đã hết hạn, tự động mở khóa cho phone={}", request.getPhone());
            }
        }

        // 3. Kiểm tra tài khoản còn hoạt động
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("ACCOUNT_SUSPENDED", "Tài khoản đã bị tạm khóa, vui lòng liên hệ hỗ trợ");
        }

        // 4. Xác thực mật khẩu — brute-force tracking
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, request.getPhone());
        }

        // 5. Đăng nhập thành công — reset brute-force counter
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // 6. Lấy subscription info của tenant
        Object[] tenantInfo = getTenantInfo(user.getTenantId());
        String subscriptionPlan   = (String) tenantInfo[0];
        String subscriptionStatus = (String) tenantInfo[1];
        String tenantType         = tenantInfo.length > 2 && tenantInfo[2] != null ? (String) tenantInfo[2] : "SOLO_TUTOR";

        // 7. Phát hành token
        log.info("Đăng nhập thành công — phone={}, userId={}", request.getPhone(), user.getId());
        auditLogService.logInfo(user.getTenantId(), user.getId(), "LOGIN_SUCCESS", "USER", user.getId().toString(), null, null);
        List<String> roles = getUserRoles(user);
        return buildAuthResponse(user, user.getTenantId(), roles, subscriptionPlan, subscriptionStatus, tenantType);
    }

    // ─── REFRESH TOKEN ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // 1. Tìm refresh token trong DB
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token không hợp lệ"));

        // 2. Kiểm tra còn hợp lệ — phát hiện Refresh Token Replay Attack (RFC 6749)
        if (!stored.isValid()) {
            if (stored.isRevoked()) {
                log.warn("CẢNH BÁO BẢO MẬT: Phát hiện sử dụng lại Refresh Token đã thu hồi (Token Replay Attack)! userId={}",
                        stored.getUser().getId());
                auditLogService.logWarn(stored.getTenantId(), stored.getUser().getId(),
                        "TOKEN_REPLAY_ATTACK_DETECTED", "REFRESH_TOKEN", stored.getId().toString(), null, null);
                // Thu hồi toàn bộ refresh token còn lại của user để bảo vệ tài khoản khỏi kẻ tấn công
                refreshTokenRepository.revokeAllByUserId(stored.getUser().getId(), Instant.now());
            }
            throw new BusinessException("REFRESH_TOKEN_EXPIRED", "Refresh token đã hết hạn hoặc bị thu hồi, vui lòng đăng nhập lại");
        }

        // 3. Revoke token cũ (rotation — bảo mật hơn)
        stored.setRevoked(true);
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        // 4. Phát hành Access Token + Refresh Token mới
        User user = stored.getUser();
        Object[] tenantInfo = getTenantInfo(stored.getTenantId());
        List<String> roles = getUserRoles(user);
        String tenantType = tenantInfo.length > 2 && tenantInfo[2] != null ? (String) tenantInfo[2] : "SOLO_TUTOR";
        return buildAuthResponse(user, stored.getTenantId(), roles,
                (String) tenantInfo[0], (String) tenantInfo[1], tenantType);
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
    public void logoutAll(String accessTokenHeader) {
        UUID userId = getUserIdFromContext();

        // Thu hồi tất cả refresh tokens trong DB
        int revoked = refreshTokenRepository.revokeAllByUserId(userId, Instant.now());

        // Blacklist access token hiện tại để không thể tiếp tục dùng
        if (StringUtils.hasText(accessTokenHeader) && accessTokenHeader.startsWith("Bearer ")) {
            String accessToken = accessTokenHeader.substring(7);
            try {
                Claims claims = jwtTokenProvider.validateAndGetClaims(accessToken);
                String jti = jwtTokenProvider.getJti(claims);
                Instant expiry = jwtTokenProvider.getExpiry(claims);
                if (jti != null) {
                    tokenBlacklist.blacklist(jti, expiry);
                }
            } catch (Exception e) {
                log.warn("Không thể blacklist access token khi logout-all: {}", e.getMessage());
            }
        }

        log.info("Đăng xuất tất cả thiết bị — userId={}, refreshTokensRevoked={}", userId, revoked);
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    /**
     * Xử lý đăng nhập thất bại: tăng failedLoginAttempts, thông báo số lượt thử còn lại,
     * khóa tài khoản nếu vượt ngưỡng 5 lần.
     */
    private void handleFailedLogin(User user, String phone) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
            userRepository.save(user);
            log.warn("Tài khoản bị khóa do brute-force: phone={}, attempts={}, lockedUntil={}",
                    phone, attempts, user.getLockedUntil());
            auditLogService.logWarn(user.getTenantId(), user.getId(), "ACCOUNT_LOCKED_BRUTE_FORCE", "USER", user.getId().toString(), null, null);
            throw new BusinessException("ACCOUNT_LOCKED",
                    "Bạn đã nhập sai mật khẩu 5 lần liên tiếp. Tài khoản bị tạm khóa 15 phút để bảo vệ an toàn.");
        } else {
            userRepository.save(user);
            int remaining = MAX_FAILED_ATTEMPTS - attempts;
            log.warn("Đăng nhập sai mật khẩu: phone={}, attempts={}/{}", phone, attempts, MAX_FAILED_ATTEMPTS);
            auditLogService.logWarn(user.getTenantId(), user.getId(), "LOGIN_FAILED", "USER", user.getId().toString(), null, null);

            String message;
            if (remaining <= 2) {
                message = "Số điện thoại hoặc mật khẩu không đúng. Cảnh báo: Bạn còn " + remaining + " lần thử trước khi tài khoản bị khóa 15 phút.";
            } else {
                message = "Số điện thoại hoặc mật khẩu không đúng";
            }
            throw new BusinessException("INVALID_CREDENTIALS", message);
        }
    }

    private AuthResponse buildAuthResponse(User user, UUID tenantId, List<String> roles,
                                           String subscriptionPlan, String subscriptionStatus, String tenantType) {
        // Tạo Access Token (bao gồm JTI)
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
                        .tenantType(tenantType != null ? tenantType : "SOLO_TUTOR")
                        .build())
                .build();
    }

    private List<String> getUserRoles(User user) {
        List<String> roleCodes = roleRepository.findRoleCodesByUserIdAndTenantId(user.getId(), user.getTenantId());
        if (roleCodes == null || roleCodes.isEmpty()) {
            if (Boolean.TRUE.equals(user.getIsOwner())) {
                return List.of("OWNER");
            }
            return List.of("VIEWER");
        }
        return roleCodes;
    }

    @SuppressWarnings("unchecked")
    private Object[] getTenantInfo(UUID tenantId) {
        Object result = entityManager.createNativeQuery(
                        "SELECT subscription_plan, subscription_status, COALESCE(tenant_type, 'SOLO_TUTOR') FROM tenants WHERE id = :id")
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
