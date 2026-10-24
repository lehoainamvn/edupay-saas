---
name: edupay-workflow
description: Workflow and technical guidelines for developing EduPay SaaS (Java Spring Boot, Angular, PostgreSQL, Multi-tenant, VietQR)
---

# EduPay SaaS Developer & Agent Workflow

Use this skill whenever you are implementing tasks, writing code, or creating Pull Requests for EduPay SaaS.

## 1. Branching Strategy
- Main branch: `main` (Production only)
- Integration branch: `develop` (Sprint testing)
- Feature branch: `feature/sprint<N>/<TASK_ID>-<task_name>`

```bash
git checkout develop
git pull origin develop
git checkout -b feature/sprint1/TASK05-jwt-tenant-auth
```

## 2. Multi-Tenant Backend Pattern (Java)
```java
// Always inherit BaseEntity
@Entity
@Table(name = "classes")
public class ClassEntity extends BaseEntity {
    // inherits: id, tenantId, createdAt, updatedAt
    private String name;
}

// In Service layer:
UUID tenantId = TenantContext.getTenantId();
List<ClassEntity> classes = classRepository.findByTenantId(tenantId);
```

## 3. Angular Mobile-First Pattern
- Ensure touch targets are at least 48px height.
- Responsive breakpoints: Mobile (`< 640px`) is primary.
- Always handle loading states and clear error feedback.
