package com.edupay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuDto {
    private UUID id;
    private String code;
    private String name;
    private String icon;
    private String route;
    private UUID parentId;
    private Integer sortOrder;
    private String featureId;
    private Boolean isActive;
    private Instant createdAt;
}

