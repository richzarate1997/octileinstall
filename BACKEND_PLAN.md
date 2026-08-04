# OC Tile Install — Backend API & Admin Dashboard Development Plan

**Status:** Planning phase  
**Target:** Java Spring Boot REST API + React Admin Dashboard for inquiry & inspection logging  
**Architecture:** Separate `backend/` folder, Spring Boot, MySQL, JWT auth

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
│   └── .env                         (API_URL=http://localhost:8080/api)
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
CREATE TABLE inspections (
  id INT PRIMARY KEY AUTO_INCREMENT,
  inquiry_id INT,
  scheduled_date DATETIME NOT NULL,
  scheduled_time TIME,
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

### Phase 1: Project Setup & Database

**Tasks:**
- [ ] Initialize Maven project with Spring Boot 3.x starter
- [ ] Add dependencies: Spring Web, Spring Data JPA, MySQL Driver, Spring Security, JWT, Lombok, Validation
- [ ] Configure `application.properties` with MySQL connection, JPA settings, JWT secret
- [ ] Create database schema in MySQL
- [ ] Test connection with simple H2 in-memory DB first (optional, for TDD)

**Acceptance:**
- Project builds without errors (`mvn clean package`)
- Database tables created and accessible via MySQL client
- Spring Boot app starts on `localhost:8080`

---

### Phase 2: Data Entities & Repositories

**Tasks:**
- [ ] Create `User` entity (id, email, passwordHash, fullName, role, timestamps)
- [ ] Create `Inquiry` entity (customer info, project type, status, notes, timestamps, FK to User)
- [ ] Create `Inspection` entity (inquiryId, scheduledDate, scheduledTime, location, status, notes, timestamps)
- [ ] Create JPA repositories for all three entities
- [ ] Add Lombok annotations (`@Entity`, `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- [ ] Add validation annotations (`@NotBlank`, `@Email`, `@NotNull`, etc.)

**Acceptance:**
- All entities compile and map to database tables
- Repositories can be injected in services (no compilation errors)
- Can run a quick integration test to save/retrieve a User

---

### Phase 3: Authentication (JWT)

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

### Phase 4: Inquiry Endpoints (CRUD)

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
  - Request: `{ inquiryId, scheduledDate, scheduledTime, location }`
  - Response: `{ id, inquiryId, scheduledDate, ..., status, createdAt }`

**Acceptance:**
- POST `/api/inspection` with valid inquiryId creates inspection
- GET `/api/inspection?from=2026-08-01&to=2026-08-31` filters by date range
- PUT `/api/inspection/1` reschedules or updates status
- Attempting to create inspection for non-existent inquiry returns 404

---

### Phase 6: Error Handling & Testing

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

### Phase 1: Setup & Login

**Tasks:**
- [ ] Create React app with Vite (faster than CRA)
- [ ] Install dependencies: axios, react-router-dom, date-fns, tailwindcss
- [ ] Create `.env` file with `VITE_API_URL=http://localhost:8080/api`
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
  "scheduledDate": "2026-08-10",
  "scheduledTime": "14:00",
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
   - `mvn spring-boot:run` (starts on 8080)
   - MySQL running locally on port 3306

2. **Set up frontend locally**
   - Clone/pull repo, `cd admin-dashboard`
   - `npm install`
   - `npm run dev` (starts on 5173)
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

Backend:
- [ ] 1.1 Maven project setup
- [ ] 1.2 MySQL connection configured
- [ ] 2.1 Entity classes (User, Inquiry, Inspection)
- [ ] 2.2 JPA repositories
- [ ] 3.1 JWT token provider
- [ ] 3.2 Auth filter & security config
- [ ] 3.3 Auth controller & service
- [ ] 4.1 Inquiry controller & service
- [ ] 4.2 Inquiry DTOs
- [ ] 5.1 Inspection controller & service
- [ ] 5.2 Inspection DTOs
- [ ] 6.1 Global exception handler
- [ ] 6.2 Integration tests
- [ ] 6.3 Postman collection

Frontend:
- [ ] 1.1 React + Vite setup
- [ ] 1.2 Login page
- [ ] 1.3 Auth context & private routes
- [ ] 2.1 Dashboard page
- [ ] 2.2 Inquiries page & table
- [ ] 2.3 Inquiry form (create/edit)
- [ ] 3.1 Inspections page
- [ ] 3.2 Inspection form (create/edit/reschedule)
- [ ] 4.1 Tailwind styling
- [ ] 4.2 Responsive design
- [ ] 4.3 Toast notifications & confirmations

---

## 8. Estimated Timeline

| Phase | Task | Hours | Difficulty |
|-------|------|-------|------------|
| 1 | Backend setup | 2 | Easy |
| 2 | Entities & repos | 3 | Easy |
| 3 | JWT auth | 4 | Medium |
| 4 | Inquiry CRUD | 5 | Medium |
| 5 | Inspection CRUD | 5 | Medium |
| 6 | Testing & errors | 4 | Medium |
| - | Backend subtotal | **23 hours** | - |
| 1 | Frontend setup & login | 3 | Easy |
| 2 | Inquiry management | 6 | Medium |
| 3 | Inspection management | 6 | Medium |
| 4 | Polish & responsive | 4 | Medium |
| - | Frontend subtotal | **19 hours** | - |
| **Total** | - | **42 hours** | - |

Realistic timeline: 2-3 weeks part-time, 1 week full-time

---

## 9. Notes for Smaller Models

**Keep it simple:**
- Start with Phase 1-2 (setup, entities) — get comfortable
- Phase 3 (JWT) is the trickiest part; take time here
- Phases 4-5 are repetitive CRUD; use phase 4 as template for phase 5
- Frontend is mostly copy-paste tables/forms; Tailwind docs are your friend
- Test as you go (don't wait until the end)

**Resources:**
- Spring Boot docs: https://spring.io/projects/spring-boot
- JPA/Hibernate: https://spring.io/projects/spring-data-jpa
- JWT in Spring: https://www.baeldung.com/spring-security-oauth-jwt
- React docs: https://react.dev
- Tailwind CSS: https://tailwindcss.com/docs
- Axios: https://axios-http.com/docs/intro

**Ask questions when stuck** — don't guess on security (auth, password hashing, CORS)

