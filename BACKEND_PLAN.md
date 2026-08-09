# OC Tile Install — Backend API & Admin Dashboard Development Plan

**Status:** Planning phase  
**Target:** Java Spring Boot REST API + React Admin Dashboard for inquiry & inspection logging  
**Architecture:** Separate `backend/` folder, Spring Boot, MySQL, JWT auth

---

## 0. Model Strategy — READ FIRST

This plan is built to be worked through by **more than one model**. Each phase is tagged with the model to run it on, and there are 🛑 **SWITCH** banners between phases where the model needs to change. Switch with `/model <id>` in Claude Code.

**Start on Haiku 4.5.** Full sequence:

| Phases | Model | `/model` command | Why |
|---|---|---|---|
| Backend 1–2 | Haiku 4.5 | `/model claude-haiku-4-5-20251001` | Maven config + JPA entities are mechanical boilerplate |
| **Backend 3** | **Opus 5** | `/model claude-opus-5` | **Auth code — do not economize here** |
| Backend 4–6 | Sonnet 5 | `/model claude-sonnet-5` | Repetitive CRUD; phase 4 is the template for phase 5 |
| Frontend 1–4 | Sonnet 5 | *(stay on Sonnet 5)* | Forms, tables, Tailwind — well-trodden ground |

**Why Phase 3 gets Opus.** JWT auth is the one place in this plan where a weak model's mistakes are both most damaging and least visible: a broken filter chain or a botched BCrypt comparison still compiles and still passes a happy-path test. Everything else here fails loudly when it's wrong. Spend the Opus tokens on that one phase, then drop back down.

**Why Haiku stops after Phase 2.** Haiku 4.5 has a **200K context window**; Sonnet 5 and Opus 5 have 1M. Once a dozen backend files are in play, Haiku starts losing track of the codebase — that's the practical reason to move off it, more than raw capability.

**Cost note (as of 2026-08-04):** Sonnet 5 is at introductory pricing — **$2/$10 per million tokens through 2026-08-31**, vs. its normal $3/$15. Haiku 4.5 is $1/$5; Opus 5 is $5/$25. Doing the bulk of this in August is meaningfully cheaper.

**At every 🛑 SWITCH banner:** stop, run the `/model` command shown, then continue. If you're an AI model reading this plan and you hit a banner naming a model that isn't the one you're running as, **stop and tell the user to switch** rather than continuing into the next phase.

---

## 1. Project Structure

```
octileinstall/
├── backend/                          (New Java backend)
│   ├── pom.xml                       (Maven dependencies)
│   ├── src/main/java/com/octileinstall/
│   │   ├── OctileInstallApplication.java  (Spring Boot entry point)
│   │   ├── controller/
│   │   │   ├── AuthController.java        (login, register)
│   │   │   ├── InquiryController.java     (CRUD inquiries)
│   │   │   └── InspectionController.java  (CRUD inspections)
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── InquiryService.java
│   │   │   └── InspectionService.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── InquiryRepository.java
│   │   │   └── InspectionRepository.java
│   │   ├── entity/
│   │   │   ├── User.java              (Alberto + admin users)
│   │   │   ├── Inquiry.java           (Customer inquiries)
│   │   │   └── Inspection.java        (Scheduled inspections)
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java
│   │   │   └── JwtAuthFilter.java
│   │   ├── dto/
│   │   │   ├── LoginRequest.java
│   │   │   ├── LoginResponse.java
│   │   │   ├── InquiryDTO.java
│   │   │   └── InspectionDTO.java
│   │   └── config/
│   │       └── SecurityConfig.java
│   ├── src/main/resources/
│   │   ├── application.properties      (Spring config, DB credentials)
│   │   └── schema.sql                  (Database initialization)
│   └── Dockerfile                     (Optional: for containerization)
├── admin-dashboard/                   (New React frontend)
│   ├── package.json
│   ├── public/
│   │   └── index.html
│   ├── src/
│   │   ├── App.jsx
│   │   ├── pages/
│   │   │   ├── LoginPage.jsx
│   │   │   ├── DashboardPage.jsx
│   │   │   ├── InquiriesPage.jsx
│   │   │   └── InspectionsPage.jsx
│   │   ├── components/
│   │   │   ├── InquiryForm.jsx
│   │   │   ├── InquiryTable.jsx
│   │   │   ├── InspectionForm.jsx
│   │   │   └── InspectionTable.jsx
│   │   ├── services/
│   │   │   └── api.js               (Axios calls to backend)
│   │   └── index.jsx
│   └── .env                         (API_URL=http://localhost:8081/api)
└── [existing frontend site remains unchanged]
```

---

## 2. Database Schema (MySQL)

