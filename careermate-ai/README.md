# CareerMate AI

AI-powered career development and job-search platform: job discovery, AI job matching, resume analysis and building,
application tracking, notifications and an AI career assistant, for every industry.

> **Status of this code base:** generated from the project requirements. The frontend was type-checked, unit-tested and
> production-built. The backend, database migrations and Docker files were written carefully but **could not be compiled,
> tested or started in the environment where they were generated** (no access to Maven Central or Docker). Run
> `mvn test` and `docker compose up --build` once on your machine and expect to fix small issues. See
> [Verification status](#verification-status).

## Technology stack

| Layer | Technology |
|---|---|
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, React Router, Axios, Lucide icons, Recharts |
| Backend | Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA, JWT (jjwt), Maven |
| Database | PostgreSQL 16, Flyway migrations |
| AI | Google Gemini API (server side; per-user keys encrypted at rest) with rule-based fallbacks |
| Files | PDFBox (PDF text), Apache POI (DOCX text), OpenPDF (PDF export) |
| Ops | Docker, Docker Compose, Nginx reverse proxy, Spring Actuator health checks |
| Tests | JUnit 5, Mockito, Spring Boot Test + MockMvc (H2), Vitest + React Testing Library |

## Quick start (Docker)

```bash
cp .env.example .env        # optional but recommended: set JWT_SECRET and passwords
docker compose up --build
```

* App: http://localhost:3000 (Nginx serves the SPA and proxies `/api` to the backend)
* Swagger UI: http://localhost:3000/swagger-ui.html (or http://localhost:8080/swagger-ui.html)
* Health: http://localhost:8080/actuator/health

Demo credentials (created when `SEED_DEMO_DATA=true`, the default):

| Role | Email | Password |
|---|---|---|
| Job seeker (pre-filled profile, sample applications) | `demo@careermate.local` | `Demo@12345` |
| Administrator | `admin@careermate.local` | `Admin@12345` |

**Change or disable these for any real deployment** (`SEED_DEMO_DATA=false`).

## Local development (without Docker)

Prerequisites: JDK 21, Maven 3.9+, Node 20+ (22 recommended), PostgreSQL 14+.

```bash
# 1. Database
createdb careermate   # and a user matching DB_USERNAME / DB_PASSWORD (defaults: careermate / careermate)

# 2. Backend (http://localhost:8081) - Flyway creates the schema and demo jobs on first start
cd backend
export JWT_SECRET="$(openssl rand -base64 48)"
export API_KEY_ENCRYPTION_SECRET="$(openssl rand -base64 48)"   # optional; defaults to JWT_SECRET
export GEMINI_API_KEY=            # optional shared key; users can also add their own in Settings
mvn spring-boot:run

# 3. Frontend (http://localhost:5173) - Vite proxies /api to localhost:8081
cd frontend
npm install
npm run dev
```

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | local Postgres | JDBC connection |
| `JWT_SECRET` | dev value | HMAC key, **at least 32 characters**; always override in production |
| `JWT_EXPIRATION_MINUTES` | `1440` | Token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated origins |
| `API_KEY_ENCRYPTION_SECRET` | falls back to `JWT_SECRET` | Encrypts per-user Gemini keys at rest; use a separate long random value in production |
| `RESUME_STORAGE_DIR` | `./data/resumes` (`/data/resumes` in Docker) | Where uploaded/built resume files live |
| `GEMINI_API_KEY` | empty | Optional shared server key. Users can add their own key in Settings; empty = fallback mode |
| `GEMINI_MODEL` | `gemini-3.5-flash` | Gemini model id |
| `GEMINI_TIMEOUT_SECONDS` | `30` | Request timeout |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | empty / `587` | SMTP server for password-reset codes. Empty host = code is logged to the backend console (dev only) |
| `MAIL_FROM`, `MAIL_FROM_NAME` | `no-reply@careermate.local`, `CareerMate AI` | Sender address/name |
| `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | `true`, `true` | SMTP security options |
| `PASSWORD_RESET_OTP_EXPIRY_MINUTES` | `10` | Reset code lifetime |
| `PASSWORD_RESET_RESEND_COOLDOWN_SECONDS` | `60` | Minimum wait before another code is emailed |
| `PASSWORD_RESET_MAX_ATTEMPTS` | `5` | Wrong-code attempts before the code is invalidated |
| `SEED_DEMO_DATA` | `true` | Create demo + admin users |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `DEMO_USER_EMAIL` / `DEMO_USER_PASSWORD` | see `.env.example` | Seed account credentials |
| `JOB_SYNC_ENABLED` | `true` | Scheduled external job sync |
| `JOB_SYNC_CRON` | `0 0 */6 * * *` | Sync schedule (Spring cron) |
| `JOB_SYNC_ON_STARTUP` | `true` | Import real listings on first startup when no external jobs exist |
| `JOB_PREFER_EXTERNAL` | `true` | Keep live external listings ahead of seeded demo jobs in recommendations (demo jobs remain as fallback) |
| `JOB_SYNC_ITPRO`, `JOB_SYNC_REMOTIVE`, `JOB_SYNC_ARBEITNOW` | `true` | Enable individual keyless sources |
| `ITPRO_COUNT`, `ITPRO_DAYS_BEHIND`, `ITPRO_TYPE`, `ITPRO_CATEGORY` | `50`, `30`, `0`, `0` | ITPro.lk query settings (0 = all types/categories) |
| `JOOBLE_ENABLED`, `JOOBLE_API_KEY` | `false`, empty | Jooble aggregator (free key from https://jooble.org/api/about); disabled until a key is set |
| `JOOBLE_KEYWORDS`, `JOOBLE_LOCATION`, `JOOBLE_MAX_PAGES` | `software engineer`, `Sri Lanka`, `5` | Jooble search query settings |
| `JSEARCH_ENABLED`, `JSEARCH_API_KEY` | `false`, empty | JSearch on RapidAPI (https://rapidapi.com/letscrape-6bRBa3QguO5/api/jsearch); disabled until a key is set |
| `JSEARCH_QUERY`, `JSEARCH_COUNTRY`, `JSEARCH_NUM_PAGES`, `JSEARCH_HOST` | `software engineer`, `lk`, `1`, `jsearch.p.rapidapi.com` | JSearch query settings |
| `JOB_SYNC_MAX_PER_SOURCE`, `JOB_SYNC_STALE_DAYS` | `50`, `21` | Import size / expiry of unseen jobs |
| `NOTIFICATION_CRON` | `0 0 * * * *` | Reminder generation schedule |
| `FRONTEND_PORT`, `BACKEND_PORT` | `3000`, `8080` | Published ports (Compose) |

## AI configuration (Google Gemini)

1. Create a key in Google AI Studio (https://aistudio.google.com/apikey).
2. Sign in to CareerMate, open **Settings**, and use the **Gemini API key** card to add, update or remove your own key.
   The backend validates the key with Google, encrypts it with AES-256-GCM and stores it on your user account. The full
   key is never returned to the browser or written to logs; only a masked suffix such as `****abcd` is shown.
3. Optionally set a shared fallback key with `GEMINI_API_KEY` in `.env` (Docker) or the environment (local). Users
   without a personal key use this shared key. Keep `API_KEY_ENCRYPTION_SECRET` stable so saved user keys remain
   decryptable across restarts.
4. The Settings card and assistant page show whether AI is active. Users can delete their key at any time to return to
   the shared server key or built-in fallback logic.

AI is used for: job compatibility refinement, resume analysis, resume text suggestions and the career assistant.
**Without a key every feature still works** using deterministic fallbacks (weighted skill/experience/education matching,
a rule-based resume analyzer with spelling/ATS checks, and a keyword-driven assistant). On timeouts, HTTP 429, invalid
responses or outages the backend silently falls back per request. Prompts tell the model to treat resume/profile text as
data and never to invent qualifications.

## Architecture

```
frontend/ (React SPA)  --/api-->  Nginx  -->  backend/ (Spring Boot)  -->  PostgreSQL
                                                   |--> file storage (resumes)
                                                   |--> Gemini API (optional)
                                                   |--> external job APIs (optional)
```

Backend packages (`com.careermate`): `config`, `security` (JWT filter, token revocation list), `auth`, `user`, `profile`,
`jobs` (+ `ingestion`), `matching`, `resume`, `applications`, `notifications`, `assistant`, `dashboard`, `ai`
(Gemini client), `common` (exceptions, response wrappers, utilities). Each feature has controller, service, repository,
entities and DTOs. All responses use `ApiResponse<T>` (`success`, `message`, `data`, `timestamp`). Validation errors
return field messages in `data`. Roles are stored as strings (`JOB_SEEKER`, `RECRUITER`, `ADMIN`) so recruiter features can
be added later. Admin endpoints require `ROLE_ADMIN`.

Data access rules: every private resource (profile, resumes, analyses, applications, conversations, notifications,
saved jobs) is looked up by `id AND user_id`; other users' resources return `404`.

### Job sources
* **Manual:** `POST /api/v1/admin/jobs`, `POST /api/v1/admin/jobs/import` (admin only).
* **Seeded demo jobs:** 11 fictional sample jobs (IT, business, finance, marketing, customer service, engineering, HR,
  design, education, hospitality, internship) created by Flyway `V2`. They are labelled "Demo listing" and are **not real vacancies**.
* **Automated:** `ExternalJobSource` implementations for the public ITPro.lk (Sri Lanka), Remotive and Arbeitnow APIs,
  plus the Jooble and JSearch aggregators when their API keys are configured. Sync runs on `JOB_SYNC_CRON` when
  `JOB_SYNC_ENABLED=true`, once on startup when `JOB_SYNC_ON_STARTUP=true` (only if no external jobs exist yet), or on
  demand via `POST /api/v1/admin/jobs/sync`. Jobs are normalised, de-duplicated by `(source, external_id)` and a
  title/company/location fingerprint, keep their original application URL and source, and are deactivated when their
  deadline passes or they stop appearing in the feed. With `JOB_PREFER_EXTERNAL=true` live listings are listed ahead of
  seeded demo jobs, which remain available as a fallback instead of being removed. Respect each provider's terms and
  rate limits; add your own feed by implementing `ExternalJobSource` as a Spring bean. No scraping is used.

### Matching algorithm
Overall = 50% skills (required skills weigh 2x, synonyms such as `k8s`/`postgres`/`golang` normalised, resume text also
counts) + 20% experience level vs years + 10% education level + 15% preferences + 5% posting recency. Ambiguous short
terms such as `Go` are only inferred from `golang` or an explicit job title, not from prose. With Gemini configured the
AI score is blended 60/40 with this result and its strengths/gaps/recommendations replace the generic ones.

## API documentation

* Swagger UI: `/swagger-ui.html`, OpenAPI JSON: `/v3/api-docs`
* Endpoint reference: [docs/API.md](docs/API.md)
* Postman collection: [docs/CareerMate.postman_collection.json](docs/CareerMate.postman_collection.json)
  (run **Login** first; it stores the token in a collection variable)

## Testing

```bash
cd backend  && mvn test          # JUnit 5 + MockMvc on in-memory H2, Mockito unit tests
cd frontend && npm test          # Vitest + React Testing Library
cd frontend && npm run build     # type-check + production build
```

Backend tests cover registration/login/JWT/logout, profile management, job search/filter/pagination/saving, job
matching (including Gemini success and failure), resume upload validation/extraction/analysis fallback/PDF builder,
applications and history, notifications, dashboard statistics, assistant fallback and Gemini paths, ingestion
de-duplication/expiry, and cross-user access restrictions. Tests run with Flyway disabled and Hibernate creating the schema
on H2 (PostgreSQL mode); the real PostgreSQL migrations are exercised only when the app is started against Postgres.

## Verification status

What was actually run when this project was generated:

| Check | Result |
|---|---|
| Frontend `tsc` type-check, `vite build`, Vitest (10 tests) | Passed |
| Backend Java syntax check (`javac` parse only) and import heuristics | Passed (no syntax errors) |
| Backend `mvn compile` / `mvn test` | **Not run** (Maven Central unreachable in the generation sandbox) |
| Flyway migrations on PostgreSQL | **Not run** |
| `docker compose up --build`, image builds, health checks | **Not run** (Docker unavailable) |
| Calls to Gemini, Remotive, Arbeitnow | **Not run** (no credentials / network); response parsing follows their documented formats and may need small adjustments |

## Troubleshooting

* **Backend fails with "JWT_SECRET must be at least 32 characters"** - set a longer secret.
* **`Connection refused` to Postgres** - check `DB_URL`; in Docker the host is `db`.
* **Frontend shows "Cannot reach the server"** - backend not started, or in dev the Vite proxy target is wrong (`VITE_DEV_API_TARGET`).
* **Resume upload rejected** - only text-based PDF/DOCX up to 5 MB; scanned/image PDFs have no extractable text.
* **AI answers say "offline mode"** - add a key in **Settings > Gemini API key**, or set `GEMINI_API_KEY` as a shared fallback. The model id may also be unavailable to the key (check logs for HTTP status).
* **Gemini returns errors for the default model** - set `GEMINI_MODEL` to a model your key can use.
* **PDF export shows `?` for some characters** - the built-in PDF fonts support Latin-1 only; non-Latin scripts are not supported in the builder PDF.
* **Reset everything** - `docker compose down -v` (deletes database and uploaded resumes).

## Security notes

BCrypt password hashing, stateless JWT with server-side revocation on logout (in-memory list, cleared on restart; use a
shared store such as Redis if you run several backend instances), CORS allow-list, bean validation, JPA parameterised queries,
upload size/extension/magic-byte validation, files stored outside the database under generated names, path-traversal guard,
no secrets in the frontend, single-use hashed password-reset codes with expiry/attempt limits and non-enumerating
responses. Not included: rate limiting, email verification and refresh tokens; add them before exposing the service
publicly. The default demo passwords and dev JWT secret must not be used in production.
