# IOPMS — System Architecture

## High-level design

```
Browser (static HTML/JS, served from src/main/resources/static)
   │  fetch + Bearer JWT (localStorage)
   ▼
SecurityConfig ── JwtAuthFilter (validates token, sets ROLE_ADMIN / ROLE_INTERN)
   ▼
Controllers: AuthController · ProfileController · AdminController · InternController
   │           routing + @Valid request validation
   ▼
Services: InternService · AttendanceService · TaskService · ReportService ·
          ProjectService · DashboardService · InternReportService
   │           business rules, @Transactional boundaries
   ▼
Repositories (Spring Data JPA, derived/JPQL queries)
   ▼
Supabase (PostgreSQL)  ·  H2 (dev profile & tests)
   ▼
GlobalExceptionHandler (validation → 400, business rules → 400/403/409, rest → 500)
```

## Layering rules

| Layer | Responsibility | Never does |
|-------|----------------|------------|
| Controller | HTTP routing, `@Valid`, status codes | business rules, direct DB access |
| Service | business rules, transactions, authorization context | HTTP concerns |
| Repository | queries | business logic |
| Frontend | rendering, forms, routing between tabs | computing hours/permissions — the backend is the source of truth |

## Package layout (package-per-feature)

```
com.valenlabs.iopms
  auth/        JwtService, JwtAuthFilter, SecurityConfig, login
  user/        User entity, profile self-edit, intern CRUD, SeedData (admin)
  attendance/  sessions, audit trail, attendance rules
  task/        allocation and progress tracking
  report/      daily work reports + review workflow
  project/     projects, interns M2M, versioned submissions
  admin/       AdminController, DashboardService, InternReportService + CSV
  intern/      InternController (self-scoped: user id always from the JWT)
  common/      ApiException, GlobalExceptionHandler, CurrentUser, shared enums
```

Conventions: records for DTOs, enums stored as `VARCHAR` in the DB, server-authoritative
timestamps and hour calculations, intern-scoped endpoints derive the user id from the
JWT — never from a request parameter.

## Data model (8 tables)

Full DDL: [`../schema-postgres.sql`](../schema-postgres.sql)

| Table | Purpose | Key constraints |
|-------|---------|-----------------|
| `users` | admins + interns, BCrypt hashes | unique email, `role` |
| `attendance` | one row per work session | **one open session per intern** (partial unique index), minutes computed by backend |
| `attendance_audit` | append-only log of admin corrections | reason mandatory, old/new values stored |
| `tasks` | assigned work items | status/progress; intern can't set COMPLETED |
| `daily_reports` | one report per intern per day | **unique (intern_id, date)** |
| `projects` | project definitions | supervisor, dates, status |
| `project_interns` | M2M assignment | — |
| `project_submission` | deliverable versions | version increments per (project, intern) |

## Key design decisions

1. **Stateless JWT auth** — no server sessions; token carries role + id, validated on every
   request by `JwtAuthFilter`. Logout is client-side (token discard).
2. **Attendance is not login** — check-in/check-out is a separate concept from
   authentication; interns have **no endpoint that can write timestamps** (server clock only).
3. **One open session** — enforced at service layer *and* by a partial unique index;
   end-without-start is rejected.
4. **Admin corrections always audited** — reason is mandatory, both old and new values are
   stored, the intern cannot edit history.
5. **Versioned submissions** — the DB assigns the next version number per (project, intern);
   review feedback is stored on the submission.
6. **Role checks at two layers** — URL matchers (`/api/admin/**` → ADMIN,
   `/api/intern/**` → INTERN) plus service-level ownership checks (intern can only touch
   *their own* rows).
7. **`ddl-auto=update` for the MVP** — schema bootstrap is automatic;
   `schema-postgres.sql` is the authoritative human-readable schema.

## Security

- BCrypt password hashes; passwords never leave the backend.
- JWT HS256, `JWT_SECRET` ≥ 32 bytes (fails fast if shorter).
- Deactivated accounts are rejected even with a valid token.
- Input validation (`@Valid` + jakarta constraints) on every request body.
- Errors: validation → 400, business-rule violations → 400/403/409, unexpected → 500
  (details in server log only, never leaked to the client).

## Environments

| Profile | Database | Use |
|---------|----------|-----|
| *(none)* | Supabase PostgreSQL via `DB_URL`/`DB_USER`/`DB_PASSWORD` | production / demo — see [`../run-supabase.ps1`](../run-supabase.ps1) |
| `dev` | embedded H2 file (`./data/iopms`) | zero-setup local development |
| *(tests)* | in-memory H2, random port | `mvn test` |
