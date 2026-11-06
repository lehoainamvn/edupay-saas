package com.edupay.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory JWT Access Token Blacklist.
 * Lưu JTI (JWT ID) của các token đã bị thu hồi (logout/logout-all).
 *
 * Cơ chế: JTI → thời điểm hết hạn token gốc.
 * Scheduled cleanup mỗi 5 phút để loại bỏ các entry đã hết hạn.
 *
 * MVP: In-memory (single instance). Production multi-instance cần Redis.
 */
@Slf4j
@Component
public class TokenBlacklist {

    // jti → expiry time của token (để tự động cleanup)
    private final ConcurrentHashMap<String, Instant> blacklistedJtis = new ConcurrentHashMap<>();

    /**
     * Thêm JTI vào blacklist kèm thời điểm hết hạn.
     * Token sẽ bị từ chối cho đến khi hết hạn, sau đó được cleanup.
     */
    public void blacklist(String jti, Instant expiry) {
        if (jti == null || jti.isBlank()) return;
        blacklistedJtis.put(jti, expiry);
        log.debug("JWT blacklisted: jti={}, expiry={}", jti, expiry);
    }

    /**
     * Kiểm tra JTI có trong blacklist không.
     * Token hết hạn vẫn được giữ cho đến khi cleanup chạy.
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) return false;
        return blacklistedJtis.containsKey(jti);
    }

    /**
     * Xóa các JTI đã hết hạn — chạy mỗi 5 phút.
     * Token hết hạn không còn được chấp nhận bởi JwtTokenProvider,
     * nên không cần giữ trong blacklist.
     */
    @Scheduled(fixedRate = 300_000L) // 5 phút
    public void cleanupExpired() {
        Instant now = Instant.now();
        int beforeSize = blacklistedJtis.size();
        blacklistedJtis.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        int removed = beforeSize - blacklistedJtis.size();
        if (removed > 0) {
            log.debug("TokenBlacklist cleanup: removed {} expired entries, remaining={}", removed, blacklistedJtis.size());
        }
    }

    /** Trả về số lượng entry hiện tại — dùng cho monitoring/test */
    public int size() {
        return blacklistedJtis.size();
    }
}

