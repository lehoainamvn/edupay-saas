package com.edupay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCenterRequest {

    @NotBlank(message = "Tên trung tâm không được để trống")
    private String name;

    private String email;

    private String subscriptionPlan;

    private String subscriptionStatus; // ACTIVE, SUSPENDED, EXPIRED

    private Instant subscriptionEndsAt;

    private String representativeName;
}

