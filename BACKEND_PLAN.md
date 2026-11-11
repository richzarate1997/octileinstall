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
- [x] 3.1 Inspections page
- [x] 3.2 Inspection form (create/edit/reschedule)
- [x] 4.1 Tailwind styling
- [x] 4.2 Responsive design
- [x] 4.3 Toast notifications & confirmations

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

Frontend Phase 3 landed (Inspections page: table with color-coded status, status/date-range
filters, pagination; create/edit form; a separate Reschedule action; Mark Complete;
delete confirmation). Notes for whoever picks up Phase 4:

- **Reschedule is a deliberately separate, minimal modal from Edit**, even though both
  end up calling the identical `PUT /api/inspection/{id}` — the backend draws no
  distinction between "reschedule" and "general edit" (`InspectionUpdateRequest` bundles
  status/notes/scheduledDate together), but the plan lists them as two distinct row
  actions, and "I need to move this appointment" vs. "I want to edit this record" are
  different user intents worth different UI even when the underlying call is the same.
  `InspectionRescheduleModal` is just a date/time field + Save; `InspectionForm`'s edit
  mode has all three fields together.
- **Mark Complete is a genuine one-click action** (`updateInspection(id, {status:
  'COMPLETED'})`, no modal) — hidden once an inspection is already COMPLETED, matching
  Phase 2's per-row conditional-action pattern.
