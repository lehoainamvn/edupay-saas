package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health Check", description = "Kiểm tra trạng thái hoạt động của Backend API")
public class HealthController {

    @GetMapping
    @Operation(summary = "Kiểm tra sức khỏe hệ thống (Health Check)", description = "Trả về trạng thái hoạt động của Backend và thời gian hiện tại")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> status = Map.of(
                "status", "UP",
                "service", "EduPay SaaS Backend",
                "version", "1.0.0",
                "serverTime", Instant.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.ok(status, "Hệ thống EduPay Backend đang hoạt động ổn định"));
    }
}
