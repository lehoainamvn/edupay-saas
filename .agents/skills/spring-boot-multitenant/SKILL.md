---
name: spring-boot-multitenant
description: Comprehensive guidelines, architectural patterns, and code snippets for developing Spring Boot 3 Multi-Tenant services with PostgreSQL, Flyway, and ThreadLocal isolation in EduPay SaaS.
---

# ☕ Spring Boot 3 Multi-Tenant SaaS Development Skill

Use this skill whenever you are designing, coding, or refactoring Backend APIs, Services, JPA Entities, Repositories, or Security logic in EduPay SaaS.

---

## 1. Nguyên Tắc Cốt Lõi: Bảo Mật & Cô Lập Multi-Tenant (P0)

1. **Tuyệt đối không nhận `tenant_id` từ HTTP Request Body hoặc Query Param:**
   - Kẻ gian có thể giả mạo `tenant_id` của trung tâm khác.
   - Luôn luôn trích xuất danh tính Tenant từ JWT Token đã xác thực:
     ```java
     UUID tenantId = TenantContext.getTenantId();
     ```
2. **Kế thừa `BaseEntity` cho mọi bảng nghiệp vụ:**
   - Mọi Entity liên quan đến trung tâm/giáo viên (`Student`, `ClassEntity`, `AttendanceSession`, `Invoice`, v.v.) phải kế thừa `BaseEntity`.
3. **Repository phải luôn lọc theo `tenant_id`:**
   - Không dùng `findById(id)` đơn lẻ cho dữ liệu có chủ sở hữu.
   - Luôn dùng: `findByIdAndTenantId(UUID id, UUID tenantId)`.

---

## 2. Tiêu Chuẩn 4 Tầng (Layered Pattern)

```
[Client Request]
       ↓
[Controller]  ← Validate DTO (@Valid), trả về ResponseEntity<ApiResponse<T>>
       ↓
[Service]     ← 100% Logic nghiệp vụ, @Transactional, kiểm tra TenantContext
       ↓
[Repository]  ← Spring Data JPA, câu query luôn chứa tenantId
       ↓
[Database]
```

### 1. Template Entity Chuẩn
```java
package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students", indexes = {
    @Index(name = "idx_students_tenant", columnList = "tenant_id"),
    @Index(name = "idx_students_phone", columnList = "parent_phone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String parentPhone;

    @Column(length = 255)
    private String note;
}
```

### 2. Template Repository Chuẩn
```java
package com.edupay.repository;

import com.edupay.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Student> findAllByTenantId(UUID tenantId, Pageable pageable);

    boolean existsByTenantIdAndParentPhone(UUID tenantId, String parentPhone);
}
```

### 3. Template Service Chuẩn
```java
package com.edupay.service.impl;

import com.edupay.config.TenantContext;
import com.edupay.dto.request.CreateStudentRequest;
import com.edupay.dto.response.StudentResponse;
import com.edupay.entity.Student;
import com.edupay.exception.DuplicateResourceException;
import com.edupay.exception.ResourceNotFoundException;
import com.edupay.mapper.StudentMapper;
import com.edupay.repository.StudentRepository;
import com.edupay.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    @Override
    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        UUID tenantId = TenantContext.getTenantId();

        if (studentRepository.existsByTenantIdAndParentPhone(tenantId, request.getParentPhone())) {
            throw new DuplicateResourceException("Số điện thoại phụ huynh đã tồn tại trong danh sách lớp của bạn");
        }

        Student student = studentMapper.toEntity(request);
        student.setTenantId(tenantId);

        Student saved = studentRepository.save(student);
        log.info("Tenant {} created student id {}", tenantId, saved.getId());
        return studentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Student student = studentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học sinh với ID: " + id));
        return studentMapper.toResponse(student);
    }
}
```

### 4. Template Controller Chuẩn
```java
package com.edupay.controller;

import com.edupay.dto.ApiResponse;
import com.edupay.dto.request.CreateStudentRequest;
import com.edupay.dto.response.StudentResponse;
import com.edupay.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {
        StudentResponse data = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm học sinh thành công", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(@PathVariable UUID id) {
        StudentResponse data = studentService.getStudentById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin học sinh thành công", data));
    }
}
```

---

## 3. Quản Lý Flyway Migrations
- File migration đặt tại: `backend/src/main/resources/db/migration/`
- Tên file bắt buộc theo dạng: `V{version}__{description}.sql` (chú ý 2 dấu gạch dưới).
- Schema của mỗi bảng nghiệp vụ luôn có:
  ```sql
  tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
  ```
- Tạo index `idx_{table}_tenant` trên mọi cột `tenant_id`.
