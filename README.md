# IOPMS — Intern Operations & Performance Management System

Internal web platform for Valen Labs to manage interns: attendance, working hours, task
allocation, daily reports, project submissions and admin reporting.

## Stack

| Layer      | Technology                                          |
|------------|-----------------------------------------------------|
| Backend    | Java 21, Spring Boot 3.4, Spring Data JPA, Spring Security (JWT), REST |
| Database   | Supabase (PostgreSQL); H2 for local dev/tests       |
| Frontend   | Plain HTML/CSS/JS, served by the backend             |
| API docs   | Swagger UI at `/swagger-ui.html`                     |
| Tools      | Maven, Git, Postman (`postman/IOPMS.postman_collection.json`) |

## Quick start (no database needed)

```powershell
# Windows PowerShell: keep the -D flag quoted, or PowerShell splits it on the dots
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"   # uses an embedded H2 file DB
```

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev     # macOS / Linux
```

Open http://localhost:8080 — log in with the seeded admin:

```
admin@valenlabs.com / Admin@123
```

Create an intern from the **Interns** tab, log out, log in as that intern.

## Supabase setup (production)

The Supabase **direct connection is IPv6-only**, which most local networks can't reach.
Use the **transaction pooler** instead (IPv4, port 6543, username `postgres.<project-ref>`).

1. Create a Supabase project (note its region, e.g. `ap-northeast-1`) and copy the
   **Transaction pooler** connection string from Dashboard → Connect.
2. Set environment variables and start (or just use the helper script):

```powershell
.\run-supabase.ps1     # prompts for the DB password, generates a JWT secret
```

```bash
# macOS / Linux equivalent
export DB_URL="jdbc:postgresql://aws-0-<region>.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0"
export DB_USER=postgres.<project-ref>
export DB_PASSWORD=<database-password>
export JWT_SECRET=<random-string-at-least-32-bytes>
mvn spring-boot:run     # no dev profile — that would switch back to H2
```

JPA (`ddl-auto=update`) creates the tables on first start and `SeedData` creates
`admin@valenlabs.com / Admin@123`. The full schema is documented in
[`schema-postgres.sql`](schema-postgres.sql) (note: Supabase is PostgreSQL; the schema is the
authoritative deliverable — MySQL is not used).

## Modules

| Module | Where | Highlights |
|--------|-------|-----------|
| A. Auth & users | `auth/`, `user/` | BCrypt hashing, JWT + role authorities, intern CRUD/deactivate, profile self-edit. Passwords never leave the backend (only hashes are stored). |
| B. Attendance | `attendance/` | Start/End buttons, server clock only, one open session at a time, backend-computed minutes, monthly totals, incomplete-session detection, admin corrections with mandatory reason + `attendance_audit` history. Interns have **no** endpoint that writes timestamps. |
| C. Tasks | `task/` | Admin creates/assigns with priority, dates, status, progress, remarks. Interns update progress; only admins may set COMPLETED/CHANGES_REQUESTED. |
| D. Daily reports | `report/` | One report per intern per date (DB unique index), submit timestamp, approve / request-changes with remarks, missing-report listing. |
| E. Projects | `project/` | Multi-intern assignment, supervisor, dates, status; deliverable submissions (repo URL + notes) with automatic version numbers and review feedback. |
| F. Dashboard | `admin/` | Team, attendance, task, reporting and project counters computed server-side. |
| G. Intern report | `admin/InternReportService` | One consolidated payload per intern (profile, attendance, tasks, reports, submissions, feedback) with month filter and CSV export. |

## Architecture

```
Browser (static HTML/JS, served from src/main/resources/static)
   │  fetch + Bearer JWT (localStorage)
   ▼
SecurityConfig ── JwtAuthFilter (validates token, sets ROLE_ADMIN / ROLE_INTERN)
   ▼
Controllers: AuthController · ProfileController · AdminController · InternController   (routing + @Valid)
   ▼
Services: InternService · AttendanceService · TaskService · ReportService ·
          ProjectService · DashboardService · InternReportService                     (business rules, @Transactional)
   ▼
Repositories (Spring Data JPA, derived/JPQL queries)  ──►  Supabase (PostgreSQL) / H2
   ▼
GlobalExceptionHandler (validation → 400, business rules → 400/403/409, rest → 500)
```

Conventions: package-per-feature, records for DTOs, enum-per-field in the DB
(`VARCHAR`), server-authoritative timestamps and hours, intern-scoped endpoints always
derive the user id from the JWT — never from a request parameter.

## Security

- Login/logout separated from check-in/check-out (attendance is its own concept).
- BCrypt password hashes; JWT signed with HS256 (`JWT_SECRET`, ≥ 32 bytes, fails fast if short).
- Role checks at two layers: URL matchers (`/api/admin/**` → ADMIN, `/api/intern/**` → INTERN) plus service-level ownership checks.
- Deactivated accounts are rejected even with a valid token.
- Input validation (`@Valid` + jakarta constraints) on every request body.

## Tests

```bash
mvn test
```

| Suite | Covers |
|-------|--------|
| `AttendanceServiceTest` (Mockito) | one-open-session rule, end-without-start, server-side hour computation, correction recompute + audit row, invalid correction rejected |
| `AppFlowTest` (full HTTP + H2) | admin login → create intern → role isolation (403/401) → start/end rules → audited correction (480 min) → task progress rules → one-report-per-day (409) → report review → versioned project submissions → dashboard numbers → consolidated report + CSV |

## API

- Swagger UI: http://localhost:8080/swagger-ui.html
- Postman collection: `postman/IOPMS.postman_collection.json` (collection variable `token` is
  filled automatically by the login requests).

## Repository structure

```
src/main/java/com/valenlabs/iopms/
  auth/       JWT service, security filter, login
  user/       User entity, profile + intern management, seeded admin
  attendance/ sessions, audit, business rules
  task/       task allocation and tracking
  report/     daily work reports
  project/    projects and versioned submissions
  admin/      admin controller, dashboard, consolidated report + CSV
  intern/     intern controller (self-scoped endpoints)
  common/     errors, current user, shared enums, helpers
src/main/resources/static/   index.html (login), admin.html, intern.html, style.css, *.js
```

## Deliverables checklist

- [x] Working backend (all 7 modules) and frontend
- [x] `schema-postgres.sql` — database schema
- [x] README with setup instructions
- [x] Postman API collection
- [x] Swagger documentation (auto-generated)
- [x] Test cases and passing test results (`mvn test`)
- [x] System architecture (above)
- [ ] Screenshots of working modules — capture from `localhost:8080`
- [ ] Walkthrough video — record with Win+G (Xbox Game Bar) or OBS while clicking through
      Admin Dashboard → Interns → Attendance → Tasks → Reports → Projects → Intern portal
