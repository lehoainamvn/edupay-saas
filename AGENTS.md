# 🤖 EduPay SaaS — Instructions & Skill Registry for AI Agents

> This file is automatically loaded by AI coding assistants (Antigravity, Cursor, Windsurf, Copilot) when working in this workspace.

## 📌 Ground Rules & Constraints
Please strictly follow the rules defined in [.agents/rules/ai-agent-rules.md](.agents/rules/ai-agent-rules.md):
1. **Multi-Tenant Isolation (P0):** Never accept `tenant_id` from client payloads. Always extract from `TenantContext.getTenantId()`. Every business entity must inherit `BaseEntity`. Every repository query must filter by `tenant_id`.
2. **Layered Architecture:** Controller (HTTP & validation only) -> Service (100% business logic, `@Transactional`) -> Repository -> Database. Never expose JPA Entities directly; use DTOs and MapStruct.
3. **Mobile-First UX:** Touch targets must be >= 48px height. Optimistic UI updates for attendance (Present: Green, Excused: Yellow, Absent: Red). Never use wide horizontal tables on `< 640px` screens.
4. **VietQR Specifications:** Public invoice link `/p/:hash` must never require login. Transfer codes must be alphanumeric uppercase without diacritics. Webhook must handle idempotency.
5. **Flyway Migrations:** Never edit existing committed migrations (`V1`, `V2`). Always append new ones (`V3__...`).
6. **Reuse-First & Anti-Duplication Law (P0):** Before creating ANY new UI component, navbar, modal, service, DTO, entity, or helper, Agent MUST check existing code in `shared/`, `core/`, and feature branches. If a component or logic already exists (e.g. `NavbarComponent`, `BaseEntity`, `ApiResponse`), Agent MUST REUSE IT. NEVER duplicate or re-invent what is already built.
7. **Anti-AI Cliché Icons & Clean UI Law (P0):** Never insert random emojis (`👋`, `📱`, `⚡`, `🧮`) or hallucinated AI SVG icons into headings, button labels, kicker badges, or the navbar. Follow Clean Typography First. Freeze the unified text-only navbar (`NavbarComponent`).
8. **Single Source of Truth for Routing (P0):** All Angular frontend routes must be declared exclusively in `src/app/app.routes.ts`. Never create fragmented, decentralized feature routing files.
9. **Real API & Empty State First (P0):** Never leave hardcoded mock data in production screens. Connect to backend database APIs and implement thoughtful, elegant empty states for zero-data conditions.

## 🧠 Project Specialized Skills
Detailed developer and agent cheatsheets are located in `.agents/skills/`:
- [`edupay-workflow`](.agents/skills/edupay-workflow/SKILL.md): Git branching (`feature/sprint<N>/<task>`), pull request standards.
- [`angular-mobile-ui`](.agents/skills/angular-mobile-ui/SKILL.md): Angular 17+ Standalone Components, Signals, Touch UX, Reactive Forms.
- [`spring-boot-multitenant`](.agents/skills/spring-boot-multitenant/SKILL.md): Spring Boot 3 Multi-Tenant pattern, JPA repository, Service transactions.
- [`vietqr-payment-reconciliation`](.agents/skills/vietqr-payment-reconciliation/SKILL.md): Dynamic VietQR Napas247, banking webhook handling, auto-reconciliation.
- [`edupay-testing-quality`](.agents/skills/edupay-testing-quality/SKILL.md): JUnit 5, Mockito service testing, WebMvcTest, Angular Jasmine tests.
