# IOPMS — Interview Revision Cheat Sheet

> Read this once slowly, then read the **"5-minute cram"** section the night before.
> Goal: you can explain the project, navigate to any file, and answer cross-questions
> without looking at notes. Everything here is true of the code in this repo.

---

## 1. The 5-minute cram (memorize this)

**One-line pitch:**
> "IOPMS is an internal web app for Valen Labs where interns log their work and admins
> track and evaluate it — attendance, tasks, daily reports and project submissions, with
> server-enforced rules, JWT role-based login and a Supabase PostgreSQL database."

**Numbers to remember:**
- **7 modules** (A–G) · **8 DB tables** · **14 passing tests** (5 unit + 9 end-to-end)
- **2 roles** (ADMIN, INTERN) · **2 browser panels**, one login page
- **Java 21 / Spring Boot 3.4 / JWT / Supabase PostgreSQL 17 / plain HTML-CSS-JS**

**Commands (say them out loud):**
- `mvn spring-boot:run "-Dspring-boot.run.profiles=dev"` → local H2, zero setup
- `.\run-supabase.ps1` → real Supabase database
- `mvn test` → 14/14 green
- App: `http://localhost:8080` · Swagger: `/swagger-ui.html`

**Logins:** admin `admin@valenlabs.com / Admin@123` · intern `ayush@valenlabs.com / Intern@123`

**Three things you personally did that prove you understand it** (use these when asked):
1. Found and fixed a **missing `users.save()`** in intern creation — the intern was never
   persisted even though the API returned success; a test caught it.
2. Fixed a **500 error** on the projects list: `LazyInitializationException` because a lazy
   collection was read outside a transaction → added `@Transactional(readOnly = true)` and a
   regression test that lists projects.
3. Fixed a **startup crash** (`NoClassDefFoundError: UserRepository`) caused by a wrong
   package declaration in `AuthController.java` + a stale compiled class → fixed the package,
   ran `mvn clean test`.

---

## 2. What the project does (the story to tell)

- Problem: intern data was scattered (spreadsheets, chats, emails), hours were manual and
  disputable, feedback was informal, and no single view existed for evaluation.
- Solution: one app with **two views**:
  - **Intern panel**: login → edit profile → start/end work → see hours/history → update task
    progress → submit daily report → submit project deliverable → read admin feedback.
  - **Admin panel**: dashboard → create interns → assign tasks/projects → review reports and
    submissions → correct attendance with reason → export one intern's month as CSV.
- Key design theme to repeat: **rules are enforced by the backend, not by the UI**. The browser
  can be bypassed, so hours, roles, ownership and dates are all decided server-side.

**The 7 modules (know the letter → name):**
- **A** Auth & users — login, BCrypt, JWT, roles, intern CRUD, self-profile edit
- **B** Attendance — work sessions, server clock, one-open-session, corrections + audit
- **C** Tasks — admin assigns, intern updates progress, only admin completes
- **D** Daily reports — one per day, submit + review with remarks, missing-report check
- **E** Projects — multi-intern, versioned submissions with feedback
- **F** Dashboard — 5 sections of live counters
- **G** Intern report — consolidated month view + CSV export

---

## 3. Tech stack — what it is and why we used it (layman terms)

- **Java 21** — the programming language; chosen as the assessment's required backend language.
  Stable, widely used in enterprises, strong tooling.
- **Spring Boot 3.4** — a Java framework that sets up a web app for you (server, config,
  dependency injection). **Why:** "convention over configuration" — we write business code,
  not boilerplate. Spring alone would require manual XML/Java configuration; Boot auto-configures.
- **Spring MVC / REST controllers** — turns an HTTP request into a Java method call and back
  into JSON. **Why:** standard way to build APIs in Spring.
- **Spring Data JPA + Hibernate** — JPA is the API, Hibernate is the implementation. It maps
  Java objects (entities) to database rows so we write repositories instead of SQL.
  **Why:** fast development; the same code works on H2 and PostgreSQL.
- **Spring Security + JWT (jjwt)** — security framework + token library. **Why:** stateless
  auth fits a REST API; no server session to store or sync.
- **BCrypt** — password hashing algorithm with a random salt. **Why:** even if the DB leaks,
  passwords aren't recoverable; never store plain text or fast hashes like MD5/SHA.
