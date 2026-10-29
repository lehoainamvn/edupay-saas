package com.edupay.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Sinh và xác thực JWT Access Token.
 * Payload: sub=userId, tenantId, roles
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long accessExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(secret.getBytes())
        ));
        this.accessExpirationMs = accessExpirationMs;
    }

    /** Sinh Access Token chứa userId, tenantId, roles */
    public String generateAccessToken(UUID userId, UUID tenantId, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /** Xác thực token và trả về Claims nếu hợp lệ */
    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Lấy userId từ token (đã validate) */
    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    /** Lấy tenantId từ token (đã validate) */
    public UUID getTenantId(Claims claims) {
        return UUID.fromString(claims.get("tenantId", String.class));
    }

    /** Lấy danh sách roles từ token */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims claims) {
        return (List<String>) claims.get("roles", List.class);
    }

    /** Kiểm tra token có hợp lệ không (không ném exception) */
    public boolean isTokenValid(String token) {
        try {
            validateAndGetClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token đã hết hạn: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT token không hợp lệ: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT exception: {}", e.getMessage());
        }
        return false;
    }
}
