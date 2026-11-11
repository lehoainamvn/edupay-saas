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
public class PermissionGroupResponse {
    private String resource; // e.g. "student", "class", "attendance", "invoice", "user", "system"
    private String resourceName; // e.g. "Quản lý Học sinh", "Quản lý Lớp học", etc.
    private List<PermissionItemResponse> permissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionItemResponse {
        private UUID id;
        private String code;
        private String name;
        private String action; // read, create, update, delete
    }
}
