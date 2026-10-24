# EduPay SaaS Development Guidelines for AI & Developers

When developing features for EduPay SaaS, strictly follow these constraints:

1. **Multi-Tenant Isolation (P0 Priority)**:
   - Every database table related to business logic must have `tenant_id`.
   - Never accept `tenant_id` from client request bodies. Always retrieve it from `TenantContext.getTenantId()`.
   - Every SQL query or JPA specification must filter by `tenant_id`.

2. **Technology Stack**:
   - Backend: Java 17+, Spring Boot 3.3, Spring Data JPA, Spring Security (JWT), PostgreSQL 16, Flyway migrations.
   - Frontend: Angular 17+ (standalone components, reactive forms, mobile-first design, TailwindCSS).

3. **Git Branching & Workflow**:
   - Never commit directly to `main` or `develop`.
   - Branch format: `feature/sprint<N>/<TASK_ID>-<task_name>`.
   - Commit format: `<type>(<scope>): <description>`.

4. **VietQR Specifications**:
   - Public invoice route `/p/:hash` must never require authentication.
   - QR code must use standard Napas247 URL format: `https://img.vietqr.io/image/{BIN}-{STK}-compact2.png?amount={AMOUNT}&addInfo={CODE}`.
