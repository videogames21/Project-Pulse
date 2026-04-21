# Project Pulse — Claude Code Instructions

## Project Overview
Project Pulse is a full-stack web app that replaces manual Google Sheets WARs and Excel peer evaluations for TCU Senior Design students. It supports three user roles: **Admin**, **Instructor**, and **Student**.

## Team
| Member | Owns |
|---|---|
| **Harlem** | Sections and Rubic managment (UC - 1, 2, 3, 4, 5, 6, 22, 23, 24) |
| **Angel** | Team and Instructor managment (UC - 7, 8, 9, 10, 14, 18, 19, 20, 21) |
| **Grayson** | Student managment and accounts (UC - 11, 12, 15, 16, 17, 25, 26, 30) |
| **Lid** | WARs, Peer Evals, and Reports (UC - 13, 27, 28, 29, 31, 32, 33, 34) |

## Source of Truth (READ FIRST)

Before making design or implementation decisions, always review:

1. `/requirements/vision_scope.md`
2. `/requirements/use_cases.md`
3. `/requirements/project_glossary.md`

Do not invent features not supported by requirements unless clearly marked as enhancements.

## Tech Stack
- **Frontend:** Vue.js + Vite + Vuetify — do NOT use ElementPlus
- **Backend:** Spring Boot 4.x (Java), REST API
- **Database:** MySQL (or PostgreSQL)
- **CI/CD:** GitHub Actions
- **Deployment:** Microsoft Azure
- **Auth:** JWT (role-based: ADMIN, INSTRUCTOR, STUDENT)

## Project Structure
```
project-pulse/
├── backend/          # Spring Boot project (Maven)
│   └── src/main/java/com/projectpulse/
│       ├── auth/
│       ├── section/
│       ├── team/
│       ├── user/
│       ├── war/
│       ├── peerevaluation/
│       ├── rubric/
│       └── report/
├── frontend/         # Vue.js + Vuetify project
│   └── src/
│       ├── views/
│       ├── components/
│       ├── stores/       # Pinia stores
│       ├── router/
│       └── api/          # Axios API calls
├── output/           # Planning docs (team-plan, schema, api-contracts)
└── CLAUDE.md
```

Use domain-oriented modules with internal layers.

Each module should contain:

- controller
- service
- repository
- domain
- dto

Rules:

- Organize by business/domain first
- Keep business logic out of controllers
- Keep persistence logic inside repositories
- Minimize cross-module coupling
- Do not introduce global layer folders (e.g., no top-level controller/)

## Common Commands
```bash
# Backend
cd backend
mvn spring-boot:run          # Start backend (port 8080)
mvn test                     # Run tests
mvn clean package            # Build JAR for deployment

# Frontend
cd frontend
npm run dev                  # Start dev server (port 3000)
npm run build                # Production build
npm run lint                 # Lint check
```

## REST API Conventions
- Base URL: `/api/v1`
- Plural nouns, lowercase, hyphenated: `/peer-evaluations`, `/active-weeks`
- No verbs in URLs — use HTTP methods instead
- All responses use this envelope:
```json
{ "success": true, "data": { ... }, "message": "", "error": null }
```
- See `output/api-contracts.md` for all endpoints and ownership

## Database Conventions
- Table names: plural snake_case (`peer_evaluations`, `active_weeks`)
- Primary keys: `id bigint auto_increment`
- Foreign keys: `<table_singular>_id` (e.g., `section_id`, `team_id`)
- Timestamps: `created_at`, use `LocalDate` for week dates (always a Monday)
- `score` and `max_score` fields: use `BigDecimal`, never `float`/`double`
- See `output/database-schema.dbml` for the full schema

## Coding Conventions
**Java / Spring Boot:**
- Package per feature (not per layer): `com.projectpulse.section`, `com.projectpulse.war`
- Class naming: `SectionController`, `SectionService`, `SectionRepository`, `SectionEntity`
- DTOs for request/response — never expose entity classes directly to the API
- Use `@Valid` on all request bodies; return `400` on validation failure

**Vue.js / Vuetify:**
- Use Composition API (`<script setup>`) — not Options API
- Pinia for state management
- Axios for all HTTP calls, centralized in `src/api/`
- Component names: PascalCase (`TeamCard.vue`, `WAREntryForm.vue`)

## Git Workflow
```
main        ← production-ready only, protected (PR required)
feature/<name>/<short-desc>   ← personal feature branches, merge directly to main

# Example
git checkout main
git checkout -b feature/harlemmariscal/auth-jwt
# ... work ...
```
- Commit often under your own name (individual contribution is graded)
- Never commit directly to `main`

## Key Business Rules
- A week always starts on Monday — use Monday's date as the week identifier
- One WAR per student per week (enforced by DB unique constraint)
- One peer evaluation per evaluator-evaluatee pair per week
- Editing a rubric during section setup creates a copy — the original is preserved
- A student must accept a section invite before being assigned to a team
- Admin is the only role that can manage sections, teams, and users

## Do Not
- Do not use ElementPlus — Vuetify only
- Do not expose JPA entities directly in API responses — use DTOs
- Do not store scores as `float`/`double` — use `BigDecimal`
- Do not guess on unclear requirements — ask Dr. Wei directly

