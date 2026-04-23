# Database Design

## Current Setup (Development Only)

The app currently uses an **in-memory H2 database** for local development. This resets on every restart and requires zero setup, making it convenient during development.

> **This is not acceptable for deployment.** See the technical requirement in `requirements/project_pulse.md`.

## Production Requirement

Per course spec, the deployed app **must use MySQL or PostgreSQL** on Microsoft Azure. H2 may only be used locally.

## Migration Checklist (Before Deployment)

- [ ] `application.yaml` — replace H2 datasource with MySQL/PostgreSQL connection string
- [ ] `pom.xml` — replace H2 driver with MySQL (`mysql-connector-j`) or PostgreSQL (`postgresql`) driver
- [ ] `data.sql` — audit for H2-specific syntax (H2 is lenient; MySQL/PostgreSQL are stricter)
- [ ] Azure — provision a database instance
- [ ] Credentials — inject via environment variables or Azure Key Vault, never hardcoded

## Ownership

This is an **unassigned shared infrastructure task** — not covered by any individual use case. Raise with Dr. Wei to confirm who owns it before deployment.

## Conventions

- Table names: plural snake_case (`peer_evaluations`, `active_weeks`)
- Primary keys: `id bigint auto_increment`
- Foreign keys: `<table_singular>_id` (e.g., `section_id`, `team_id`)
- Timestamps: `created_at`, use `LocalDate` for week dates (always a Monday)
- `score` and `max_score` fields: use `BigDecimal`, never `float`/`double`