- **Supabase (PostgreSQL 17)** — managed cloud database. **Why:** production-grade SQL DB with
  a dashboard; no database server to install/maintain. PostgreSQL supports the constraints we
  rely on (partial unique index, unique constraints).
- **H2** — small embedded database used only in `dev` and tests. **Why:** anyone can run the
  project instantly with no cloud account; tests are fast and isolated.
- **Plain HTML/CSS/JS** — two browser pages served by Spring Boot itself from `static/`.
  **Why:** no npm/build step, no CORS problem (same origin), focus stays on backend.
- **Maven (`pom.xml`)** — build tool and dependency manager. **Why:** declared dependencies,
  reproducible builds, standard commands (`mvn test`, `mvn spring-boot:run`).
- **Lombok** — generates getters/constructors at compile time via annotations. **Why:** less
  boilerplate; needed explicit annotation processor config in `pom.xml` to work on Java 21.
- **Swagger (springdoc)** — auto-generated API documentation at `/swagger-ui.html`.
- **Postman collection** — saved example requests in `postman/`, useful for the assessment.
- **Git + GitHub** — version control (12+ commits, branch `main`) and the public repo.

---

## 3.1 Decision log — what we picked, why, and what we rejected

> Interviewer-friendly pattern: **"We chose X because Y; we didn't pick Z because…; the trade-off is…"**

- **Spring Boot over plain servlets/Spring MVC**
  - Why: auto-config + dependency injection + huge ecosystem → faster, less error-prone setup.
  - Rejected: manual servlet/XML configuration (lots of boilerplate).
  - Trade-off: some "magic" — you must understand what auto-configuration does.
- **REST + JSON over server-rendered pages / SOAP**
  - Why: matches our JS frontend exactly; each endpoint is small and testable independently.
  - Trade-off: more HTTP calls, so we keep endpoints coarse (one call returns a whole report).
- **JPA / Hibernate over hand-written JDBC SQL**
  - Why: repository interfaces instead of SQL boilerplate; same code runs on H2 and Postgres.
  - Rejected: raw JDBC (verbose, DB-specific).
  - Trade-off: less control → we hit a lazy-loading 500 and had to learn about `@Transactional`.
- **Stateless JWT over server sessions**
  - Why: no session storage on the server; token carries identity + role; scales horizontally.
  - Rejected: `HttpSession` (server memory, sticky sessions).
  - Trade-off: can't instantly revoke a token — future improvement is refresh tokens/blacklist.
- **BCrypt over MD5/SHA/plain text**
  - Why: slow + salted → brute-force resistant; never store reversible passwords.
  - Trade-off: hashing costs a little CPU at login (intentionally).
- **Supabase PostgreSQL over a local database**
  - Why: managed, real SQL constraints, dashboard to verify data; assessment wanted a cloud DB.
  - Rejected: MongoDB (no SQL constraints like partial unique index for our invariants).
- **H2 for dev/tests over always using Supabase**
  - Why: zero-setup local runs, fast isolated tests, no credentials needed for `mvn test`.
  - Trade-off: two DBs → must ensure JPQL works on both (it did).
- **Plain HTML/CSS/JS over React/Angular**
  - Why: no npm/build step; Spring Boot serves the files → same-origin, no CORS; focus on backend.
  - Trade-off: manual DOM rendering; we fixed re-render bugs (month picker, intern list).
- **Frontend served by the backend over a separate static host**
  - Why: one process to run, no CORS configuration, simple deployment story.
- **Business rules in services, not only in the UI**
  - Why: a user can call the API directly with Postman; the UI can be bypassed. Every rule
    (one open session, one report/day, ownership, who can complete a task) lives in the service.
  - Trade-off: more service code, but it is unit-testable with Mockito.
- **DB constraints in addition to service checks**
  - Why: defence in depth — even a bug or concurrent request can't create two open sessions or
    two reports on the same date.
- **Append-only audit table over overwriting the row**
  - Why: after a correction we must answer "what was it before, who changed it, why?".
- **Month filter as a query parameter over client-side filtering**
  - Why: less data over the wire; server stays authoritative for hour totals.
- **Hand-written CSV exporter over a PDF library**
  - Why: one small format covers the requirement, no extra dependency; PDF declared as out of scope.
