# Project Pulse

A web-based student performance tracking system for the TCU Department of Computer Science Senior Design course sequence (COSC 40943/40993). It replaces the manual Google Sheets / Excel workflow for Weekly Activity Reports (WARs) and peer evaluations with an automated, unified platform.

## What it does

**Three user roles:**

- **Students** — log weekly activities (WARs), submit peer evaluations for teammates, and view their own evaluation report
- **Instructors** — generate peer evaluation reports for their section and WAR reports for teams
- **Admins** — manage sections, teams, evaluation rubrics, and invite users via email

**Business goals:**
- Reduce instructor grading time for peer evaluations by 50%
- Increase WAR and peer evaluation submission rates by 20%
- Reduce student time to complete WARs and peer evaluations by 25%

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3, Vite 5 |
| Backend | Java 25, Spring Boot 4 |
| ORM | Spring Data JPA |
| Auth | Spring Security |
| Database (dev) | H2 (in-memory) |
| Database (prod) | MySQL |
| Email | Gmail API |
| Hosting | Microsoft Azure |
| Build tool | Maven |

## Project structure

```
Project-Pulse/
├── CLAUDE.md                        Claude Code guide and architecture rules
├── requirements/                    Source-of-truth spec documents
│   ├── 1 Project Glossary.md
│   ├── 2 Vision and Scope.md
│   └── 3 Use Cases.md               34 use cases across all three roles
├── docs/                            Architecture, coding standards, ADRs
├── backend/                         Spring Boot application
│   ├── pom.xml
│   └── src/main/java/edu/tcu/cs/projectpulse/
└── frontend/                        Vue 3 + Vite application
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js
        ├── App.vue
        ├── assets/main.css
        ├── router/index.js          Vue Router (role-based guards)
        ├── stores/auth.js           Pinia auth store
        ├── services/api.js          API service layer
        ├── data/mockData.js         Seed data (replace with API calls)
        ├── components/AppLayout.vue Shared sidebar layout
        └── features/
            ├── auth/LoginView.vue
            ├── war/WARView.vue
            ├── peer-eval/PeerEvalView.vue
            ├── report/MyReportView.vue
            ├── instructor/SectionReportView.vue
            ├── instructor/TeamWARView.vue
            └── admin/{Sections,Teams,Rubrics,Invitations}View.vue
```

## Getting started

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Runs on `http://localhost:8080`. The H2 console is available at `/h2-console` in development.

## Frontend screens

| Role | Screen | Description |
|---|---|---|
| Student | Weekly Activity Report | Add, edit, and delete activity entries with category, hours, and status |
| Student | Peer Evaluation | Score each teammate against rubric criteria; add public and private comments |
| Student | My Report | View your own averaged peer eval scores and public comments |
| Instructor | Section Peer Eval Report | Section-wide scores, submission status, non-submitter flags |
| Instructor | Team WAR Report | Per-student activity counts, planned vs. actual hours, variance |
| Admin | Sections | Create and manage academic year sections |
| Admin | Teams | Create and manage project teams |
| Admin | Rubrics | Define evaluation rubrics with scored criteria |
| Admin | Invitations | Send email invitations to students and instructors |

## Key business rules

- **BR-1** — Every team must have at least one instructor
- **BR-2** — Active weeks: Fall weeks 5-15, Spring weeks 1-15. Peer evals only open during active weeks; WARs can be submitted anytime
- **BR-3** — Peer evaluations cannot be edited after submission
- **BR-4** — Students can only submit a peer eval for the previous week; no make-ups
- **BR-5** — Students can only see their own scores, public comments, and overall grade — not private comments or evaluator identities
