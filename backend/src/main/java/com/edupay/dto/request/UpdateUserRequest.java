package com.edupay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    private String email;

    private String status; // ACTIVE, SUSPENDED

    private List<String> roles;
}