- **`ddl-auto=update` over Flyway migrations**
  - Why: MVP speed — tables appear on first boot.
  - Trade-off (admit it): not safe for evolving a real production schema; Flyway would be next.
- **Transaction pooler over Supabase's direct host**
  - Why: direct host is IPv6-only and unreachable here; pooler is IPv4 (port 6543).
  - Trade-off: must use `postgres.<ref>` as username and `prepareThreshold=0` with JDBC.
- **Lombok over manual getters/constructors**
  - Why: less boilerplate; `@RequiredArgsConstructor` gives constructor injection for free.
  - Trade-off: needs annotation-processor configuration on Java 21 (we fixed that early on).
- **Environment variables over hardcoded credentials**
  - Why: the repo is public — no passwords or secrets in code; `run-supabase.ps1` prompts.
- **`@Valid` DTOs over trusting request bodies**
  - Why: reject bad input at the edge with 400 before it reaches business logic.
- **One `GlobalExceptionHandler` over try/catch in every method**
  - Why: consistent error format and status codes; real details stay in server logs.
- **Package-per-feature (`attendance/`, `task/`, …) over layer-only packages**
  - Why: everything for one feature sits together — easy to find when interviewer asks "where is X?".
- **Unit + e2e tests over manual clicking only**
  - Why: unit tests catch rule regressions instantly; e2e proves filters/URLs/JSON/status codes work.
  - Evidence: tests caught the missing `save()` and the lazy-loading 500 before delivery.

---

## 3.2 Dependency injection in this project — where to point in the code

**The idea in one line:** a class doesn't create its own helpers with `new`; Spring creates them
once and **hands them in through the constructor**. That's dependency injection (DI).

**The pattern used everywhere here:**
- Class is marked `@Service`, `@Component`, `@Configuration` or `@RestController` → Spring
  registers it as a **bean**.
- Dependencies are `private final` fields.
- Lombok's `@RequiredArgsConstructor` generates the constructor that takes those fields —
  Spring calls it automatically. There is **no `@Autowired` in our code** (constructor injection
  via Lombok is cleaner).

**Concrete injection points (open these files when asked):**

| File | What is injected into it | Why |
|------|--------------------------|-----|
| `auth/AuthController.java` | `UserRepository`, `PasswordEncoder`, `JwtService` | login needs to find user, check hash, issue token |
| `auth/SecurityConfig.java` | `JwtAuthFilter`; **defines** `@Bean PasswordEncoder` (BCrypt) and `@Bean SecurityFilterChain` |central place that wires security together |
| `auth/JwtAuthFilter.java` | `JwtService`, `UserRepository` | validate token, then confirm the DB user is still active |
| `attendance/AttendanceService.java` | `AttendanceRepository`, `AttendanceAuditRepository` | rules + saving sessions and audit rows |
| `user/InternService.java` | `UserRepository`, `PasswordEncoder` | create interns with hashed passwords |
| `project/ProjectService.java` | `ProjectRepository`, `ProjectSubmissionRepository`, `UserRepository` | projects + versions + assignees |
| `admin/AdminController.java` | 7 services + `CurrentUser` | all admin screens behind one controller |
| `intern/InternController.java` | `CurrentUser` + 4 services | self-scoped endpoints |
| `admin/DashboardService.java` | repositories **and** Task/Report/Project services | composing other services is normal |
| `common/CurrentUser.java` | `UserRepository` | resolve "who am I" from the token, once, for all controllers |
| `user/SeedData.java` | `UserRepository`, `PasswordEncoder` | seed admin with a proper hash at startup |

**Where the injected objects come from:**
- `@Service` / `@Component` → our own classes (e.g. `JwtService`, `CurrentUser`).
- Spring Data JPA → creates repository **implementations** from the interfaces at startup
  (we only write the interface, e.g. `UserRepository extends JpaRepository<User, Long>`).
- `@Bean` methods in `SecurityConfig` → third-party objects we choose to create, e.g.
  `new BCryptPasswordEncoder()` is made once and injected into `AuthController`, `InternService`
  and `SeedData`.
- By default these beans are **singletons** — one shared instance per app, not one per request.

**Why DI matters here (interviewer answer):**
- Testability: `AttendanceServiceTest` uses Mockito `@Mock` repositories + `@InjectMocks` service
  → business rules tested without a database.