```sql
-- Users table (Alberto & admin staff)
CREATE TABLE users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,  -- BCrypt hashed
  full_name VARCHAR(255) NOT NULL,
  role ENUM('ADMIN', 'VIEWER') DEFAULT 'VIEWER',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Inquiries table (Customer project inquiries)
CREATE TABLE inquiries (
  id INT PRIMARY KEY AUTO_INCREMENT,
  customer_name VARCHAR(255) NOT NULL,
  customer_email VARCHAR(255) NOT NULL,
  customer_phone VARCHAR(20),
  project_type ENUM(
    'Custom Shower',
    'Bathroom Floors & Walls',
    'Kitchen Backsplashes',
    'Flooring - Tile, Marble & Mosaic',
    'Tub Surrounds',
    'Repairs'
  ),
  city VARCHAR(255),
  message TEXT,
  status ENUM('NEW', 'CONTACTED', 'QUOTED', 'SCHEDULED', 'COMPLETED', 'ARCHIVED') DEFAULT 'NEW',
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by INT,
  FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Inspections table (Scheduled site visits)
-- NOTE: originally had scheduled_date DATETIME + scheduled_time TIME (the redundant split
-- flagged in §9). Collapsed to a single DATETIME during Phase 3 — see backend/db/schema.sql
-- for the version actually implemented.
CREATE TABLE inspections (
  id INT PRIMARY KEY AUTO_INCREMENT,
  inquiry_id INT,
  scheduled_date DATETIME NOT NULL,
  location VARCHAR(500),
  status ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE CASCADE
);

-- Index for faster queries
CREATE INDEX idx_inquiry_status ON inquiries(status);
CREATE INDEX idx_inquiry_created ON inquiries(created_at);
CREATE INDEX idx_inspection_scheduled ON inspections(scheduled_date);
```

---

## 3. Backend Development Phases

> 🟢 **START HERE — Haiku 4.5.** Run `/model claude-haiku-4-5-20251001` before beginning Phase 1.

### Phase 1: Project Setup & Database

> **Model: Haiku 4.5**

**Tasks:**
- [ ] Initialize Maven project with Spring Boot 3.x starter
- [ ] Add dependencies: Spring Web, Spring Data JPA, MySQL Driver, Spring Security, JWT, Lombok, Validation
- [ ] Configure `application.properties` with MySQL connection, JPA settings, JWT secret
- [ ] Create database schema in MySQL
- [ ] Test connection with simple H2 in-memory DB first (optional, for TDD)

