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
public class UpdateTutorRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    private String email;

    private String subscriptionPlan;

    private String subscriptionStatus;

    private Instant subscriptionEndsAt;
}

