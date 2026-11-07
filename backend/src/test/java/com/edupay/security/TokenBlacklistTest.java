package com.edupay.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenBlacklist - Quản lý thu hồi Access Token")
class TokenBlacklistTest {

    private TokenBlacklist tokenBlacklist;

    @BeforeEach
    void setUp() {
        tokenBlacklist = new TokenBlacklist();
    }

    @Test
    @DisplayName("Token chưa bị blacklist trả về false")
    void isBlacklisted_NonBlacklistedToken_ReturnsFalse() {
        assertFalse(tokenBlacklist.isBlacklisted("some-random-jti"));
    }

    @Test
    @DisplayName("Thêm JTI vào blacklist -> isBlacklisted trả về true")
    void blacklist_ValidJti_ReturnsTrue() {
        String jti = "jwt-id-12345";
        Instant expiry = Instant.now().plus(1, ChronoUnit.HOURS);

        tokenBlacklist.blacklist(jti, expiry);

        assertTrue(tokenBlacklist.isBlacklisted(jti));
        assertEquals(1, tokenBlacklist.size());
    }

    @Test
    @DisplayName("Bỏ qua JTI rỗng hoặc null")
    void blacklist_NullOrBlankJti_Ignored() {
        tokenBlacklist.blacklist(null, Instant.now().plus(1, ChronoUnit.HOURS));
        tokenBlacklist.blacklist("   ", Instant.now().plus(1, ChronoUnit.HOURS));

        assertEquals(0, tokenBlacklist.size());
        assertFalse(tokenBlacklist.isBlacklisted(null));
        assertFalse(tokenBlacklist.isBlacklisted("   "));
    }

    @Test
    @DisplayName("Cleanup tự động loại bỏ các JTI đã hết hạn")
    void cleanupExpired_RemovesExpiredTokens() {
        String expiredJti = "expired-jti";
        Instant pastExpiry = Instant.now().minus(10, ChronoUnit.MINUTES);

        String activeJti = "active-jti";
        Instant futureExpiry = Instant.now().plus(1, ChronoUnit.HOURS);

        tokenBlacklist.blacklist(expiredJti, pastExpiry);
        tokenBlacklist.blacklist(activeJti, futureExpiry);

        assertEquals(2, tokenBlacklist.size());

        // Chạy cleanup
        tokenBlacklist.cleanupExpired();

        assertEquals(1, tokenBlacklist.size());
        assertFalse(tokenBlacklist.isBlacklisted(expiredJti));
        assertTrue(tokenBlacklist.isBlacklisted(activeJti));
    }
}