**Acceptance:**
- Project builds without errors (`mvn clean package`)
- Database tables created and accessible via MySQL client
- Spring Boot app starts on `localhost:8081` (port 8080 is taken by another local project's static-site server, see `.claude/launch.json`)

---

### Phase 2: Data Entities & Repositories

> **Model: Haiku 4.5** (no switch needed — same as Phase 1)

**Tasks:**
- [ ] Create `User` entity (id, email, passwordHash, fullName, role, timestamps)
- [ ] Create `Inquiry` entity (customer info, project type, status, notes, timestamps, FK to User)
- [ ] Create `Inspection` entity (inquiryId, scheduledDate, location, status, notes, timestamps)
- [ ] Create JPA repositories for all three entities
- [ ] Add Lombok annotations (`@Entity`, `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- [ ] Add validation annotations (`@NotBlank`, `@Email`, `@NotNull`, etc.)

**Acceptance:**
- All entities compile and map to database tables
- Repositories can be injected in services (no compilation errors)
- Can run a quick integration test to save/retrieve a User

---

## 🛑 SWITCH: Haiku 4.5 → Opus 5

**Stop here. Before starting Phase 3, run:**

```
/model claude-opus-5
```

Phase 3 is the auth layer. It's the one phase in this plan where a mistake compiles cleanly, passes a happy-path test, and ships a security hole anyway — a filter that never runs, a `SecurityContext` that isn't populated, a BCrypt check comparing the wrong two values. Every other phase fails loudly when it's wrong; this one doesn't.

Run it on Opus 5, then switch back down at the next banner.

---

### Phase 3: Authentication (JWT)

> 🔴 **Model: Opus 5** — if you are not running as Opus 5, stop and tell the user to switch before writing any code in this phase.

**Tasks:**
- [ ] Create `JwtTokenProvider` service
  - Generate JWT from User (payload: id, email, role; exp: 7 days)
  - Validate JWT signature
  - Extract user info from token
- [ ] Create `JwtAuthFilter` (extends `OncePerRequestFilter`)
  - Extract token from `Authorization: Bearer <token>` header
  - Validate and set SecurityContext
- [ ] Create `SecurityConfig` class
  - Disable CSRF (stateless API)
  - Set JWT filter in chain
  - Permit `/api/auth/login` without auth, require auth for `/api/inquiry/**`, `/api/inspection/**`
- [ ] Create `AuthController` with POST `/api/auth/login` endpoint
  - Request: `{ email, password }`
  - Response: `{ token, expiresIn }`
  - On success: generate JWT with user's id, email, role
  - On failure: return 401 Unauthorized
- [ ] Create `AuthService` to validate credentials (BCrypt password check)

**Acceptance:**
- POST `/api/auth/login` with valid credentials returns JWT
- Requests to protected endpoints without token return 401
- Requests with valid token are authenticated (Principal available in controller)

---

## 🛑 SWITCH: Opus 5 → Sonnet 5

**Stop here. Before starting Phase 4, run:**

```
/model claude-sonnet-5
```

Auth is done — the expensive part is behind you. Phases 4 through 6 are repetitive CRUD and tests, which Sonnet 5 handles at near-Opus quality for a fraction of the cost (and it's at introductory pricing through 2026-08-31). **Stay on Sonnet 5 for the rest of this plan, backend and frontend.** There are no further switches.

---

### Phase 4: Inquiry Endpoints (CRUD)

> **Model: Sonnet 5**

**Tasks:**
- [ ] Create `InquiryController`
  - GET `/api/inquiry` — list all inquiries (sorted by created_at DESC)
  - GET `/api/inquiry/{id}` — get single inquiry
  - POST `/api/inquiry` — create new inquiry (from customer form)
  - PUT `/api/inquiry/{id}` — update inquiry (status, notes)
  - DELETE `/api/inquiry/{id}` — delete inquiry
- [ ] Create `InquiryService`
  - Encapsulate business logic (status transitions, validation)
  - Set `created_by` from JWT principal when creating
- [ ] Create `InquiryDTO` (DTO pattern to avoid exposing JPA entities)
  - Request: `{ customerName, customerEmail, customerPhone, projectType, city, message }`
  - Response: `{ id, customerName, ..., status, createdAt, createdBy }`
- [ ] Add filtering/pagination
  - Query params: `?status=NEW&limit=20&offset=0`
  - Response includes total count

**Acceptance:**
- POST `/api/inquiry` creates a new inquiry in DB
- GET `/api/inquiry?status=NEW` returns only new inquiries
- PUT `/api/inquiry/1` updates status/notes (authenticated)
- DELETE `/api/inquiry/1` removes inquiry

---

### Phase 5: Inspection Endpoints (CRUD)

> **Model: Sonnet 5** (no switch — use Phase 4 as the template)

**Tasks:**
- [ ] Create `InspectionController`
  - GET `/api/inspection` — list all inspections (sorted by scheduled_date ASC)
  - GET `/api/inspection/{id}` — get single inspection
  - POST `/api/inspection` — create new inspection (link to inquiry)
  - PUT `/api/inspection/{id}` — update inspection (status, notes, reschedule)
  - DELETE `/api/inspection/{id}` — delete inspection
- [ ] Create `InspectionService`
  - Validate inquiry exists before creating inspection
  - Validate scheduled_date is in future
- [ ] Create `InspectionDTO`
  - Request: `{ inquiryId, scheduledDate, location }` (scheduledDate is a single ISO-8601 datetime, e.g. `"2026-08-10T14:00:00"`)
  - Response: `{ id, inquiryId, scheduledDate, ..., status, createdAt }`

**Acceptance:**
- POST `/api/inspection` with valid inquiryId creates inspection
- GET `/api/inspection?from=2026-08-01&to=2026-08-31` filters by date range
- PUT `/api/inspection/1` reschedules or updates status
- Attempting to create inspection for non-existent inquiry returns 404

---

### Phase 6: Error Handling & Testing

> **Model: Sonnet 5** (no switch)

**Tasks:**
- [ ] Create global `@ControllerAdvice` exception handler
  - `NotFoundException` → 404
  - `ValidationException` → 400 with error message
  - `UnauthorizedException` → 401
  - Generic Exception → 500 with safe error message
- [ ] Add request validation (e.g., `@Valid` on DTO parameters)
- [ ] Write integration tests for auth endpoints
- [ ] Write integration tests for inquiry CRUD
- [ ] Write integration tests for inspection CRUD
- [ ] Test with Postman/Insomnia collection

**Acceptance:**
- All happy-path tests pass
- All error-path tests pass (invalid input, missing auth, not found)
- Postman collection provided for manual testing

---

## 4. Frontend (React Dashboard) Phases

> ✅ **No switch needed — stay on Sonnet 5** for all four frontend phases.

### Phase 1: Setup & Login

**Tasks:**
- [ ] Create React app with Vite (faster than CRA)
- [ ] Install dependencies: axios, react-router-dom, date-fns, tailwindcss
- [ ] Create `.env` file with `VITE_API_URL=http://localhost:8081/api`
- [ ] Create `LoginPage` component
  - Form: email, password
  - Button: Submit
  - On success: store JWT in localStorage, redirect to dashboard
  - On failure: show error message
- [ ] Create auth context/hook to manage JWT globally
- [ ] Create protected route wrapper (`PrivateRoute`)
  - If no JWT, redirect to login
  - If JWT exists, allow access

**Acceptance:**
- User can log in with valid credentials
- JWT stored in localStorage
- User redirected to dashboard on success
- Invalid credentials show error message

---

### Phase 2: Dashboard & Inquiry Management

**Tasks:**
- [ ] Create `DashboardPage` (home/overview)
  - Show summary: total inquiries, new inquiries, scheduled inspections
  - Quick stats cards
- [ ] Create `InquiriesPage` with table
  - Display all inquiries: customer name, project type, status, created date
  - Sort by column (created_at DESC default)
  - Filter by status dropdown
  - Pagination controls
- [ ] Create `InquiryForm` modal/page
  - Add new inquiry: customer info, project type, message
  - Edit existing inquiry: update status, add notes
  - Submit creates/updates in backend via POST/PUT
- [ ] Add row actions (Edit, Delete, View Details)
- [ ] Create inquiry detail modal: full info, notes, inspection history

**Acceptance:**
- Dashboard shows correct stats
- Inquiry table loads and displays all inquiries
- Can filter by status
- Can create new inquiry (shows in table immediately)
- Can edit inquiry details and status
- Can delete inquiry with confirmation

---

### Phase 3: Inspection Management

**Tasks:**
- [ ] Create `InspectionsPage` with calendar/table view
  - Display scheduled inspections: date, time, location, status, linked inquiry
  - Sort by scheduled_date ASC
  - Color-code by status (green=scheduled, blue=in-progress, gray=completed)
- [ ] Create `InspectionForm` modal
  - Schedule new inspection: link to inquiry, select date/time, location
  - Edit existing inspection: reschedule, update status, notes
  - Submit creates/updates via POST/PUT
- [ ] Add inspection row actions (Edit, Mark Complete, Reschedule, Delete)
- [ ] Show inspection details with linked inquiry info

**Acceptance:**
- Inspections page shows all scheduled inspections
- Can create new inspection linked to an inquiry
- Can reschedule inspection (update date/time)
- Can mark inspection as completed
- Can delete inspection

---

### Phase 4: UI Polish & Responsive Design

**Tasks:**
- [ ] Add Tailwind CSS styling
  - Dark theme to match main site (navy/blue)
  - Responsive layout (mobile, tablet, desktop)
- [ ] Add navigation sidebar with logout
- [ ] Add loading spinners for async operations
- [ ] Add success/error toast notifications
- [ ] Add confirmation dialogs for destructive actions (delete)
- [ ] Test on mobile/tablet viewports

**Acceptance:**
- Dashboard looks professional and matches main site brand
- All pages responsive and usable on mobile
- UX is intuitive (clear buttons, good feedback)

---

## 5. API Reference

### Authentication

**POST /api/auth/login**
```json
Request:
{
  "email": "alberto@octileinstall.com",
  "password": "securePassword123"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 604800,
  "userId": 1,
  "email": "alberto@octileinstall.com"
}
```

### Inquiries

**GET /api/inquiry**
```
Query params: ?status=NEW&limit=20&offset=0
Response (200 OK): { data: [...], total: 45 }
```

**POST /api/inquiry**
```json
Request:
{
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "customerPhone": "(714) 555-1234",
  "projectType": "Custom Shower",
  "city": "Irvine",
  "message": "Looking for a custom shower design..."
}

Response (201 Created):
{
  "id": 1,
  "customerName": "John Doe",
  "status": "NEW",
  "createdAt": "2026-08-04T10:30:00Z",
  "createdBy": 1
}
```

**PUT /api/inquiry/{id}**
```json
Request:
{
  "status": "CONTACTED",
  "notes": "Called customer, scheduled site visit for Aug 10"
}

Response (200 OK): { updated inquiry }
```

**DELETE /api/inquiry/{id}**
```
Response (204 No Content)
```

### Inspections

**GET /api/inspection**
```
Query params: ?from=2026-08-01&to=2026-08-31&status=SCHEDULED
Response (200 OK): { data: [...], total: 12 }
```

**POST /api/inspection**
```json
Request:
{
  "inquiryId": 1,
  "scheduledDate": "2026-08-10T14:00:00",
  "location": "123 Main St, Irvine, CA 92614"
}

Response (201 Created):
{
  "id": 1,
  "inquiryId": 1,
  "scheduledDate": "2026-08-10T14:00:00Z",
  "status": "SCHEDULED",
  "createdAt": "2026-08-04T10:30:00Z"
}
```

**PUT /api/inspection/{id}**
```json
Request:
{
  "status": "COMPLETED",
  "notes": "Inspection completed. Design finalized with customer."
}

Response (200 OK): { updated inspection }
```

---

## 6. Development Workflow

1. **Set up backend locally**
   - Clone/pull repo, `cd backend`
   - `mvn spring-boot:run` (starts on 8081)
   - MySQL running locally on port 3306

2. **Set up frontend locally**
   - Clone/pull repo, `cd admin-dashboard`
   - `npm install`
   - `npm run dev` (starts on 5177 — Vite's default 5173-5176 were all taken by other local dev servers)
   - Update `.env` to point to backend API

3. **Test workflow**
   - Log in via dashboard
   - Create inquiry
   - Create inspection linked to inquiry
   - Update statuses
   - Test filtering and pagination

4. **Deployment** (post-launch, Phase 9)
   - Backend: Deploy to Heroku, AWS, or DigitalOcean
   - Frontend: Deploy to Vercel or Netlify
   - Connect via environment variables

---

## 7. Task Checklist

Backend — **Haiku 4.5** (`/model claude-haiku-4-5-20251001`):
- [x] 1.1 Maven project setup
- [x] 1.2 MySQL connection configured
- [x] 2.1 Entity classes (User, Inquiry, Inspection)
- [x] 2.2 JPA repositories

🛑 **SWITCH → Opus 5** (`/model claude-opus-5`)

Backend — **Opus 5**:
- [x] 3.1 JWT token provider
- [x] 3.2 Auth filter & security config
- [x] 3.3 Auth controller & service

Phase 3 landed with 22 passing tests (`mvn test`). Three defects carried in from
Phases 1–2 were fixed as part of it, all of which would have surfaced as "auth is
broken" rather than pointing at their real cause:

1. **`server.servlet.context-path=/api`** — Spring Security matchers run against the
   path *within* the servlet context, so `requestMatchers("/api/auth/login")` would
   never have matched. The prefix now lives in `@RequestMapping` and the matchers are
   literal URLs.
2. **The seeded BCrypt hash in `schema.sql` was not a real digest** of any password, so
   nobody could ever have logged in. Account creation moved to `DataSeeder`, which hashes
   with the same `PasswordEncoder` bean that login verifies against.
3. **`schema.sql` declared MySQL `ENUM`s of display strings** ("Custom Shower") while
   `@Enumerated(EnumType.STRING)` writes constant names (`CUSTOM_SHOWER`) — every insert
   would have been rejected. Columns are now `VARCHAR`, and the file moved to
   `backend/db/` because Spring Boot auto-executes a *classpath* `schema.sql` against
   embedded datasources, which broke the H2 test context.

The auth tests were mutation-checked: flipping `.anyRequest().authenticated()` to
`permitAll()` fails 5 tests, and removing the JWT filter from the chain fails 1. They
are not vacuous.

🛑 **SWITCH → Sonnet 5** (`/model claude-sonnet-5`) — stay here for everything below

Backend — **Sonnet 5**:
- [x] 4.1 Inquiry controller & service
- [x] 4.2 Inquiry DTOs
- [x] 5.1 Inspection controller & service
- [x] 5.2 Inspection DTOs
- [x] 6.1 Global exception handler
- [x] 6.2 Integration tests
- [x] 6.3 Postman collection

Phase 4 landed with 15 new tests (37 total, `mvn test`). Notes that carried into Phase 5:

- **`projectType` serializes as the display label** ("Custom Shower"), not the enum
  constant name, via `@JsonValue`/`@JsonCreator` on `Inquiry.ProjectType`. This matches
  both this doc's own API examples and the string values the public site's quote form
  already sends (`index.html`'s `<select name="projectType">`) — the two had drifted
  apart before this phase. JPA storage is unaffected (`@Enumerated(EnumType.STRING)`
  still persists `CUSTOM_SHOWER`); Jackson and JPA serialize independently.
- **`NotFoundException`** (`com.octileinstall.exception`) is shared, not Inquiry-specific
  — Phase 5 reused it directly for the "inspection references a nonexistent inquiry" 404
  rather than inventing a second exception type. Each controller still maps it locally
  via `@ExceptionHandler`; Phase 6 centralizes this into one `@ControllerAdvice`.
- **PUT is a partial update**: a field omitted from the request body is left
  unchanged, not nulled out. This was mutation-tested in both Phase 4 and Phase 5
  (unconditionally overwriting the fields fails the tests) precisely because "just
  assign both fields" is the natural-looking simplification that breaks it.
- **Pagination assumes `offset` is page-aligned** (`offset = page * limit`); an
  unaligned offset rounds down to its containing page rather than erroring. Documented
  in `InquiryService.list()` and reused as-is in `InspectionService.list()`.

Phase 5 landed with 21 new tests (58 total, `mvn test`). Notes for whoever picks up
Phase 6:

- **`ValidationException`** (`com.octileinstall.exception`) is new this phase, for the
  "scheduled date must be in the future" business rule — a runtime check that Bean
  Validation can't express since it depends on the current clock, not a fixed
  constraint. Mapped to 400 locally on `InspectionController` for now, same pattern as
  `NotFoundException`; Phase 6 should absorb both into the global handler.
- **The future-date check only runs when `scheduledDate` is actually present in a PUT
  body.** Marking an already-past inspection COMPLETED must not be blocked by it — that
  is the normal way a completed inspection gets closed out. This was mutation-tested
  two ways: validating unconditionally on every update blocks legitimate past-inspection
  status changes (1 test), and validating against the *old* date instead of the
  *incoming* one lets a past-date reschedule slip through unvalidated (a second, separate
  test). Both failed under the mutation; see `InspectionService.update()`.
- **`?from=`/`?to=` are calendar dates, not full datetimes**, expanded to day boundaries
  in `InspectionService` (`atStartOfDay()` / `atTime(LocalTime.MAX)`) since
  `scheduledDate` is a `LocalDateTime`. An absent bound is unbounded in that direction,
  not defaulted to "today". Spring's default JSR-310 converter binds the query param to
  `LocalDate` with no `@DateTimeFormat` annotation needed — confirmed by the date-range
  integration tests actually passing, not just assumed.

Phase 6 landed with 7 new tests (65 total, `mvn test`) plus a Postman collection at
`backend/postman/OctileInstall-Backend.postman_collection.json`. Notes for whoever picks
up frontend work, since it's the first thing that will actually consume these responses:

- **`GlobalExceptionHandler`** (`com.octileinstall.exception`) now owns every error
  response. The per-controller local `@ExceptionHandler` methods from Phases 3–5 are
  gone — `AuthController`, `InquiryController`, and `InspectionController` no longer
  handle exceptions themselves. `AuthService.login()` now throws the new
  `UnauthorizedException` instead of Spring Security's `BadCredentialsException`, so
  every application-thrown error (`NotFoundException`, `ValidationException`,
  `UnauthorizedException`) is one consistent custom-exception family.
- **Every error response shares one shape**: `{status, error, message}`, plus
  `fieldErrors` (a field→message map) specifically for `@Valid` failures. This covers
  `NotFoundException`→404, `ValidationException`→400, `UnauthorizedException`→401, bean
  validation→400, malformed JSON or a bad enum value in a request body→400, a
  query/path param that won't convert (`?limit=abc`, `?status=BOGUS`)→400, and anything
  unanticipated→500.
- **The 500 handler never echoes the real exception message to the client** — it logs
  the full exception server-side and returns a fixed, generic message. Mutation-tested:
  changing it to return `e.getMessage()` instead of the fixed string fails the test
  that specifically asserts the client-visible message doesn't contain the (deliberately
  sensitive-looking) mock exception text used in that test.
- **Not handled by the global advice on purpose**: Spring Security's
  `AuthenticationException`/`AccessDeniedException`. Those are resolved inside the
  security filter chain (`SecurityConfig`'s `authenticationEntryPoint` /
  `accessDeniedHandler`) before a request ever reaches a controller, so this doesn't
  affect today's 401/403 responses. But `@EnableMethodSecurity` has been on since Phase
  3 with nothing using `@PreAuthorize` yet — the first controller method that adds one
  and throws `AccessDeniedException` from inside its body WOULD reach the generic
  `Exception` handler first via Spring MVC's own resolution order, turning a correct 403
  into a misleading 500. See the comment on `GlobalExceptionHandler` for the fix
  (an explicit rethrowing handler) to add at that point — not added now since no code
  path can trigger it yet.

Frontend — **Sonnet 5** (no switch):
- [x] 1.1 React + Vite setup
- [x] 1.2 Login page
- [x] 1.3 Auth context & private routes
- [x] 2.1 Dashboard page
- [x] 2.2 Inquiries page & table
- [x] 2.3 Inquiry form (create/edit)
- [ ] 3.1 Inspections page
- [ ] 3.2 Inspection form (create/edit/reschedule)
- [ ] 4.1 Tailwind styling
- [ ] 4.2 Responsive design
- [ ] 4.3 Toast notifications & confirmations

Frontend Phase 1 landed. Notes for whoever picks up Phase 2, since it builds directly on
top of this:

- **Node on this machine is v16.18.0** (Vite 5+/Tailwind v4 need 18+). `package.json`
  deliberately pins `vite@^4` and `tailwindcss@^3` — both fully Node-16-compatible.
  `npm install` prints `EBADENGINE` warnings for a couple of transitive deps anyway; they're
  warnings only, `npm run build` and `npm run dev` both work. Don't "fix" this by bumping
  major versions without confirming Node itself has also been upgraded.
- **Dev server runs on port 5177, not Vite's default 5173.** Ports 5173–5176 were all
  already held by other local dev servers on this machine. The backend's
  `app.cors.allowed-origins` (`application.properties`) is set to match 5177 exactly — if
  the dev port ever needs to change again, update both sides together or every request
  will fail CORS.
- **`services/api.js`** is the one place that talks to the backend: an axios instance with
  a request interceptor attaching `Authorization: Bearer <token>` from localStorage, a
  response interceptor that clears storage and fires a same-window `auth:unauthorized`
  event on any 401 (covers an expired token discovered mid-session, not just a missing
  one), and `extractErrorMessage()`, which unwraps the backend's `{status, error, message,
  fieldErrors}` shape (see Phase 6) into a single string. Phase 2/3's Inquiry/Inspection
  API calls should reuse this `api` instance and `extractErrorMessage()` rather than
  duplicating error handling per page.
- **`AuthContext`** owns the token/user state and listens for `auth:unauthorized` (fired
  from `api.js`, which can't call `useNavigate` itself since it's outside the React tree).
  `isAuthenticated` is a plain `Boolean(token)` check — no round-trip to `/api/auth/me` to
  validate it. A stale-but-present token will look "authenticated" until the first real
  API call 401s, at which point the interceptor above kicks in and redirects.
- **`DashboardPage` is a placeholder** ("Logged in as X — role: Y", a logout button) —
  it exists only so Phase 1's login→redirect flow had somewhere real to land. Phase 2
  replaces its contents with actual stats; the route (`/`) and the fact that it's behind
  `PrivateRoute` don't need to change.
- **Verified live in a real browser** (Vite dev server via `preview_start`, not just a
  production build): unauthenticated `/` correctly redirects to `/login`; submitting bad
  credentials shows the alert and re-enables the form; a token written to localStorage
  correctly grants access to the dashboard and renders the stored user's name/email/role;
  logout clears both localStorage keys and returns to `/login`. The one thing *not*
  verified end-to-end is an actual successful `POST /api/auth/login` round-trip against a
  running backend — this sandbox can start the Vite dev server but still cannot start the
  Spring Boot backend (see [[jdk17-required-for-backend]]), and wiring MySQL credentials
  into a committed `.claude/launch.json` isn't safe to do here. Worth one manual
  full-stack login check on the user's own machine before Phase 2.

Frontend Phase 2 landed (Dashboard stats, Inquiries table/filter/pagination, create/edit
form, detail modal with inspection history, delete confirmation). Notes for whoever picks
up Phase 3, since Inspection management follows the exact same patterns:

- **Edit only ever sends `{status, notes}`, matching `PUT /api/inquiry/{id}`'s real
  contract exactly** (see Phase 4) — `InquiryForm` renders a completely different field
  set in edit mode (status + notes) vs. create mode (customer info + project type +
  message), rather than showing customer-info fields that a PUT would silently ignore.
  The plan's Phase 2 acceptance criteria says "edit inquiry details and status," which
  reads like customer-info editing should be possible — it isn't, today. If that's
  actually wanted, it needs a new backend endpoint/DTO (reopening "finished" Phase 4),
  which wasn't done here since it wasn't asked for explicitly. Flagging it rather than
  silently deciding either way.
- **`Inquiry.projectType` dropdown options are the exact canonical backend display
  strings** ("Flooring - Tile, Marble & Mosaic", not the shorter "Flooring" that
  `index.html`'s own quote-request form submits). This matters for round-tripping: GET
  responses always echo the canonical label, so an edit form's pre-filled dropdown has to
  use the same strings its own `<option>`s offer, or the selected value won't match any
  option.
- **The backend has no `?inquiryId=` filter on `/api/inspection`** (Phase 5 only supports
  `status`/date-range). `InquiryDetailModal`'s "Inspection History" section works around
  this by fetching a `limit=100` page and filtering client-side
  (`services/inspections.js`'s `listInspectionsForInquiry()`) — correct today at this
  admin tool's expected per-inquiry inspection count, but not something that scales. If
  Phase 3 needs inquiry-scoped inspection queries anywhere else, add a real backend filter
  rather than copying this workaround a second time.
- **Dashboard stats reuse the existing pagination contract** rather than needing a new
  endpoint: `GET /api/inquiry?limit=1&status=NEW` returns `total` as the *full* matching
  count regardless of the requested page size (`Page.getTotalElements()`, proven by
  Phase 4's own pagination tests), so `getInquiriesCount()`/`getInspectionsCount()` just
  read `.total` off a minimal-size request. No `/api/stats` endpoint exists or is needed.
- **A failed refetch doesn't clear existing data** — `InquiriesPage`'s error state is
  deliberately checked as `error && inquiries.length === 0` before suppressing the table,
  not just `error` alone. A delete-triggered refetch that fails should keep showing the
  last-known-good rows alongside the error, not blank the screen; only a first-load
  failure (genuinely nothing to show) suppresses the table. Caught this while testing
  against no backend, before it shipped.
- **Verified live against a real (backend-less) browser session**, same approach as Phase
  1: confirmed exact request URLs/params for the stats queries, the status filter, and
  the create POST via network inspection; confirmed the create form preserves entered
  values and re-enables its Save button after a failed submit; confirmed the table
  correctly disappears (not "No inquiries found") when the initial load fails. Not
  interactively tested: Edit/Delete/Detail-modal row actions, since no backend means no
  rows exist to click - these reuse the exact same modal/error-handling patterns already
  verified for Create, just with different field sets, confirmed by direct code review
  rather than a click-through.

---

## 8. Estimated Timeline

⚠️ **These hour estimates are rough guesses, not measurements.** Use them for relative sizing (phase 4 is bigger than phase 1), not for scheduling or commitments.

| Phase | Task | Model | Hours | Difficulty |
|-------|------|-------|-------|------------|
| 1 | Backend setup | Haiku 4.5 | 2 | Easy |
| 2 | Entities & repos | Haiku 4.5 | 3 | Easy |
| 3 | JWT auth | **Opus 5** | 4 | Medium |
| 4 | Inquiry CRUD | Sonnet 5 | 5 | Medium |
| 5 | Inspection CRUD | Sonnet 5 | 5 | Medium |
| 6 | Testing & errors | Sonnet 5 | 4 | Medium |
| - | Backend subtotal | - | **23 hours** | - |
| 1 | Frontend setup & login | Sonnet 5 | 3 | Easy |
| 2 | Inquiry management | Sonnet 5 | 6 | Medium |
| 3 | Inspection management | Sonnet 5 | 6 | Medium |
| 4 | Polish & responsive | Sonnet 5 | 4 | Medium |
| - | Frontend subtotal | - | **19 hours** | - |
| **Total** | - | - | **42 hours** | - |

Rough timeline: 2-3 weeks part-time, 1 week full-time

---

## 9. Notes for Whoever Implements This

**Respect the model switches.** See §0 for the full strategy. If you are a model working through this plan and you reach a 🛑 SWITCH banner, stop and tell the user to switch — don't carry on into the next phase on the wrong model. Phase 3 in particular should not be written by a small model.

**Keep it simple:**
- Start with Phase 1-2 (setup, entities) — get comfortable
- Phase 3 (JWT) is the trickiest part; take time here
- Phases 4-5 are repetitive CRUD; use phase 4 as template for phase 5
- Frontend is mostly copy-paste tables/forms; Tailwind docs are your friend
- Test as you go (don't wait until the end)

**Two things this plan left open:**
- ~~**How does Alberto's account get created?**~~ **RESOLVED in Phase 3.** `DataSeeder`
  creates it on startup, hashing with the same `PasswordEncoder` bean login verifies
  against. It only ever *creates* — a restart can never reset a password that was
  changed. Set `ADMIN_SEED_PASSWORD` to choose the password; otherwise a random one is
  generated and logged once at WARN level.
- ~~**`scheduled_date DATETIME` + `scheduled_time TIME` in the schema is redundant**~~ **RESOLVED.** Collapsed to a single `scheduledDate DATETIME` on the `Inspection` entity before Phase 5 started, so `InspectionService` and its DTO are written against one field from the outset. `InspectionDTO`'s request/response should carry one ISO-8601 datetime string (e.g. `"2026-08-10T14:00:00"`), not a separate date and time.

**Resources:**
- Spring Boot docs: https://spring.io/projects/spring-boot
- JPA/Hibernate: https://spring.io/projects/spring-data-jpa
- JWT in Spring: https://www.baeldung.com/spring-security-oauth-jwt
- React docs: https://react.dev
- Tailwind CSS: https://tailwindcss.com/docs
- Axios: https://axios-http.com/docs/intro

**Ask questions when stuck** — don't guess on security (auth, password hashing, CORS)

