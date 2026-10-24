# 🤖 EduPay SaaS — Instructions & Skill Registry for AI Agents

> This file is automatically loaded by AI coding assistants (Antigravity, Cursor, Windsurf, Copilot) when working in this workspace.

## 📌 Ground Rules & Constraints
Please strictly follow the rules defined in [.agents/rules/ai-agent-rules.md](.agents/rules/ai-agent-rules.md):
1. **Multi-Tenant Isolation (P0):** Never accept `tenant_id` from client payloads. Always extract from `TenantContext.getTenantId()`. Every business entity must inherit `BaseEntity`. Every repository query must filter by `tenant_id`.
2. **Layered Architecture:** Controller (HTTP & validation only) -> Service (100% business logic, `@Transactional`) -> Repository -> Database. Never expose JPA Entities directly; use DTOs and MapStruct.
3. **Mobile-First UX:** Touch targets must be >= 48px height. Optimistic UI updates for attendance (Present: Green, Excused: Yellow, Absent: Red). Never use wide horizontal tables on `< 640px` screens.
4. **VietQR Specifications:** Public invoice link `/p/:hash` must never require login. Transfer codes must be alphanumeric uppercase without diacritics. Webhook must handle idempotency.
5. **Flyway Migrations:** Never edit existing committed migrations (`V1`, `V2`). Always append new ones (`V3__...`).

## 🧠 Project Specialized Skills
Detailed developer and agent cheatsheets are located in `.agents/skills/`:
- [`edupay-workflow`](.agents/skills/edupay-workflow/SKILL.md): Git branching (`feature/sprint<N>/<task>`), pull request standards.
- [`angular-mobile-ui`](.agents/skills/angular-mobile-ui/SKILL.md): Angular 17+ Standalone Components, Signals, Touch UX, Reactive Forms.
- [`spring-boot-multitenant`](.agents/skills/spring-boot-multitenant/SKILL.md): Spring Boot 3 Multi-Tenant pattern, JPA repository, Service transactions.
- [`vietqr-payment-reconciliation`](.agents/skills/vietqr-payment-reconciliation/SKILL.md): Dynamic VietQR Napas247, banking webhook handling, auto-reconciliation.
- [`edupay-testing-quality`](.agents/skills/edupay-testing-quality/SKILL.md): JUnit 5, Mockito service testing, WebMvcTest, Angular Jasmine tests.
