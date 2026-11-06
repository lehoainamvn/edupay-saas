package com.edupay.security;

import com.edupay.dto.request.LoginRequest;
import com.edupay.entity.User;
import com.edupay.exception.BusinessException;
import com.edupay.repository.RefreshTokenRepository;
import com.edupay.repository.UserRepository;
import com.edupay.service.AuditLogService;
import com.edupay.service.impl.AuthServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BruteForceProtection - Chống tấn công dò mật khẩu")
class BruteForceProtectionTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private final String phone = "0912345678";
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .phone(phone)
                .fullName("Nguyen Van A")
                .passwordHash("$2a$10$hashedPassword")
                .status("ACTIVE")
                .isOwner(true)
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build();
        testUser.setId(UUID.randomUUID());
        testUser.setTenantId(tenantId);

        ReflectionTestUtils.setField(authService, "accessExpirationMs", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 86400000L);
        ReflectionTestUtils.setField(authService, "entityManager", entityManager);
    }

    @Test
    @DisplayName("Sai mật khẩu lần đầu: tăng failedLoginAttempts lên 1, không khóa tài khoản")
    void login_WrongPassword_IncrementsFailedAttempts() {
        when(userRepository.findByPhoneAndIsOwnerTrue(phone)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong-password", testUser.getPasswordHash())).thenReturn(false);

        LoginRequest req = new LoginRequest(phone, "wrong-password");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals("INVALID_CREDENTIALS", ex.getErrorCode());
        assertEquals(1, testUser.getFailedLoginAttempts());
        assertNull(testUser.getLockedUntil());
        verify(userRepository).save(testUser);
        verify(auditLogService).logWarn(eq(tenantId), eq(testUser.getId()), eq("LOGIN_FAILED"), eq("USER"), any(), any(), any());
    }

    @Test
    @DisplayName("Sai mật khẩu lần thứ 5: khóa tài khoản trong 15 phút (lockedUntil)")
    void login_FifthWrongPassword_LocksAccount() {
        testUser.setFailedLoginAttempts(4); // Đã sai 4 lần trước đó
        when(userRepository.findByPhoneAndIsOwnerTrue(phone)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong-password", testUser.getPasswordHash())).thenReturn(false);

        LoginRequest req = new LoginRequest(phone, "wrong-password");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals("ACCOUNT_LOCKED", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("tạm khóa 15 phút"));
        assertEquals(5, testUser.getFailedLoginAttempts());
        assertNotNull(testUser.getLockedUntil());
        assertTrue(testUser.getLockedUntil().isAfter(Instant.now()));
        verify(auditLogService).logWarn(eq(tenantId), eq(testUser.getId()), eq("ACCOUNT_LOCKED_BRUTE_FORCE"), eq("USER"), any(), any(), any());
    }

    @Test
    @DisplayName("Cảnh báo số lượt thử còn lại khi sắp bị khóa (còn <= 2 lần thử)")
    void login_NearLockout_WarnsRemainingAttempts() {
        testUser.setFailedLoginAttempts(3); // Đã sai 3 lần, lần này là lần 4 -> còn 1 lần
        when(userRepository.findByPhoneAndIsOwnerTrue(phone)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong-password", testUser.getPasswordHash())).thenReturn(false);

        LoginRequest req = new LoginRequest(phone, "wrong-password");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals("INVALID_CREDENTIALS", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Bạn còn 1 lần thử"));
    }

    @Test
    @DisplayName("Thời gian khóa đã qua: tự động mở khóa và cho phép đăng nhập bình thường")
    void login_LockExpired_AutomaticallyUnlocksAndAllowsLogin() {
        // Khóa từ 20 phút trước (hết 15 phút khóa)
        testUser.setLockedUntil(Instant.now().minus(5, ChronoUnit.MINUTES));
        testUser.setFailedLoginAttempts(5);

        when(userRepository.findByPhoneAndIsOwnerTrue(phone)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correct-password", testUser.getPasswordHash())).thenReturn(true);

        var mockQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(new Object[]{"PRO", "ACTIVE"});

        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock-access-token");

        LoginRequest req = new LoginRequest(phone, "correct-password");
        authService.login(req);

        assertEquals(0, testUser.getFailedLoginAttempts());
        assertNull(testUser.getLockedUntil());
    }

    @Test
    @DisplayName("Tài khoản đang bị khóa: từ chối ngay lập tức với mã lỗi ACCOUNT_LOCKED")
    void login_AccountLocked_ThrowsAccountLockedException() {
        // Tài khoản bị khóa đến 10 phút sau
        testUser.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(userRepository.findByPhoneAndIsOwnerTrue(phone)).thenReturn(Optional.of(testUser));

        LoginRequest req = new LoginRequest(phone, "any-password");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals("ACCOUNT_LOCKED", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("khóa"));
        // Không kiểm tra password khi tài khoản đang bị khóa
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Đăng nhập đúng: reset failedLoginAttempts về 0 và xóa lockedUntil")
    void login_Success_ResetsFailedAttemptsAndLock() {
        testUser.setFailedLoginAttempts(3);
        when(userRepository.findByPhoneAndIsOwnerTrue(phone)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correct-password", testUser.getPasswordHash())).thenReturn(true);

        // Mock native query cho tenant subscription info
        var mockQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(new Object[]{"PRO", "ACTIVE"});

        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock-access-token");

        LoginRequest req = new LoginRequest(phone, "correct-password");
        authService.login(req);

        assertEquals(0, testUser.getFailedLoginAttempts());
        assertNull(testUser.getLockedUntil());
        assertNotNull(testUser.getLastLoginAt());
        verify(userRepository).save(testUser);
        verify(auditLogService).logInfo(eq(tenantId), eq(testUser.getId()), eq("LOGIN_SUCCESS"), eq("USER"), any(), any(), any());
    }

    @Test
    @DisplayName("Chống Timing Attack: SĐT không tồn tại vẫn thực hiện hash check để thời gian phản hồi đồng nhất")
    void login_UserNotFound_PerformsDummyHashCheck() {
        when(userRepository.findByPhoneAndIsOwnerTrue("0999999999")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest("0999999999", "random-password");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals("INVALID_CREDENTIALS", ex.getErrorCode());

        // Phải gọi passwordEncoder.matches với dummy hash để thời gian phản hồi ~80ms đồng nhất
        verify(passwordEncoder).matches(eq("random-password"), anyString());
    }

    @Test
    @DisplayName("Chống Token Replay Attack: Sử dụng lại refresh token đã thu hồi sẽ kích hoạt Family Revocation")
    void refreshToken_ReplayAttack_RevokesAllTokensAndAlerts() {
        com.edupay.entity.RefreshToken compromisedToken = com.edupay.entity.RefreshToken.builder()
                .token("stolen-revoked-token")
                .user(testUser)
                .tenantId(tenantId)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(true) // Đã bị thu hồi trước đó
                .revokedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        compromisedToken.setId(UUID.randomUUID());

        when(refreshTokenRepository.findByToken("stolen-revoked-token")).thenReturn(Optional.of(compromisedToken));

        com.edupay.dto.request.RefreshTokenRequest req = new com.edupay.dto.request.RefreshTokenRequest();
        req.setRefreshToken("stolen-revoked-token");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.refreshToken(req));
        assertEquals("REFRESH_TOKEN_EXPIRED", ex.getErrorCode());

        // Kích hoạt Family Revocation: Thu hồi toàn bộ refresh tokens còn lại của user
        verify(refreshTokenRepository).revokeAllByUserId(eq(testUser.getId()), any());
        verify(auditLogService).logWarn(eq(tenantId), eq(testUser.getId()), eq("TOKEN_REPLAY_ATTACK_DETECTED"), any(), any(), any(), any());
    }
}

