package com.edupay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Thời gian hết hạn của Access Token (milliseconds từ epoch) */
    private long accessTokenExpiresAt;

    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID userId;
        private UUID tenantId;
        private String fullName;
        private String phone;
        private String email;
        private String avatarUrl;
        private Boolean isOwner;
        private List<String> roles;
        private String subscriptionPlan;
        private String subscriptionStatus;
    }
}
