# TaskFlow — Backend

[![CI](https://github.com/OWNER/task-flow-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/task-flow-backend/actions/workflows/ci.yml)

Jira-style task management with a **skill-aware auto-assignment engine**. When an
issue is created it is scored against every project member on skill match, spare
WIP capacity and historical resolution speed, and assigned automatically — with
the full ranked candidate list persisted so you can see *why*.

**Stack:** Java 21 · Spring Boot 4 · MySQL 8 · Flyway · STOMP WebSocket · Docker · GitHub Actions

> Replace `OWNER` in the badge URL with your GitHub org/user after pushing.

---

## Run it

### Docker (API + MySQL)

```bash
cp .env.example .env          # adjust MYSQL_ROOT_PASSWORD if you like
docker compose up --build
```

API on `http://localhost:8080`, health at `/actuator/health`. Flyway creates the
schema on first boot.

### Local (MySQL in Docker, app from your IDE / Maven)

```bash
docker compose up -d mysql
./mvnw spring-boot:run
```

### Seed demo data

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
```

Creates 6 users (varied skills), 1 project (`TF`) and 30 issues. All seeded
users have password `password123`; e.g. `admin@taskflow.dev` / `lead@taskflow.dev`.

---

## API

Base path `/api/v1`. Everything except `/auth/**` needs `Authorization: Bearer <token>`.

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me` |
| Users | `GET /users`, `GET /users/{id}`, `PATCH /users/{id}`, `GET /users/{id}/workload` |
| Skills | `GET/POST /skills`, `GET/PUT /users/{id}/skills` |
| Projects | `POST/GET /projects`, `GET/PATCH/DELETE /projects/{id}`, `GET/POST/DELETE /projects/{id}/members`, `GET /projects/{id}/board?sprintId=` |
| Sprints | `POST/GET /projects/{id}/sprints`, `POST /sprints/{id}/start`, `POST /sprints/{id}/complete`, `GET /sprints/{id}/report` |
| Issues | `POST /projects/{id}/issues`, `GET /issues/{key}`, `GET /issues?assignee=me&status=TODO`, `PATCH /issues/{key}`, `PATCH /issues/{key}/status`, `PATCH /issues/{key}/assignee`, `POST /issues/{key}/reassign`, `GET /issues/{key}/assignment-log`, `DELETE /issues/{key}` |
| Comments | `GET/POST /issues/{key}/comments`, `DELETE /comments/{id}` |
| Assignment | `GET/PUT /assignment/config`, `POST /assignment/preview`, `POST /assignment/rebalance` |

Errors use one shape: `{ timestamp, status, code, message, fieldErrors?, path }`.

### Quick start

```bash
# register
curl -sX POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"me@x.dev","password":"password123","fullName":"Me"}'

TOKEN=... # accessToken from the response

# create a project, then an issue — the engine assigns it after the 201 returns
curl -sX POST localhost:8080/api/v1/projects -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"keyCode":"TF","name":"Demo"}'

curl -sX POST localhost:8080/api/v1/projects/1/issues -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Wire up MySQL","type":"TASK","priority":"HIGH","requiredSkills":[{"skillId":1,"weight":2}]}'

# see the scored ranking
curl -s localhost:8080/api/v1/issues/TF-1/assignment-log -H "Authorization: Bearer $TOKEN"
```

---

## Auto-assignment engine

Trigger → `POST /projects/{id}/issues` saves the issue (`UNASSIGNED`) and
publishes `IssueCreatedEvent`. An `@TransactionalEventListener(AFTER_COMMIT)` +
`@Async` listener then runs the engine, so the HTTP call returns `201`
immediately and no log row can ever point at a rolled-back issue.

**Scoring** (`WeightedScoringStrategy`, weights from `taskflow.assignment.*`,
tunable live via `PUT /assignment/config`):

```
skillScore = Σ(weight_i · proficiency_i/5) / Σ(weight_i)      over the issue's required skills
loadScore  = clamp(1 − openCount / wipLimit, 0, 1)
speedScore = clamp(1 − avgResolutionHours / maxAvgHours, 0, 1)   (neutral 0.5 with no history)
total      = 0.5·skillScore + 0.3·loadScore + 0.2·speedScore
```

**Filters** (logged with a reason): `WIP_CAP_EXCEEDED`, `BELOW_SKILL_THRESHOLD`
(default 0.2), `SELF_ASSIGN_BLOCKED`. No eligible candidate → issue goes to
`TRIAGE` and the nightly job retries it.

**Concurrency.** Two issues created at the same instant can both read
`openCount = n` for the same person and both pick them. Fix: a per-project
application lock serializes assignment runs for a project, and the winner's WIP
is re-checked with a fresh count inside the locked transaction before the issue
is saved. `@Version` on `User` / `Issue` is the database-level backstop.

**Nightly job** (`@Scheduled`, 02:00): recompute `avg_resolution_hours` over a
30-day window, re-run assignment for anything stuck in `TRIAGE`, log a metrics
snapshot (assignment-log rows, triage backlog).

---

## Real-time

STOMP over SockJS at `/ws`. Subscribe to
`/topic/projects/{projectId}/board`. The CONNECT frame is authenticated from an
`Authorization: Bearer <token>` STOMP header in a `ChannelInterceptor` — an
unauthenticated socket is rejected. Only small deltas are published
(`ISSUE_CREATED`, `ISSUE_ASSIGNED`, `ISSUE_MOVED`, …), never whole boards.

---

## Tests

```bash
./mvnw test      # unit only — WeightedScoringStrategyTest, no Spring context
./mvnw verify    # + Testcontainers MySQL 8 integration tests (needs Docker); JaCoCo report at target/site/jacoco
```

### Postman / Newman (black-box E2E)

`postman/` holds a self-chaining collection (64 requests, 175 assertions) covering
auth, RBAC, projects/members, sprints, the create → async auto-assignment →
`assignment-log` flow, comments, and the assignment admin endpoints — plus the
negative cases (401/403/404/409, validation). Start the API with the `seed`
profile first (it needs a global admin + the skill catalogue):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
npx newman run postman/TaskFlow.postman_collection.json -e postman/TaskFlow.postman_environment.json
```

In the Postman app: import both files, select the **TaskFlow - Local**
environment, and run the collection top to bottom (iterations = 1). It creates
its own users/project each run and restores the assignment config at the end, so
it is re-runnable without cleanup.

Integration tests auto-skip when no Docker daemon is available.

- `WeightedScoringStrategyTest` — pins the scoring formula (perfect match → 1.0,
  zero overlap → filtered, WIP cap → filtered, no history → 0.5 speed, empty → triage).
- `AssignmentIntegrationTest` — register → project → create issue → assert the
  best skill match is auto-assigned; plus 10 concurrent creations never breaching
  any WIP limit.

---

## Configuration

| Key | Default | Notes |
|---|---|---|
| `TASKFLOW_JWT_SECRET` | dev secret | **set in every real env**, ≥ 32 chars |
| `taskflow.jwt.access-token-ttl` | `15m` | |
| `taskflow.jwt.refresh-token-ttl` | `7d` | |
| `taskflow.assignment.skill-weight` / `load-weight` / `speed-weight` | `0.5 / 0.3 / 0.2` | |
| `taskflow.assignment.min-skill-threshold` | `0.2` | |
| `taskflow.assignment.prevent-self-assign` | `true` | |
| `taskflow.assignment.triage-retry-hours` | `24` | |
| `taskflow.assignment.nightly-cron` | `0 0 2 * * *` | |