- **The "linked inquiry" column shows a customer name, not a bare id** — `InspectionDTO`
  only returns `inquiryId`. `InspectionsPage` fetches inquiries (`limit=100`, same scale
  assumption as Phase 2's `listInspectionsForInquiry()`) alongside inspections and builds
  an id→name lookup map, falling back to `Inquiry #N` if a mapping is missing (e.g. a
  deleted inquiry an inspection still references).
- **The create form's inquiry picker is a `<select>` of up to 100 existing inquiries**
  (`inquiry.customerName — inquiry.projectType (inquiry.status)`), not a raw id input —
  `POST /api/inspection` requires a valid `inquiryId` and 404s otherwise, so picking from
  real records avoids that entirely. Save is disabled if no inquiry can be selected
  (e.g. the picker's own fetch failed, or there are zero inquiries yet) — confirmed live.
- **Considered but not built**: a "Schedule Inspection" button inside
  `InquiryDetailModal` (Phase 2), pre-filling the inquiry so users don't have to search
  for it in the standalone picker. Would reuse `InspectionForm` via an optional
  `initialInquiryId` prop — small, but it means touching a Phase 2 file for a Phase 3
  feature not explicitly asked for, so it was left as a flagged possible enhancement
  rather than built silently.
- **`utils/datetime.js`'s `toLocalInputValue()`** converts an ISO datetime to the
  `YYYY-MM-DDTHH:mm` shape `<input type="datetime-local">` requires — shared by both
  `InspectionForm` (edit mode) and `InspectionRescheduleModal`. Verified directly
  (zero-padding, midnight) rather than assumed correct.
- **Verified live against a real (backend-less) browser session**: confirmed the
  Inspections nav link and page render correctly; confirmed the inquiry-picker's error
  state renders an alert instead of a broken/empty dropdown when its own fetch fails, and
  that Save is correctly disabled in that state; confirmed status+from+to filters combine
  into one correctly-shaped request URL. Not interactively tested: Edit/Reschedule/Mark
  Complete/Delete row actions, since no backend means no rows exist to click — same
  limitation as Phase 2, same reasoning (these reuse already-verified patterns).

Frontend Phase 4 landed (Tailwind brand styling, sidebar navigation, responsive layout,
loading spinners, toast notifications). This is the last frontend phase in the plan —
the admin dashboard build is now complete end to end. Notes for anyone touching styling
or layout later:

- **The dashboard's colors/fonts/radius are the main site's real design tokens, not a
  generic dark theme.** Pulled directly from `css/styles.css`'s `:root` block: navy
  `#0a0e1a`/`#111827`/`#1c2535`, blue `#1a6fd4`/`#3d9bff`, grey `#8a9ab5`, a near-flat 2px
  border radius, and the same three Google Fonts (Bebas Neue / Barlow / Barlow Condensed)
  at the same weights. Implemented in `tailwind.config.js` by **overriding the values of
  Tailwind's existing `slate`/`blue` shade keys** (950/900/800/700/500/400/300 and
  600/500/400 respectively) rather than adding new color names — every `bg-slate-950`,
  `text-slate-400`, `bg-blue-600`, etc. already written across every component
  automatically became brand-correct with zero component-file edits. If a shade not
  currently in that override map gets used later (e.g. `slate-600` or `blue-300`), it
  will silently fall back to Tailwind's stock gray/blue instead of the brand palette —
  add it to the map in `tailwind.config.js` rather than hand-picking a hex value inline.
- **Fonts are applied globally via `index.css`'s `@layer base`**, not per-element
  classNames: `body` → Barlow, `h1`/`h2` → Bebas Neue, every `<button>` → Barlow
  Condensed (matching that font's own documented purpose in `css/styles.css` as "nav/
  buttons/labels/eyebrows"). Deliberately **not** applied to `h3`/`h4` — unlike the
  marketing site, this app's `h3`/`h4` are small in-modal sub-labels ("Message", "Notes"),
  and Bebas Neue's bold condensed all-caps style stops being legible that small.
- **The nav is a real sidebar now** (`components/Layout.jsx`), not Phase 2's placeholder
  top bar: persistent 256px sidebar at `lg:` (1024px) and up, a top bar + slide-down
  drawer below that. The breakpoint is deliberately `lg`, not `md` — a 768px-wide
  *portrait* tablet gets the mobile hamburger pattern (verified usable at that width),
  not a cramped persistent sidebar; landscape tablets and up get the full sidebar.
- **Toasts are scoped to action *outcomes* only** (`context/ToastContext.jsx`,
  `useToast()`) — create/update/delete/reschedule/mark-complete success, plus delete and
  mark-complete failure (both migrated off the page-level `error` banner they used before
  this phase, since those are one-action failures, not "the page is broken"). They
  deliberately do **not** replace: the persistent inline banner a full list/dashboard
  load failure uses (an ephemeral toast is wrong for "here's why this page is empty"),
  or inline form-validation errors inside modals (the user is already looking at the
  form; a floating duplicate would be noise). If a new page/action is added later, follow
  this same split rather than defaulting everything to toasts.
- **Tables get horizontal scroll on narrow viewports, not a card-layout transform** —
  `InquiryTable`/`InspectionTable` are wrapped in `overflow-x-auto` with an explicit
  `min-w-[640px]`/`min-w-[720px]` on the `<table>`, so mobile gets a real, usable
  horizontal scroll instead of illegibly crushed columns. A full responsive
  table→card transform was considered and skipped as disproportionate to what "usable on
  mobile" requires; revisit if the tables grow enough columns that horizontal scroll
  itself becomes the UX problem.
- **Verified live in a real browser**, more rigorously than prior phases since this is
  entirely CSS/layout work the missing backend doesn't block checking: read *computed*
  styles via `getComputedStyle` to confirm exact hex/font/radius matches (not eyeballed
  from a screenshot) — e.g. the active nav item resolved to `rgb(26, 111, 212)`, exactly
  `#1a6fd4`. Checked real 375px (mobile), 768px (tablet), and 1280px (desktop) viewports.
  Confirmed the sidebar/drawer toggle, a modal's field rendering, and the toast
  container's visual styling. One tooling wrinkle hit and resolved: the `computer` tool's
  click action hung repeatedly in this session with delayed/queued delivery (a click
  landing seconds after being issued) — worked around via direct `MouseEvent` dispatch
  through `javascript_tool`, which isn't subject to the same delay; the underlying
  component logic was confirmed correct either way. Not verified: a toast fired through
  real app state (create/delete succeeding) — no live backend to produce that success
  response in this sandbox, so only the `ToastContainer` markup/CSS was directly checked,
  not the state-management trigger path.

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


---

## 10. Post-launch addendum: public site scoped to informative-only Phase 1 (2026-08-18)

Added after all 10 original phases were complete. This went through two revisions in one
day, in response to a gap found while writing `TEST_PLAN.md` and then a deliberate scope
cut - both are recorded here so the "why" isn't lost:

1. The public site's quote form originally posted to a placeholder Formspree endpoint
   (`FORM_ID` was never replaced with a real one), so every real customer inquiry was
   silently lost.
2. A fix was built: first an unauthenticated backend endpoint the form posted JSON to
   directly, then (after feedback) scaled back to just finishing the original Formspree
   wiring, keeping the backend endpoint as a separate, disconnected "Phase 2."
3. **Final decision: cut the form entirely for Phase 1.** The public site is
   **informative-only** - no quote form, no email step, no lead-capture mechanism of any
   kind. A customer reaches Alberto by calling, texting, or DMing Instagram directly, via
   plain links. This is intentionally the simplest possible Phase 1: zero moving parts,
   nothing that can silently break, no third-party account (Formspree) required to launch.

### Phase 1 (live now) - what changed
- `index.html` - the `#quote` section no longer contains a `<form>`. It's a static panel
  with two buttons: a `tel:+17146243132` link and an `https://instagram.com/octileinstall`
  link. The hero's primary CTA button now reads "Call or Text for a Free Quote" (still
  anchors to `#quote`) instead of implying something gets "sent."
- `js/main.js` - the entire quote-form IIFE (validation, honeypot handling, fetch/submit
  logic) was deleted. The file now only handles the mobile nav, hero mosaic, scroll-reveal,
  footer year, and gallery lightbox - all unrelated to any form.
- `css/styles.css` - the `.quote-form`, `.form-row--honeypot`, `.quote-form-message`, and
  related rules (dead code once the `<form>` was removed) were replaced with one small
  `.quote-cta` rule for the two-button panel.
- Net effect: the public site has **no client-side JS dependency and no third-party
  dependency** for its primary call-to-action. It cannot "go dead" the way the Formspree
  placeholder did, because there's nothing left to misconfigure.

### Phase 2 (built, tested, not connected - unchanged from before)
Everything below still exists in `backend/`, still passes its test suite, and is still
completely unreferenced by the public site. Nothing in this revision touched it:
- `POST /api/public/inquiry` (`PublicInquiryController`) - the only unauthenticated write
  endpoint in the API. `SecurityConfig` permits this one path/method explicitly; everything
  else is still default-deny.
- `PublicInquiryCreateRequest` / `PublicInquiryResponse` - separate DTOs from the admin
  `InquiryCreateRequest`/`InquiryDTO`. The request carries `@Size` caps the admin form has no
  need for (an anonymous caller is a different threat model than a staff member), and a
  `company` honeypot field. The response is a minimal `{received, message}` ack - never the
  row id, status, or timestamps - and is byte-for-byte identical whether the submission was
  real or silently dropped as a honeypot hit, so a bot probing the endpoint can't distinguish
  detection from acceptance.
- `RateLimiterService` - a simple in-memory, per-IP, fixed-window limiter (default: 5
  requests / 15 minutes, tunable via `app.public-inquiry.rate-limit.*`). Single-instance only
  by design; see the class javadoc for what a multi-instance deployment would need instead.
  Exceeding it throws the new `TooManyRequestsException`, mapped to 429 by
  `GlobalExceptionHandler` like every other error here.
- `InquiryService.createPublic(...)` - the unauthenticated counterpart to `create(...)`, with
  `createdByUser` left null since there's no staff member to attribute the row to.
- Full integration test coverage (`PublicInquiryControllerIntegrationTest`): success,
  validation failures, honeypot drop, rate-limit exceeded, per-IP isolation. All passing as
  of this writing (`mvn clean test`).
- `app.cors.allowed-origins` in `application.properties` already lists the public site's
  production domains, and `app.public-inquiry.rate-limit.*` is already tunable - both ready
  for Phase 2 whenever it's connected.

**To connect Phase 2 later:** decide on a lead-capture UI for the public site again (a form,
or something else), point its submission at `<backend-origin>/api/public/inquiry` with a
JSON body (`customerName`, `customerEmail`, `customerPhone`, `projectType`, `city`,
`message`), and confirm the deployed backend origin is in `app.cors.allowed-origins`. The
endpoint, DTOs, service method, rate limiter, and tests are already in place - this is UI
work plus wiring, not new backend work.

**Still open / deliberately out of scope for this change:**
- No admin-side way to fix a mistyped phone/email, etc. - unrelated, pre-existing (see
  `TEST_PLAN.md` defect #11).
- Role enforcement (`TEST_PLAN.md` defect #2) and login rate limiting (defect #3) were not
  touched - neither revision of this addendum concerns the authenticated-side findings.
- `X-Forwarded-For` handling for the Phase 2 rate limiter was deliberately not added - see
  `PublicInquiryController.clientIp()`'s javadoc. Needed only once this sits behind a proxy,
  which is moot until Phase 2 is actually connected.
