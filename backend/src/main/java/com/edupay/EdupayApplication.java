package com.edupay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EdupayApplication {
    public static void main(String[] args) {
        SpringApplication.run(EdupayApplication.class, args);
    }
}