- Swappability: change H2 → Postgres via config; services don't care.
- Single source of truth: one `PasswordEncoder`, one `CurrentUser` resolver — consistent behaviour.
- No `new Service()` scattered around → loose coupling, easier to change one layer.

**What is NOT injected** (don't overclaim): DTOs and entities are created with `new`,
`LocalDateTime.now()` is called directly, and local variables/strings are ordinary code — DI is
for shared collaborators only.

**Common DI terms flashlist** (full explanations in section 12):
- **Bean** — object created and managed by Spring.
- **Container / context** — the thing that creates and holds the beans.
- **Inversion of control (IoC)** — Spring, not your class, controls object creation.
- **Constructor injection** — dependencies passed in via the constructor (our style).
- **Autowiring** — Spring matching a constructor parameter to an available bean.
- **`@Bean`** — method that declares "make this object a bean" (e.g. `PasswordEncoder`).
- **`@Service` / `@Component` / `@Repository` / `@RestController`** — "make this class a bean" markers.
- **Singleton** — one instance for the whole app.
- **Interface vs implementation** — a contract vs the class doing the work; Spring injects the
  implementation behind the interface (repositories).
- **`@Mock` / `@InjectMocks`** — Mockito's fake dependency / the object under test in unit tests.
- **Loose coupling** — classes know the *role* (interface) they need, not the concrete builder.

---

## 4. How one request travels (explain this diagram from memory)

```
Browser (fetch, Bearer token in localStorage)
   ↓
Tomcat / Spring Boot receives HTTP
   ↓
JwtAuthFilter — reads token, verifies signature, sets "who is calling" + role
   ↓
Security rules — /api/admin/** needs ADMIN, /api/intern/** needs INTERN
   ↓
Controller — @RequestMapping, @Valid checks the body, returns status codes
   ↓
Service — business rules, @Transactional, ownership checks (THE important layer)
   ↓
Repository (Spring Data JPA) — SQL generated, runs against Postgres/H2
   ↓
Database — constraints enforce the final invariants
   ↓
Exceptions → GlobalExceptionHandler → 400 / 403 / 409 / 500 JSON error
```

**Worked example — intern clicks "Start Work":**
1. `intern.js` calls `API.post('/intern/attendance/start')` with the Bearer token.
2. Filter validates JWT → knows the user id; role check lets INTERN through.
3. `InternController` → `AttendanceService.start(userId)`.
4. Service checks "no open session today" (and DB partial unique index backs it up),
   stamps `LocalDateTime.now()` server-side, saves one row.
5. Response → JS reloads the attendance view; the "Working since" message comes from the DB.

**Worked example — admin approves a report:**
1. `admin.js` → `POST /api/admin/reports/{id}/review` with `status` + `remarks`.
2. Role check → `ReportService.review()` verifies the report exists, stores status + remarks.
3. Intern's next `GET /api/intern/reports` shows the new remarks — the feedback loop is closed.

---

## 5. Must-know module details

### A — Auth & users
- Login only accepts email + password; BCrypt verifies the hash, checks `active` flag,
  issues a signed JWT containing user id + role + name.
- Roles are `ROLE_ADMIN` / `ROLE_INTERN`; "admin accounts are seeded, never created via API".
- Admin can create/update/deactivate interns; deactivated accounts are rejected **even with a
  valid token** (the filter checks the DB user's active flag).
- Intern can edit only name, phone, college via `PUT /api/auth/me` (email/password not self-editable).

### B — Attendance (the module interviewers love)
- Check-in/check-out ≠ login. Separate actions, separate concept.
- **Server clock only** — no client timestamps accepted, so interns can't fake hours.
- **One open session** per intern: service check + DB partial unique index (`check_out IS NULL`).
- End without start → 400. Minutes are computed by the backend from the two timestamps.
- Monthly view shows total hours + days with sessions that never ended.
- Admin correction: must supply a **reason**, old/new values are appended to
  `attendance_audit` (append-only log). Interns have **no edit endpoint**.

### C — Tasks
- Admin creates/assigns: title, description, assignee, priority, dates, status, remarks.
- Intern updates progress (0–100) and status among NOT_STARTED / IN_PROGRESS / UNDER_REVIEW.
- **Only admin** can set COMPLETED or CHANGES_REQUESTED — interns can't mark own work done.

### D — Daily reports
- Unique index on (intern_id, date): **one report per day**, duplicate → 409.
- Fields: tasks completed, work performed, time split, progress %, challenges, pending,
  next-day plan, links. Submit timestamp stored server-side.
- Admin review: APPROVED or CHANGES_REQUESTED + remarks; "missing reports" query for a date.

### E — Projects
- Project has supervisor, dates, status and many interns (`project_interns` join table).
- Deliverable submission = repo URL + notes; version number assigned automatically per
  (project, intern) — v1, v2…; reviews attach comments to each version, nothing overwritten.

### F — Dashboard
- Five server-computed sections: Team, Attendance (today), Tasks, Reports (today), Projects.
- "Why server-side?" — one query, no extra client round trips, counts can't be tampered with.

### G — Intern report
- One endpoint returns profile + attendance + tasks + reports + submissions + feedback for
  one intern and a month; CSV export uses the same data written by hand (no library).
- "Why CSV but no PDF?" — scope: one export format covers the requirement without a dependency;
  PDF was consciously left out and listed as a limitation.

---

## 6. Database must-knows

- 8 tables: `users`, `attendance`, `attendance_audit`, `tasks`, `daily_reports`, `projects`,
  `project_interns`, `project_submission`. Full DDL in `schema-postgres.sql`.
- Two constraints to quote:
  - unique (intern_id, date) on daily_reports → one report per day
  - partial unique index on attendance → one open session
- `attendance_audit` is append-only: corrections never overwrite history.
- `ddl-auto=update` creates/updates tables at startup; dev uses H2, production uses Supabase.
  Risk to admit: it's fine for an MVP, but a real project should use migration tool (Flyway).
- **Supabase connection story (your best "I actually debugged it" story):**
  - Supabase direct host was IPv6-only → Java got `UnknownHostException`; our network had no IPv6.
  - Tried Mumbai and Singapore poolers → `tenant/user not found` (wrong region).
  - Tokyo (`aws-0-ap-northeast-1.pooler.supabase.com:6543`) worked with username
    `postgres.xkfruqyvujflgmyclsab` and `?sslmode=require&prepareThreshold=0`.
  - `run-supabase.ps1` sets env vars and prompts for the password — no secret in git.

---

## 7. Security must-knows

- **JWT flow:** login → BCrypt check → signed token (HS256, secret ≥ 32 bytes) → browser stores
  it in `localStorage` → sends `Authorization: Bearer …` on every API call → filter validates.
- **Stateless** = server stores no session; each token carries identity + role. Trade-off:
  logout is client-side (discard token); a stolen token is valid until it expires (a refresh/
  revocation list would be the next improvement).
- **Two layers of role checks:** URL rules (`/api/admin/**`, `/api/intern/**`) *and* service-level
  ownership checks. Reason: URL rules can be misconfigured; defence in depth.
- **Intern id comes from the token, never a request parameter** → prevents ID guessing/IDOR.
- `@Valid` + jakarta constraints on request bodies → bad input gets 400 before reaching services.
- Error handling: validation → 400, authz → 403, conflicts (duplicate report, open session) → 409,
  unexpected → 500 with details in logs only.

---

## 8. Testing & quality must-knows

- `mvn test` → **14 tests**:
  - `AttendanceServiceTest` (5, Mockito) — pure business rules, no DB/network: one-open-session,
    end-without-start, hour computation, correction recompute + audit, invalid correction.
  - `AppFlowTest` (9, full HTTP + in-memory H2) — login → create intern → 401/403 role isolation
    → attendance rules → 480-min audited correction → task progress rules → one-report-per-day
    409 → review → versioned submissions → dashboard numbers → consolidated report + CSV.
- Why both kinds? Unit tests pinpoint rule failures fast; e2e proves the wiring (filters, URLs,
  JSON, status codes) actually works together.
- Manual verification was also done in a real browser: both panels, check-in/check-out cycle,
  progress update, report + deliverable submission, admin feedback seen by intern, zero console
  errors.
- Bugs found and fixed (say these — they prove hands-on work):
  - `InternService.create` forgot `users.save()` → e2e test caught it.
  - Project list 500 `LazyInitializationException` → `@Transactional(readOnly=true)` + test.
  - `AuthController` wrong package + stale class → clean rebuild.
  - Frontend forms had no `name` attributes → `FormData` was empty → shared `getForm()` by id.
  - Attendance re-render destroyed the month picker → render into a dedicated `att-body` div.
  - Admin `intern-list` null when other tabs called `loadInterns()` → guarded render.

---

## 9. Git & GitHub basics (if asked)

- Git = local version control (snapshots called **commits**); GitHub = hosted copy online.
- Branch `main` is the main line of development; `origin` is the GitHub remote.
- `git status` = what changed; `git add` = stage; `git commit -m "..."` = snapshot with message;
  `git push origin main` = upload commits; `git pull` = download.
- `.gitignore` keeps `target/` (build output) and `data/` (local H2 DB) out of the repo.
- Our repo: `github.com/Ayush0G/iopms` — README renders the docs; commits show the history.

---

## 10. Interview cross-questions — quick answers

- **"Explain the project in 30 seconds."** → Use the one-line pitch + "rules enforced server-side".
- **"Why Spring Boot over plain Java servlets?"** → Auto-config, dependency injection, huge
  ecosystem, faster and less error-prone; production-standard in Java.
- **"Where is dependency injection used in your code?"** → Everywhere Spring hands a collaborator
  to a constructor: `AuthController` gets `UserRepository`/`PasswordEncoder`/`JwtService`,
  `AttendanceService` gets two repositories, `AdminController` gets seven services; Lombok's
  `@RequiredArgsConstructor` writes the constructor. `SecurityConfig` also *defines* beans with
  `@Bean` (e.g. the BCrypt encoder). Details in section 3.2.
- **"Why constructor injection and no `@Autowired`?"** → Required dependencies are explicit in
  the constructor, the object is always fully built, and it's trivial to pass Mockito mocks in
  unit tests. `@Autowired` field injection hides dependencies and needs reflection.
- **"What is a bean / is it one per request?"** → A bean is an object managed by Spring; by
  default it's a singleton shared across the app (e.g. one `PasswordEncoder`, one `CurrentUser`).
- **"Describe one design decision and its trade-off."** → Use section 3.1, e.g. "JPA over JDBC:
  fast and portable, but we had to learn lazy loading when a list request returned 500."
- **"Why JPA/Hibernate instead of writing SQL?"** → Less boilerplate, portable across H2 and
  Postgres; SQL is still there when needed (JPQL/queries). Trade-off: less control, needs
  understanding of lazy loading — we hit that bug and fixed it.
- **"What is a controller/service/repository?"** → Controller = HTTP entry; service = business
  rules + transaction; repository = DB access. One-way dependency, no skipping layers.
- **"What is a DTO?"** → Data Transfer Object: a shape for request/response JSON (Java `record`),
  keeps the database entity from leaking to the client.
- **"What is @Transactional and why did projects break?"** → It opens a DB transaction/session
  for the method. Listing projects mapped a lazy `interns` collection after the session closed →
  `LazyInitializationException` → 500. Adding `@Transactional(readOnly=true)` kept the session
  open through mapping. Test added so it can't regress.
- **"How do you stop an intern from faking hours?"** → No timestamps accepted from client;
  server `LocalDateTime.now()`; one open session; minutes computed server-side; corrections
  audited with reason.
- **"How do you stop interns editing each other's data?"** → User id taken from the JWT,
  endpoints are self-scoped, service checks ownership, plus role URL rules.
- **"Why store password hashes?"** → A hash is one-way and salted; even a DB leak doesn't reveal
  the password. Never MD5/SHA because they're fast to brute force.
- **"JWT vs sessions?"** → JWT is stateless and scales horizontally; trade-off is revocation is
  harder (we'd add a token blacklist/refresh rotation in production).
- **"Why H2 and Postgres together?"** → H2 = zero-setup dev/tests; Postgres = real product.
  Same JPA code works on both — one profile switch.
- **"What is a profile?"** → Named config set: `dev` activates H2; no profile = Supabase env vars.
- **"How is one-report-per-day enforced?"** → DB unique index + duplicate check → 409.
- **"How do versions work?"** → Service queries max version for (project, intern) and adds 1.
- **"Why CSV by hand?"** → Tiny format, standard escaping, no dependency; PDF wasn't required.
- **"What did Swagger/Postman do for you?"** → Swagger = live auto API docs; Postman = saved,
  reusable requests with token variable so the API can be tested without the UI.
- **"How would you add a new feature?"** → Entity + migration/schema → repository → DTO →
  service with rules + transaction → controller with role check → UI tab → unit + e2e tests.
- **"How would you deploy?"** → `mvn package` → run the jar on a host/container with the 4 env
  vars (DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET); put it behind HTTPS. Honest: not deployed.
- **"What would you improve?"** → Refresh tokens, pagination, email notifications, Flyway
  migrations, PDF export, real modal inputs instead of `prompt()`, automated CI.
- **"This looks AI-assisted — what did you actually do?"** → See section 11 below. Be calm and
  specific; point at the bugs you found and the flows you verified.
- **"I don't know that one."** → Say: "I haven't used that directly. Based on what I know,
  I'd approach it by… and I'd check the docs/source." Honesty + reasoning beats bluffing.

---

## 11. Handling the "AI-made project" question (important)

- **Don't deny it.** Say: *"Yes, I used AI as a coding assistant to speed up boilerplate, but I
  read and verified every part, ran the app, and fixed the bugs it surfaced."*
- Back it with **specific, checkable evidence**:
  - "The AI-written code had a missing `users.save()`; my end-to-end test caught it and I fixed it."
  - "I debugged a 500 error in project listing myself — `LazyInitializationException` — and
    added a regression test."
  - "I connected Supabase by hand: the IPv6/pooler/region problem took real trial and error."
  - "I clicked through both panels in a browser and fixed three frontend bugs I found there."
- **Show you can navigate**: open the repo and go straight to `AuthController.java`,
  `AttendanceService.java`, `AppFlowTest.java`, `application.properties`. Know what each does.
- **Practice live explanations** without notes:
  1. Trace the login request end-to-end (section 4).
  2. Explain the attendance rules and why each exists.
  3. Explain the JWT flow and the two-layer role check.
  4. Walk through one test and say what it proves.
- If asked to change something live, talk through your approach first: find the file → change the
  service rule → update the UI if needed → run `mvn test` → re-check in browser. That demonstrates
  you can handle the code, which is the whole point.

---

## 12. Glossary — layman terms

- **API** — a defined way for one program to talk to another (here: HTTP endpoints returning JSON).
- **REST** — an API style using URLs + HTTP verbs: GET = read, POST = create/action, PUT = update,
  DELETE = remove.
- **Endpoint / route** — a specific URL that does one job, e.g. `/api/intern/attendance/start`.
- **HTTP status code** — 200 OK, 400 bad input, 401 not logged in, 403 not allowed,
  409 conflict, 500 server bug.
- **JSON** — the text format APIs use: `{"name": "Ayush", "role": "INTERN"}`.
- **Client / server** — client = browser asking; server = our Java app answering.
- **Tomcat** — the web server embedded inside Spring Boot that listens on port 8080.
- **Spring** — Java framework providing dependency injection and ready-made web/security pieces.
- **Dependency injection** — instead of creating helper objects yourself, Spring hands them to
  your class (via constructor); makes code testable.
- **Bean** — an object managed by Spring.
- **Controller** — class that receives HTTP requests and returns responses.
- **Service** — class holding business rules and transaction boundaries.
- **Repository / DAO** — class whose job is DB reads/writes; Spring Data can generate it from an
  interface name.
- **Entity** — Java class mapped to a DB table (e.g. `User` → `users`).
- **DTO (record)** — a simple data carrier for API input/output; avoids exposing the entity directly.
- **JPA** — Java standard for object↔database mapping; **Hibernate** is the tool implementing it.
- **JPQL** — SQL-like query language that queries Java entities instead of tables.
- **Transaction** — a group of DB operations treated as one unit: all succeed or all roll back;
  `@Transactional` opens one for a method.
- **Lazy loading** — related data is fetched only when first used; if the DB session is already
  closed, reading it throws `LazyInitializationException` (our projects bug).
- **Schema** — the definition of tables, columns, types and constraints.
- **Constraint** — a DB-enforced rule, e.g. "this pair of columns must be unique".
- **Unique index** — DB rule preventing duplicates (one report per day, one open session).
- **Partial unique index** — uniqueness rule that applies only to rows matching a condition
  (e.g. only rows where check-out is null).
- **Audit trail / audit table** — append-only history of who changed what and why.
- **Migration** — a versioned script that changes a DB schema safely (Flyway/Liquibase; we use
  `ddl-auto` for the MVP instead).
- **BCrypt** — slow, salted one-way password hashing; same password gives different hashes.
- **Salt** — random data mixed into a hash so identical passwords don't produce identical hashes.
- **JWT** — a signed "ID card" containing user id + role; the server verifies the signature.
- **Bearer token** — the `Authorization: Bearer <token>` header carrying the JWT.
- **Stateless** — server keeps no login session; the token carries all needed info each time.
- **HS256** — the signing algorithm; the secret must be ≥ 32 bytes.
- **localStorage** — browser storage that survives refresh; here it holds the JWT.
- **CORS** — browser rule blocking cross-origin calls; not an issue because our frontend is served
  by the same Spring Boot app (same origin).
- **Role / authority** — a label (ADMIN / INTERN) deciding what endpoints you may call.
- **IDOR** — guessing someone else's ID to access their data; prevented by deriving identity from
  the JWT, not request parameters.
- **Validation (`@Valid`)** — checking request data before business logic (e.g. required fields).
- **Profile (Spring)** — a named config set; `dev` = H2, default = Supabase.
- **H2** — tiny embedded database used for local dev and tests.
- **PostgreSQL / Supabase** — PostgreSQL is the SQL database; Supabase is a hosted PostgreSQL service.
- **Connection pool (Hikari)** — reuse a set of open DB connections instead of opening one per
  request.
- **Pooler / transaction pooler** — Supabase's IPv4 gateway to Postgres (port 6543) for networks
  that can't use the IPv6 direct connection.
- **IPv6** — newer internet address format; Supabase's direct DB host has only IPv6 addresses.
- **DDL** — schema-definition SQL (`CREATE TABLE …`).
- **CSV** — plain text table with comma-separated values; opens in Excel.
- **Swagger / OpenAPI** — auto-generated, interactive API docs.
- **Postman** — app to send saved API requests with variables (like our `token`).
- **Maven / pom.xml** — build tool and its config file listing dependencies and plugins.
- **Lombok** — annotation library generating getters/constructors at compile time.
- **Unit test vs integration/e2e test** — unit = one method in isolation (Mockito mocks);
  e2e = real HTTP calls through the whole app against a test database.
- **Mockito** — library creating fake dependencies so a service can be tested alone.
- **Commit / branch / remote / push** — git snapshot / line of work / online copy / upload commits.
- **CI** — automated build+test on every push (not set up here; mentioned as a next step).

---

## 13. What NOT to say

- Don't say "I don't know how that works" about your *own* core code — revisit sections 4–7 first.
- Don't claim the app is deployed online — it runs locally/on your machine against Supabase.
- Don't say "we used MySQL" — it's **PostgreSQL via Supabase** (H2 only for dev/tests).
- Don't say interns can mark tasks COMPLETED — only admins can.
- Don't say hours are computed in JavaScript — they are computed on the server.
- Don't overclaim about PDF export — CSV is implemented; PDF is a stated limitation.
- Don't guess an answer confidently when unsure; explain how you'd verify it instead.

---

## 14. Final 10-minute practice plan

1. Say the one-line pitch out loud twice.
2. Trace the "Start Work" request from browser to DB without notes.
3. Explain the 3 bugs you fixed, one sentence each.
4. Explain the Supabase IPv6/pooler story.
5. Point at DI in code: open `AuthController.java` (injected fields), `SecurityConfig.java`
   (`@Bean`), and `AttendanceServiceTest.java` (`@Mock` / `@InjectMocks`).
6. Name 3 decisions + their trade-offs from section 3.1 (JPA, JWT, plain JS are easy ones).
7. Open the repo and find: `AppFlowTest.java`, `application.properties`,
   `schema-postgres.sql`, `presentation/index.html`.
8. Open the app and click the demo path: admin login → Interns → logout → intern login →
   Attendance → Tasks → Daily Report → Projects.
9. Read the "AI-made" answer in section 11 once — then say it in your own words.
