package com.edupay.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNavigationResponse {
    private UUID userId;
    private UUID tenantId;
    private String fullName;
    private String phone;
    private boolean isOwner;
    private List<String> roles;
    private Set<String> permissions;
    private Map<String, Boolean> features;
    private List<MenuItemResponse> menus;
}

