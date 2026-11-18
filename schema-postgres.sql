-- IOPMS database schema (PostgreSQL / Supabase).
-- The application creates this schema automatically via JPA ddl-auto=update;
-- this file documents it and can seed a fresh Supabase project manually.

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,          -- BCrypt
    phone         VARCHAR(255),
    college       VARCHAR(255),
    role          VARCHAR(10)  NOT NULL,          -- ADMIN | INTERN
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    joined_date   DATE,
    end_date      DATE
);

CREATE TABLE IF NOT EXISTS attendance (
    id          BIGSERIAL PRIMARY KEY,
    intern_id   BIGINT      NOT NULL REFERENCES users(id),
    date        DATE        NOT NULL,
    check_in    TIMESTAMP   NOT NULL,             -- set by the server only
    check_out   TIMESTAMP,                        -- NULL = open/incomplete session
    minutes     BIGINT,                           -- working hours, computed by the backend
    correction  VARCHAR(10) NOT NULL DEFAULT 'NONE'  -- NONE | CORRECTED
);

CREATE TABLE IF NOT EXISTS attendance_audit (      -- every admin correction, append-only
    id            BIGSERIAL PRIMARY KEY,
    attendance_id BIGINT      NOT NULL REFERENCES attendance(id),
    admin_id      BIGINT      NOT NULL REFERENCES users(id),
    old_check_in  TIMESTAMP,
    old_check_out TIMESTAMP,
    new_check_in  TIMESTAMP,
    new_check_out TIMESTAMP,
    reason        VARCHAR(255) NOT NULL,
    at            TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS tasks (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    description   VARCHAR(255),
    assignee_id   BIGINT      NOT NULL REFERENCES users(id),
    priority      VARCHAR(10),                      -- LOW | MEDIUM | HIGH
    start_date    DATE,
    deadline      DATE,
    status        VARCHAR(20) NOT NULL,             -- NOT_STARTED | IN_PROGRESS | UNDER_REVIEW | COMPLETED | CHANGES_REQUESTED
    progress      INT         NOT NULL DEFAULT 0,   -- 0-100
    admin_remarks VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS daily_reports (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id),
    date            DATE        NOT NULL,
    tasks_completed VARCHAR(255) NOT NULL,
    work_performed  VARCHAR(255) NOT NULL,
    time_spent      VARCHAR(255) NOT NULL,
    progress        INT         NOT NULL,
    challenges      VARCHAR(255),
    pending_work    VARCHAR(255) NOT NULL,
    next_day_plan   VARCHAR(255) NOT NULL,
    links           VARCHAR(255),
    submitted_at    TIMESTAMP   NOT NULL,
    status          VARCHAR(20) NOT NULL,           -- PENDING | APPROVED | CHANGES_REQUESTED
    admin_remarks   VARCHAR(255),
    CONSTRAINT one_report_per_intern_per_day UNIQUE (user_id, date)
);

CREATE TABLE IF NOT EXISTS projects (
    id                 BIGSERIAL PRIMARY KEY,
    title              VARCHAR(255) NOT NULL,
    description        VARCHAR(255),
    supervisor         VARCHAR(255) NOT NULL,
    start_date         DATE,
    expected_end_date  DATE,
    status             VARCHAR(10) NOT NULL          -- ACTIVE | COMPLETED | ON_HOLD
);

CREATE TABLE IF NOT EXISTS project_interns (          -- many-to-many assignment
    project_id BIGINT NOT NULL REFERENCES projects(id),
    user_id    BIGINT NOT NULL REFERENCES users(id),
    CONSTRAINT project_intern_pk PRIMARY KEY (project_id, user_id)
);

CREATE TABLE IF NOT EXISTS project_submission (
    id             BIGSERIAL PRIMARY KEY,
    project_id     BIGINT      NOT NULL REFERENCES projects(id),
    intern_id      BIGINT      NOT NULL REFERENCES users(id),
    repo_url       VARCHAR(255) NOT NULL,
    notes          VARCHAR(255),
    submitted_at   TIMESTAMP   NOT NULL,
    version        INT         NOT NULL,              -- 1,2,3... per project+intern
    status         VARCHAR(20) NOT NULL,              -- PENDING | APPROVED | CHANGES_REQUESTED
    admin_comments VARCHAR(255)
);
