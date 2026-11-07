package com.edupay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverrideFeatureRequest {

    @NotNull(message = "Trạng thái kích hoạt không được để trống")
    private Boolean isEnabled;

    private String reason;

    private Instant expiresAt;
}

