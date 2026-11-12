package com.edupay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMenuRequest {

    @NotBlank(message = "Mã menu không được để trống")
    private String code;

    @NotBlank(message = "Tên menu không được để trống")
    private String name;

    private String icon;

    @NotBlank(message = "Đường dẫn route không được để trống")
    private String route;

    private UUID parentId;

    @Builder.Default
    private Integer sortOrder = 0;

    private String featureId;

    @Builder.Default
    private Boolean isActive = true;
}

