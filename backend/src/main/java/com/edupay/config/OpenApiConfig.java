package com.edupay.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🎓 EduPay SaaS API Documentation")
                        .description("Hệ thống RESTful API cho nền tảng quản lý học sinh, điểm danh 1 chạm & đối soát học phí VietQR tự động theo mô hình Multi-Tenant.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("EduPay Technical Team")
                                .email("dev@edupay.vn")
                                .url("https://edupay.vn"))
                        .license(new License()
                                .name("Commercial License")
                                .url("https://edupay.vn/terms")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập token JWT dạng: Bearer {token}")));
    }
}
