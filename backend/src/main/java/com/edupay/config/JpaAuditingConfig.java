package com.edupay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Cấu hình JPA Auditing (tách riêng khỏi @SpringBootApplication để tránh lỗi JpaMappingContext trong @WebMvcTest).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}

