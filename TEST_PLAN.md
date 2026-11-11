# OctileInstall — Full Validation Test Plan

Manual acceptance + security test plan covering the **public customer site**
(`index.html`, `js/main.js`), the **admin dashboard** (`admin-dashboard/`), and the
**Spring Boot API** (`backend/`).

Work top to bottom. Each case has an ID so you can report failures as "TC-B14 failed".
Priority: **P0** = blocks launch, **P1** = fix before Alberto uses it daily, **P2** = polish.

> Several cases below are expected to fail today — they are marked **⚠ known/suspected
> defect** with the reason. See [§8 Pre-identified defects](#8-pre-identified-defects)
> for the summary; don't spend time diagnosing those, just confirm the behavior.

---

## Contents

1. [Preflight — environment setup](#1-preflight--environment-setup)
2. [Backend API (TC-B)](#2-backend-api-tc-b)
3. [Admin dashboard UI (TC-A)](#3-admin-dashboard-ui-tc-a)
4. [Public customer site (TC-C)](#4-public-customer-site-tc-c)
5. [Security (TC-S)](#5-security-tc-s)
6. [Cross-cutting: resilience, data integrity, timezones (TC-X)](#6-cross-cutting-tc-x)
7. [Production readiness checklist (TC-P)](#7-production-readiness-tc-p)
8. [Pre-identified defects](#8-pre-identified-defects)
9. [Regression matrix](#9-regression-matrix)

---

## 1. Preflight — environment setup

Do this once before any test run. `curl` examples assume **Git Bash**; in PowerShell use
`curl.exe` (plain `curl` is aliased to `Invoke-WebRequest`).

| ID | Step | Expected |
|---|---|---|
| TC-E1 | `java -version` with `JAVA_HOME` pointed at JDK 17 | 17.x. The machine default is Java 8 and Maven will fail without this. |
| TC-E2 | MySQL running, database `octileinstall` exists, creds match `DB_USERNAME`/`DB_PASSWORD` | Connects |
| TC-E3 | `cd backend && ./mvnw test` | All tests green (auth, inquiry, inspection, exception-handler, JWT, seeder suites) |
| TC-E4 | `cd backend && ./mvnw spring-boot:run` | Starts on **8081**. Watch the log for the seeded admin banner. |
| TC-E5 | First-ever start: capture the generated one-time admin password from the log banner | Printed once; not recoverable. Alternatively set `ADMIN_SEED_PASSWORD` before first start. |
| TC-E6 | Restart the backend a second time | Log says "already exists; leaving it untouched" — password is **not** reset |
| TC-E7 | `cd admin-dashboard && npm install && npm run dev` | Vite on **5177** (must match `app.cors.allowed-origins`) |
| TC-E8 | Serve the public site (`npx serve .`) and open it | Loads without console errors |

Save a token for the API cases:

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login -H 'Content-Type: application/json' -d '{"email":"alberto@octileinstall.com","password":"YOUR_PASSWORD"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
```

---

## 2. Backend API (TC-B)

### 2.1 Authentication

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-B1 | P0 | `POST /api/auth/login` with correct credentials | 200 + `{token, expiresIn: 604800, userId, email, fullName, role:"ADMIN"}` |
| TC-B2 | P0 | Login with wrong password | 401, message exactly `Invalid email or password` |
| TC-B3 | P0 | Login with an email that doesn't exist | 401, **identical** message to TC-B2 (no account enumeration) |
| TC-B4 | P1 | Login with mixed case + whitespace: `"  ALBERTO@OctileInstall.com  "` | 200 — email is trimmed and lowercased |
| TC-B5 | P1 | Login with `{}` / missing password | 400, `fieldErrors` naming `email` and/or `password` |
| TC-B6 | P1 | Time 10 unknown-email logins vs 10 wrong-password logins | Both ~equally slow (BCrypt dummy-hash compare). A fast "unknown email" path = timing enumeration |
| TC-B7 | P0 | `GET /api/auth/me` with the token | 200 + userId/email/fullName/role |
| TC-B8 | P0 | `GET /api/auth/me` with no `Authorization` header | 401 `{status:401,error:"Unauthorized",message:"Authentication required"}` |
| TC-B9 | P0 | `GET /api/auth/me` with `Bearer garbage` | 401, same shape |
| TC-B10 | P0 | Token with the signature altered (flip one char of the 3rd segment) | 401 — never 200 |
| TC-B11 | P0 | Token with the payload altered to another `sub` but the original signature | 401 |
| TC-B12 | P1 | `Authorization: Bearer ` (empty token) | 401, not 500 |
| TC-B13 | P1 | `Authorization: alberto@...` (no `Bearer ` prefix) | 401 |
| TC-B14 | P1 | Delete the admin row in MySQL, then reuse a previously valid token | 401 — the filter re-reads the user per request, so deletion takes effect immediately |
| TC-B15 | P2 | Expired token (temporarily set `app.jwt.expiration=1000`, log in, wait 2s, call `/me`) | 401 |
| TC-B16 | P0 | Start the app with `JWT_SECRET` shorter than 32 bytes | App **refuses to start** with a clear message |
| TC-B17 | P1 | `GET /actuator/health` unauthenticated | 200 `{"status":"UP"}` |
| TC-B18 | P1 | Any other `/actuator/*` endpoint unauthenticated | 401 or 404 — never an exposed env/beans/heapdump |

### 2.2 Inquiries

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-B20 | P0 | `POST /api/inquiry` with `{customerName, customerEmail}` only | 201, `status:"NEW"`, `createdBy` = your userId, timestamps set |
| TC-B21 | P0 | POST with blank `customerName` | 400 + `fieldErrors.customerName = "Customer name is required"` |
| TC-B22 | P0 | POST with `customerEmail:"not-an-email"` | 400 + `fieldErrors.customerEmail` |
| TC-B23 | P0 | POST with no `customerEmail` at all | 400 (required) |
| TC-B24 | P1 | POST with `projectType:"Custom Shower"` | 201, echoed back as `"Custom Shower"` (display label, not `CUSTOM_SHOWER`) |
| TC-B25 | P1 | POST with `projectType:"CUSTOM_SHOWER"` | 201 — the enum's `@JsonCreator` accepts the constant name too |
| TC-B26 | P1 | POST with `projectType:"Mosaic Floor"` (unknown) | 400 `Malformed request body` — **not** 500 |
| TC-B27 | P1 | Verify in MySQL: `SELECT project_type FROM inquiries` | Stored as `CUSTOM_SHOWER` while the API speaks `"Custom Shower"` |
| TC-B28 | P0 | `GET /api/inquiry` | `{data:[...], total:N}`, newest first (`createdAt` DESC) |
| TC-B29 | P0 | `GET /api/inquiry?status=NEW` | Only NEW rows; `total` reflects the **filtered** count, not the table count |
| TC-B30 | P1 | `GET /api/inquiry?status=BOGUS` | 400 `Invalid value for parameter 'status'` |
| TC-B31 | P1 | `GET /api/inquiry?limit=abc` | 400, not 500 |
| TC-B32 | P1 | `?limit=0`, `?limit=-5`, `?offset=-1` | Falls back to defaults (limit 20, offset 0), no error |
| TC-B33 | P1 | With 25+ inquiries: `?limit=10&offset=10` | Rows 11–20, no overlap with `offset=0` |
| TC-B34 | P2 | `?limit=10&offset=15` (**unaligned**) | Rounds down to page 1 (rows 11–20) — documented behavior; verify it never silently skips rows |
| TC-B35 | P1 | `?limit=100000` | Confirm response time / memory is tolerable — there is no server-side max |
| TC-B36 | P0 | `GET /api/inquiry/{id}` valid | 200, full DTO |
| TC-B37 | P0 | `GET /api/inquiry/999999` | 404 `{status:404,error:"Not Found",message:"Inquiry 999999 not found"}` |
| TC-B38 | P1 | `GET /api/inquiry/abc` | 400 `Invalid value for parameter 'id'` |
| TC-B39 | P0 | `PUT /api/inquiry/{id}` `{"status":"CONTACTED"}` | 200, status changed, **notes untouched**, `updatedAt` bumped |
| TC-B40 | P0 | `PUT` `{"notes":"Called 8/18"}` | 200, notes set, **status untouched** |
| TC-B41 | P1 | `PUT` `{}` | 200, nothing changes (partial-update contract) |
| TC-B42 | P1 | `PUT` `{"customerName":"Hacked"}` | 200 but name **unchanged** — field silently ignored (see §8) |
| TC-B43 | P1 | `PUT` `{"status":"BOGUS"}` | 400 `Malformed request body` |
| TC-B44 | P0 | `PUT /api/inquiry/999999` | 404 |
| TC-B45 | P0 | `DELETE /api/inquiry/{id}` | 204, row gone; second DELETE → 404 |
| TC-B46 | P0 | **DELETE an inquiry that has an inspection attached** | ⚠ Expect a 500 `An unexpected error occurred` from the FK constraint. Should be a 409/400 with a usable message. |
| TC-B47 | P1 | POST with a 10,000-char `message` | 201 (TEXT column) — confirm no truncation on read-back |
| TC-B48 | P1 | POST with a 500-char `customerName` | Confirm behavior: VARCHAR(255) → expect a DB error surfacing as 500. Should be a 400. |
| TC-B49 | P1 | POST with unicode/emoji in name and message | Round-trips intact (check the MySQL charset is utf8mb4) |

### 2.3 Inspections

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-B60 | P0 | `POST /api/inspection` `{inquiryId, scheduledDate:"2027-01-15T10:00", location}` | 201, `status:"SCHEDULED"` |
| TC-B61 | P0 | POST with a **past** `scheduledDate` | 400 `Scheduled date must be in the future` |
| TC-B62 | P0 | POST with `scheduledDate` = now + 5 seconds | 201 (boundary is strictly-after-now) |
| TC-B63 | P0 | POST with an `inquiryId` that doesn't exist | 404 `Inquiry N not found` |
| TC-B64 | P0 | POST with no `inquiryId` / no `scheduledDate` | 400 + `fieldErrors` |
| TC-B65 | P1 | POST with `scheduledDate:"tomorrow"` | 400 `Malformed request body` |
| TC-B66 | P1 | POST two inspections at the same time for the same inquiry | 201 both — **no double-booking check exists**. Confirm that's acceptable to Alberto. |
| TC-B67 | P0 | `GET /api/inspection` | Sorted by `scheduledDate` **ASC** (soonest first) |
| TC-B68 | P0 | `?from=2026-08-01&to=2026-08-31` | Inclusive of both boundary days (`to` covers through 23:59:59.999) |
| TC-B69 | P1 | `?from=` only | Unbounded upper end |
| TC-B70 | P1 | `?to=` only | Unbounded lower end |
| TC-B71 | P1 | `?from=2026-09-01&to=2026-08-01` (inverted) | Empty `data`, `total:0` — no error, no crash |
| TC-B72 | P1 | `?from=08/01/2026` (wrong format) | 400 `Invalid value for parameter 'from'` |
| TC-B73 | P1 | `?from=...&to=...&status=SCHEDULED` | Both filters applied together |
| TC-B74 | P0 | `PUT /api/inspection/{id}` `{"status":"COMPLETED"}` on a **past** inspection | 200 — the future-date rule must **not** apply to a status-only update |
| TC-B75 | P0 | `PUT` `{"scheduledDate":"<future>"}` | 200, rescheduled |
| TC-B76 | P0 | `PUT` `{"scheduledDate":"<past>"}` | 400 `Scheduled date must be in the future` |
| TC-B77 | P0 | `PUT` `{"status":"COMPLETED","scheduledDate":"<its own past date>"}` | ⚠ 400. This is exactly what the dashboard's edit form sends — see TC-A65 and §8. |
| TC-B78 | P1 | `PUT` `{"notes":"..."}` only | 200, schedule and status untouched |
| TC-B79 | P0 | `DELETE /api/inspection/{id}` | 204; second DELETE → 404 |
| TC-B80 | P1 | Delete an inspection, then `GET` its inquiry | Inquiry unaffected |

### 2.4 Error contract

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-B90 | P1 | Malformed JSON body (`{"customerName":`) | 400 `Malformed request body` |
| TC-B91 | P1 | `Content-Type: text/plain` on a POST | 415 or 400 — not 500 |
| TC-B92 | P1 | `PATCH /api/inquiry/{id}` | 405 |
| TC-B93 | P1 | `GET /api/nonexistent` (authenticated) | 401 or 404, JSON — never an HTML Whitelabel page |
| TC-B94 | P0 | Every error response above | Shares the `{status, error, message}` shape (+ `fieldErrors` on `@Valid` failures) |
| TC-B95 | P0 | Any 500 you can trigger | Body says only `An unexpected error occurred` — **no stack trace, SQL, or class names leak**. Full detail appears in the server log. |

### 2.5 Phase 2 (built, not yet live): `POST /api/public/inquiry`

**⚠ Not reachable from the public site at all today.** Phase 1 (live now, see §4) is
informative-only — no form, no lead-capture mechanism, just phone/Instagram links. This
endpoint exists, is fully built, and passes its test suite, but nothing on the site calls
it. Test it directly against the backend (`curl`/Postman), not through the site, until it's
deliberately wired up as a later phase (see `BACKEND_PLAN.md` §10). It remains the **only
unauthenticated write endpoint** in the API — test it with the mindset of "anyone on the
internet can call this," not "an admin is calling this," whenever that day comes.

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-B100 | P0 | POST with no `Authorization` header, valid body | 201, no token needed |
| TC-B101 | P0 | Response body | `{received: true, message: "..."}` only — **no id, status, or timestamp**. Confirm nothing about the internal DTO shape leaks. |
| TC-B102 | P0 | `GET /api/inquiry` as admin right after | The new row is present, `status:"NEW"`, `createdBy:null` |
| TC-B103 | P0 | POST with blank `customerName` / `customerEmail` | 400 + `fieldErrors`, same shape as the admin endpoint |
| TC-B104 | P1 | POST with a 10,000-char `customerName` | 400 `Customer name is too long` — this endpoint has `@Size` caps the admin `InquiryCreateRequest` does not (see §8 defect #6 note: the admin path still lacks this) |
| TC-B105 | P1 | POST with a 6,000-char `message` | 400 `Message is too long` |
| TC-B106 | P0 | POST with `company` (the honeypot) filled | **201, identical response shape to a real submission** — then confirm via `GET /api/inquiry` as admin that **no row was created**. The response must never reveal detection. |
| TC-B107 | P0 | Send **6** requests from the same IP inside 15 minutes (default limit is 5) | Requests 1–5 succeed (201); the 6th returns **429** with the standard `{status,error,message}` shape |
| TC-B108 | P1 | Wait out the window (or use a fresh IP) after hitting the limit | Requests succeed again |
| TC-B109 | P1 | Two different IPs, each sending 5 | Both succeed — the limit is per-IP, not global (confirm no shared bucket) |
| TC-B110 | P1 | Cross-origin `fetch` from the production public-site origin (once `app.cors.allowed-origins` is updated — see TC-P3-equivalent below) | Succeeds; from any other origin, blocked by CORS |
| TC-B111 | P1 | POST with `projectType:"Flooring"` (the public form's actual `<option value>`, not the full display label) | 201 — resolves via the enum constant-name fallback (`FLOORING`), not the display label. This is a real, if slightly fragile, coincidence in `index.html`'s markup — worth knowing if that option's value ever changes. |
| TC-B112 | P2 | `PublicInquiryControllerIntegrationTest` (backend test suite) | All 6 cases (success, honeypot-drop, missing fields, invalid email, rate-limit exceeded, per-IP isolation) pass — already run in this session; rerun after any related change |

---

## 3. Admin dashboard UI (TC-A)

### 3.1 Auth & routing

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-A1 | P0 | Visit `/` logged out | Redirects to `/login` |
| TC-A2 | P0 | Visit `/inquiries` logged out, then log in | Lands on **`/inquiries`**, not `/` (the `from` location is preserved) |
| TC-A3 | P0 | Log in with valid credentials | Redirect to dashboard, sidebar shows Alberto's name |
| TC-A4 | P0 | Log in with a wrong password | Inline red alert `Invalid email or password`; you can retry immediately |
| TC-A5 | P1 | Submit login with empty fields | Native HTML5 required-field validation blocks it; no request fires |
| TC-A6 | P1 | Click Sign In and watch the button | Disabled + spinner + "Signing in…" during the request; **double-click cannot fire two logins** |
| TC-A7 | P0 | Stop the backend, then attempt login | Friendly `Something went wrong. Please try again.` — not a blank page or a raw axios error |
| TC-A8 | P0 | Log in, refresh the page (F5) | Still logged in (token in localStorage) |
| TC-A9 | P0 | Log in, corrupt `localStorage.token` in DevTools, then navigate to Inquiries | The 401 clears storage and bounces to `/login` mid-session |
| TC-A10 | P0 | Click Logout | Returns to `/login`; browser **Back** does not restore the dashboard with data |
| TC-A11 | P1 | After logout, inspect `localStorage` | `token` and `user` both removed |
| TC-A12 | P1 | Two tabs open, log out in tab A, act in tab B | Tab B's next request 401s and redirects. (It won't redirect until you interact — confirm that's acceptable.) |
| TC-A13 | P2 | Deep-link `/inspections` while logged in, then Back/Forward | Navigation works, no state corruption, filters reset sanely |
| TC-A14 | P1 | Visit an unknown route like `/nope` | Document the behavior — there is **no 404 route defined**, so expect a blank page inside the layout |

### 3.2 Dashboard page

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-A20 | P0 | Load with data present | Three cards: Total Inquiries, New Inquiries, Scheduled Inspections — all match the DB |
| TC-A21 | P1 | Load with an empty database | Shows `0`, not blank cards or a crash |
| TC-A22 | P1 | Throttle to Slow 3G and reload | Spinner + "Loading…" appears; no layout jump to a broken state |
| TC-A23 | P0 | Stop the backend and reload | Persistent inline red banner (not a toast); no infinite spinner |
| TC-A24 | P1 | Create a new inquiry, return to the dashboard | Counts update (note: stats load once on mount — confirm whether a refresh is needed) |
| TC-A25 | P2 | Change an inquiry from NEW to CONTACTED, revisit the dashboard | "New Inquiries" decreases |

### 3.3 Inquiries page

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-A30 | P0 | Load the list | Table renders; newest first; `N total` matches |
| TC-A31 | P0 | Empty database | A sensible empty state, not a bare header |
| TC-A32 | P0 | New Inquiry → fill all fields → save | Modal closes, success toast, row appears at the top with status NEW |
| TC-A33 | P0 | New Inquiry → leave name blank → save | Inline error **inside the form** (not a toast); modal stays open; entered data is not lost |
| TC-A34 | P0 | New Inquiry → invalid email → save | Same: inline field error |
| TC-A35 | P1 | New Inquiry → project type dropdown | All six options present and matching the public site's list |
| TC-A36 | P1 | Double-click Save | Only one inquiry created (button disables while submitting) |
| TC-A37 | P1 | Open the form, press **Esc** / click the backdrop | Document the behavior — confirm there's a clear way to cancel without saving |
| TC-A38 | P0 | Edit an inquiry → change status + notes → save | Row updates, success toast |
| TC-A39 | P1 | Inspect what the edit form exposes | ⚠ Only Status and Notes. Confirm with Alberto whether he needs to fix a mistyped phone/email — that's backend work (see §8) |
| TC-A40 | P0 | View (detail modal) | Shows every field including message, city, timestamps, and the linked inspections |
| TC-A41 | P0 | Delete → confirm dialog → Cancel | Nothing is deleted |
| TC-A42 | P0 | Delete → Confirm | Row disappears, toast, `total` decrements |
| TC-A43 | P0 | Delete an inquiry **that has an inspection** | ⚠ Expect an ugly failure (see TC-B46). Confirm the user sees an error toast and the list stays usable — not a blank screen |
| TC-A44 | P0 | Status filter → NEW | Only NEW rows; pagination resets to page 1 |
| TC-A45 | P1 | Filter to a status with no rows | Empty state, `0 total`, no stale rows from the previous filter |
| TC-A46 | P0 | With 25+ inquiries: page forward and back | Correct rows; Prev disabled on page 1; Next disabled on the last page |
| TC-A47 | P1 | Go to page 2, then change the status filter | Jumps back to page 1 (not an empty page 2 of the new result set) |
| TC-A48 | P1 | Stop the backend, then delete a row | Error **toast** appears while the already-loaded table stays visible — the screen must not blank out (this was a real regression once) |
| TC-A49 | P1 | Stop the backend, then reload the page | Persistent banner, no table |

### 3.4 Inspections page

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-A60 | P0 | Load the list | Sorted soonest-first; each row shows the **customer name**, not a bare inquiry id |
| TC-A61 | P0 | New Inspection → pick inquiry, future date/time, location → save | Created, toast, appears in the list |
| TC-A62 | P0 | New Inspection → pick a past date | Inline error `Scheduled date must be in the future`; modal stays open |
| TC-A63 | P1 | New Inspection with **zero inquiries in the DB** | The inquiry dropdown is empty — confirm the form doesn't submit `inquiryId: NaN`. It should tell the user to create an inquiry first |
| TC-A64 | P0 | "Mark complete" on a **past** inspection | Succeeds, toast, status → COMPLETED |
| TC-A65 | P0 | **Edit** a past inspection (change notes only) → save | ⚠ Expect 400 `Scheduled date must be in the future` — the edit form always resends `scheduledDate`. See §8 |
| TC-A66 | P0 | Edit a **future** inspection (change notes) → save | Succeeds |
| TC-A67 | P0 | Reschedule modal → future date | Succeeds, list re-sorts |
| TC-A68 | P0 | Reschedule modal → past date | Inline error, modal stays open |
| TC-A69 | P0 | Date range From/To filter | Boundary days included; results correct |
| TC-A70 | P1 | Set From later than To | Empty result, no crash, and it's obvious why (consider whether the UI should warn) |
| TC-A71 | P1 | Combine status + date range | Both applied |
| TC-A72 | P1 | Clear all filters | Full list returns |
| TC-A73 | P0 | Delete an inspection → confirm | Removed, toast |
| TC-A74 | P1 | With **more than 100 inquiries** in the DB, view inspections | ⚠ Names beyond the first 100 will be missing — the page fetches only `limit=100` for the lookup. Verify the row degrades to the id rather than "undefined" |
| TC-A75 | P1 | Inquiry detail modal → linked inspections | Same 100-row assumption; verify it lists this inquiry's inspections correctly |

### 3.5 Layout, responsiveness, accessibility

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-A80 | P0 | Resize to 375px (mobile) | Sidebar collapses to a toggle; tables scroll horizontally rather than overflowing the page |
| TC-A81 | P0 | Mobile: open the nav, tap a link | Navigates **and** closes the nav |
| TC-A82 | P1 | 768px (tablet) and 1280px (desktop) | No overlap, no clipped buttons, modals fit on screen |
| TC-A83 | P1 | Modals on a 667px-tall phone | Content scrolls inside the modal (`max-h-[85vh]`); action buttons stay reachable |
| TC-A84 | P1 | Keyboard only: Tab through login and each form | Visible focus ring on every control; logical order; Enter submits |
| TC-A85 | P1 | Keyboard: open a modal, press Esc, Tab past the last field | ⚠ No focus trap or Esc handler exists in the modals — verify how bad this is and decide whether it's P1 |
| TC-A86 | P1 | axe DevTools / screen reader pass on each page | Error banners announce (`role="alert"` present); modals lack `role="dialog"`/`aria-modal` — note findings |
| TC-A87 | P2 | Zoom to 200% | Layout remains usable |
| TC-A88 | P2 | Toasts | Auto-dismiss, stack without covering the primary action, and appear only for action outcomes (never page-load failures) |
| TC-A89 | P1 | Browser console across the whole session | Zero React warnings/errors (watch for key and controlled-input warnings) |
| TC-A90 | P1 | `npm run build`, then preview the production bundle | Builds clean; the app works identically from the built output |

---

## 4. Public customer site (TC-C)

**Phase 1 has no form and no lead-capture mechanism of any kind — this is intentional.**
The `#quote` section (and the hero's "Call or Text for a Free Quote" button) is a static
call-to-action panel: a `tel:` link and an Instagram link, nothing else. There is no
`<form>`, no client-side JS to test, and no honeypot. A customer reaches Alberto only by
calling, texting, or DMing directly — there is no async "we'll get back to you" path in this
phase. (The earlier revisions of this plan covered a quote form wired to Formspree, then to
a backend API; both were removed from scope. The backend endpoint from that earlier work
still exists, untouched, deferred as **Phase 2** — see §8 item 10 and `BACKEND_PLAN.md` §10.)

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-C1 | P0 | Hero button "Call or Text for a Free Quote" | ✅ Confirmed 2026-09-02: `href="#quote"`, resolves to the CTA panel |
| TC-C1a | P0 | `#quote` section's "Call or Text (714) 624-3132" button | ✅ Confirmed: `href="tel:+17146243132"` — matches all 5 `tel:` links sitewide (checked via DOM query, not just spot-checked) |
| TC-C1b | P0 | `#quote` section's "Message Us on Instagram" button | ✅ Confirmed: `href="https://instagram.com/octileinstall"`, `target="_blank"`, `rel="noopener"` |
| TC-C2 | P2 | View page source around `#quote` | ✅ Confirmed via DOM query: zero `<form>` elements on the page, `#quote-form`/`#quote-form-message` don't exist, no `[name="company"]` honeypot field anywhere |
| TC-C9 | P0 | All `tel:+17146243132` links sitewide (navbar ×2, hero, `#quote`, `#contact`) | ✅ Confirmed: all 5 identical, `tel:+17146243132` |
| TC-C10 | P1 | Instagram link(s) | ✅ Confirmed: all 4 instances (navbar-adjacent nav, `#quote`, `#contact`, footer) share `target="_blank" rel="noopener"` |
| TC-C11 | P0 | Mobile nav toggle at 375px | ✅ Confirmed via dispatched click events: opens (`aria-expanded` true, `.is-open` added), then closes automatically when a nav link is clicked |
| TC-C12 | P0 | Gallery lightbox: click an image | ✅ Confirmed: opens with the correct image src and caption ("Custom tile installation") |
| TC-C13 | P0 | Lightbox close: X button, backdrop click, **Esc** | ✅ Confirmed: all three close it (tested each independently by reopening between checks) |
| TC-C14 | P1 | Lightbox on a placeholder tile (no image) | Not retested this session (no change in this area) — see original note: shows caption text, not a broken-image icon |
| TC-C15 | P1 | Every gallery/hero image | ✅ Confirmed via Network tab: all 15 gallery images + `alberto.jpg` + `images/logo.jpg` return 200, zero 404s |
| TC-C16 | P1 | The new untracked files in `images/` | Not retested this session — still worth confirming unreferenced large files (including the `.MOV`) don't ship |
| TC-C17 | P1 | Scroll-reveal animations | ✅ Confirmed: `.reveal` elements start with 0 visible, gain `.visible` as they scroll into view (IntersectionObserver firing correctly) |
| TC-C18 | P1 | Footer copyright year | ✅ Confirmed: renders "2026" |
| TC-C19 | P1 | Lighthouse (mobile) | **Not run this session** — no Lighthouse access from this environment. Still open. |
| TC-C20 | P1 | View source: title, meta description, canonical, OG/Twitter tags | ✅ Confirmed present: title, meta description, canonical (`https://www.octileinstall.com/`) all read correctly via DOM |
| TC-C21 | P1 | `og:image` = `images/logo.jpg` | ✅ **Corrected 2026-09-02** — an earlier revision of this plan flagged this as broken (claiming `logo.jpg` only exists at the repo root). That was wrong: `images/logo.jpg` is a real, separate 40KB file and returns 200 OK, confirmed via the Network tab this session. Still worth a social-preview debugger pass before launch, but not the bug previously described. |
| TC-C22 | P1 | `robots.txt` and `sitemap.xml` | ✅ Confirmed 2026-09-02: both return 200, `robots.txt` allows all + points to the sitemap, `sitemap.xml` lists `https://www.octileinstall.com/` matching the canonical domain |
| TC-C23 | P1 | License number `#1040037` and service areas | Factually correct (contractor license claims carry legal weight) |
| TC-C24 | P1 | Cross-browser: Chrome, Safari (iOS), Firefox, Edge | Layout and lightbox behave the same |
| TC-C25 | P2 | Page with JS disabled | Content fully readable, including `#quote`'s call/text and Instagram links — both are plain anchor tags, no JS dependency at all now that there's no form |

---

## 5. Security (TC-S)

### 5.1 Authorization

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-S1 | P0 | Call every endpoint (`GET/POST/PUT/DELETE` on `/api/inquiry`, `/api/inspection`, `/api/auth/me`) with **no token** | Every one returns 401. Only `/api/auth/login` and `/actuator/health` are public |
| TC-S2 | P0 | Same sweep with an **expired/tampered** token | All 401 |
| TC-S3 | P0 | Create a second user with `role='VIEWER'` in MySQL (BCrypt hash), log in as them, then `DELETE /api/inquiry/{id}` | ⚠ **Expect success — roles are not enforced anywhere.** Any authenticated user has full admin power. See §8 |
| TC-S4 | P1 | Demote your user to VIEWER in the DB mid-session, then call an endpoint | Confirm the role is re-read per request (the filter reloads the user), even though nothing acts on it yet |
| TC-S5 | P1 | Copy a token from the browser, click Logout in the UI, then reuse the token from `curl` | ⚠ Still works — logout is client-side only; no server-side revocation. Confirm the 7-day expiry is acceptable |

### 5.2 Injection & output handling

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-S10 | P0 | Create an inquiry with name `<script>alert(1)</script>` and notes `<img src=x onerror=alert(1)>` | Renders as literal text everywhere in the dashboard (list, detail modal, edit form). No alert fires |
| TC-S11 | N/A | ~~Same payload submitted through the public quote form~~ | Not applicable in Phase 1 — there is no public-facing input field on the site at all (no form, no API call). This becomes relevant again only if/when Phase 2 (§8 item 10) is connected — reuse TC-S10's payload against `POST /api/public/inquiry` directly at that point. |
| TC-S12 | P0 | SQL injection attempts: `?status=NEW' OR '1'='1`, name `Robert'); DROP TABLE inquiries;--` | 400 for the enum param; the name is stored as an inert string. Tables intact (JPA parameterizes everything) |
| TC-S13 | P1 | `GET /api/inquiry/1%20OR%201=1` | 400, not a data leak |
| TC-S14 | P1 | Very long payloads (1MB request body) | Rejected or handled — no OOM |
| TC-S15 | P1 | Login with a null byte or 200-char password | No crash, no stack trace |

### 5.3 Transport, headers, CORS

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-S20 | P0 | `curl -i` any API response and inspect headers | `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-cache` (Spring Security defaults) present |
| TC-S21 | P0 | Cross-origin `fetch` from an evil origin (`http://localhost:9999`) to `/api/inquiry` | Blocked by CORS — only `localhost:5177` / `127.0.0.1:5177` are allowed |
| TC-S22 | P0 | `OPTIONS` preflight with no `Authorization` header | 200, not 401 |
| TC-S23 | P0 | Production: `app.cors.allowed-origins` updated to the real dashboard origin, localhost removed | Yes before launch |
| TC-S24 | P0 | Production: is the API served over **HTTPS only**? | Yes — a bearer token over plain HTTP is interceptable |
| TC-S25 | P1 | Check the login response and server logs | Password never appears (`LoginRequest.toString()` masks it); the token isn't logged |
| TC-S26 | P1 | Search the built dashboard bundle for secrets | Only `VITE_API_URL`. No API keys — anything in a `VITE_*` var is public |

### 5.4 Brute force & abuse

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-S30 | P0 | Fire 200 failed logins in a loop | ⚠ **No rate limiting or lockout exists on login.** All 200 attempts are served. Decide on a mitigation (rate limit, lockout, or a fronting proxy) before exposing the API publicly. (Note: `/api/public/inquiry` has its own rate limiter built and tested, see TC-B107 — but that endpoint isn't reachable from the live site at all in Phase 1, so it does nothing to protect what's actually deployed today.) |
| TC-S31 | P1 | Fire 500 rapid authenticated list requests | Server stays responsive |
| TC-S32 | P1 | `?limit=10000000` repeatedly | Confirm this can't exhaust memory (no server-side cap today) |
| TC-S33 | N/A | ~~Public quote form: submit 50 times in a minute~~ | Not applicable — there's no form to spam. The `tel:`/Instagram links have no submission surface at all. |
| TC-S34 | P1 (Phase 2, direct API only) | `/api/public/inquiry` with a spoofed `X-Forwarded-For` header, called directly (no proxy) | The header is ignored — the rate limiter keys on the TCP peer address (`request.getRemoteAddr()`), not any client-supplied header, so this can't be used to evade the limit *or* to frame another IP. Confirm this is still true if a reverse proxy is ever added (`PublicInquiryController.clientIp()` would need updating then — see its javadoc). Not reachable from the live site until Phase 2 is connected. |

### 5.5 Secrets & configuration

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-S40 | P0 | Production `JWT_SECRET` | Set via env to a long random value. The default `dev-only-secret-change-me-...` is **in the repo** — anyone could mint valid admin tokens |
| TC-S41 | P0 | Production `DB_PASSWORD` | Set; not the empty-root default |
| TC-S42 | P0 | `ADMIN_SEED_PASSWORD` | Either set to something strong, or the generated one was captured and then changed |
| TC-S43 | P0 | `git log -p` / `git grep` for credentials | No real secrets committed |
| TC-S44 | P1 | `spring.datasource.url` has `useSSL=false` | Fine for localhost; must change if the DB is remote |
| TC-S45 | P1 | `spring.jpa.hibernate.ddl-auto=update` in production | Consider `validate` + migrations — `update` can silently alter a live schema |
| TC-S46 | P1 | `logging.level.com.octileinstall=DEBUG` in production | Lower to INFO; DEBUG logs token-rejection details |

---

## 6. Cross-cutting (TC-X)

| ID | Pri | Case | Expected |
|---|---|---|---|
| TC-X1 | P0 | **Timezone:** set the browser to a timezone ahead of the server (or vice versa), then schedule an inspection ~1 hour in the future | ⚠ `LocalDateTime` carries no zone. The browser sends wall-clock time and the server compares it to **its own** `LocalDateTime.now()`. A valid future time can be rejected as past (or a past one accepted). Test explicitly — this bites when the server runs in UTC and Alberto is in Pacific |
| TC-X2 | P1 | Schedule an inspection across a DST boundary (Nov 1, 2026) | Displays the same time it was entered |
| TC-X3 | P1 | Create an inquiry in the dashboard, verify in MySQL, read it back through the API | Same values at all three layers |
| TC-X4 | P1 | Two tabs: edit the same inquiry in both, save A then B | Last write wins (no optimistic locking). Confirm that's acceptable for a single-user tool |
| TC-X5 | P1 | Restart the backend with existing data | Nothing lost; the seeder doesn't touch the existing admin |
| TC-X6 | P1 | Backend down → dashboard up | Every page shows an error banner; no white screen, no infinite spinner |
| TC-X7 | P1 | `VITE_API_URL` pointed at the wrong port | Same graceful failure (this is the common misconfiguration) |
| TC-X8 | P1 | Slow backend (add an artificial delay) | Spinners appear; buttons stay disabled; no duplicate submissions |
| TC-X9 | P2 | Seed ~100 inquiries and ~50 inspections | Lists stay responsive; pagination correct at scale |
| TC-X10 | P0 | **End-to-end happy path (Phase 1):** customer calls or texts (714) 624-3132, or DMs Instagram → Alberto manually creates the matching inquiry in the dashboard → schedules an inspection → marks it complete | This is the entire intended Phase 1 workflow — fully manual at the "customer reaches out" → "Alberto enters it" step, by design. Nothing on the public site captures a lead automatically. See §8 item 10 (deferred, not a defect). |
| TC-X10a | P2 | **End-to-end happy path (Phase 2, direct API only — not reachable from the live site):** `POST /api/public/inquiry` directly → row appears in `inquiries` with status NEW → Alberto sees it on the Inquiries page → schedules an inspection → marks it complete | Confirms Phase 2's backend chain works when it's eventually wired up. Every individual step has been tested (backend integration tests + manual dashboard testing), but not yet chained together against a live system in this sandbox (see the environment note in §1) |

---

## 7. Production readiness (TC-P)

| ID | Pri | Item |
|---|---|---|
| TC-P1 | N/A | ~~Real Formspree form ID~~ | Not applicable — the quote form was removed from Phase 1 entirely (2026-08-18); there's nothing to configure here. Revisit if a lead-capture mechanism is added in a later phase. |
| TC-P2 | P0 | `JWT_SECRET`, `DB_PASSWORD`, `ADMIN_SEED_PASSWORD` set as environment variables in the deploy target (needed for the admin dashboard/backend regardless of the public site's phase) |
| TC-P3 | P0 | `app.cors.allowed-origins` set to the production admin dashboard origin. (Phase 2's public-site origin is **not** needed yet — the live site makes no API calls at all in Phase 1. Add it only once Phase 2 is connected, per `BACKEND_PLAN.md` §10.) |
| TC-P4 | P0 | `VITE_API_URL` in `admin-dashboard/.env.production` points at the production API over HTTPS |
| TC-P5 | P0 | HTTPS with a valid certificate on both the site and the API |
| TC-P6 | P0 | The admin dashboard is not publicly indexable (`robots.txt` / auth wall) |
| TC-P7 | P1 | MySQL backups configured and a restore rehearsed |
| TC-P8 | P1 | `logging.level` lowered; logs go somewhere durable |
| TC-P9 | P1 | `spring.jpa.hibernate.ddl-auto` reviewed |
| TC-P10 | P1 | A password-change path exists for Alberto (there is no change-password endpoint today — document the DB/reseed procedure) |
| TC-P11 | P2 | Uptime monitoring on `/actuator/health` |

---

## 8. Pre-identified defects

Found while reading the code. The cases above will confirm them; listed here so you can
triage before testing.

| # | Severity | What | Where | Notes |
|---|---|---|---|---|
| 1 | **N/A — removed from scope** | ~~The public quote form posts to a placeholder Formspree endpoint~~ | `index.html` | Moot as of 2026-08-18: Phase 1 has no quote form at all. `#quote` is a static call/text/Instagram CTA panel — nothing to configure, nothing to break. Superseded this item's earlier "fixed"/"blocked" history; see git history if that context is needed. |
| 2 | **P0** | **No role enforcement.** `User.Role` (ADMIN/VIEWER) exists and rides in the token, but `SecurityConfig` only requires `authenticated()` and nothing uses `@PreAuthorize` | `SecurityConfig.java`, all controllers | Any account — including a future VIEWER — can delete every record. Harmless while Alberto is the only user; a trap the moment a second account exists. Still open. |
| 3 | **P0** | **No rate limiting on login** | `AuthController` / `SecurityConfig` | Unlimited password guesses against a single known account. Timing-safe and enumeration-safe, but not brute-force-safe. Still open — note this is distinct from the public-inquiry rate limiter (item 15's `RateLimiterService`), which only covers `/api/public/inquiry` and isn't even reachable from the live site yet. |
| 4 | **P0** | Default JWT secret is committed and used unless `JWT_SECRET` is set | `application.properties` | Anyone with the repo can forge admin tokens against a misconfigured deploy. Still open. |
| 5 | **P1** | The inspection **edit form always resends `scheduledDate`**, so editing a past inspection 400s with "Scheduled date must be in the future" | `InspectionForm.jsx` (`handleSubmit`) vs `InspectionService.update()` | "Mark complete" works because it sends status only. Fix: omit `scheduledDate` from the edit payload when unchanged. Cases TC-A65 / TC-B77. Still open — out of scope for this round of work. |
| 6 | **P1** | Deleting an inquiry that has inspections hits an FK constraint and surfaces as a **500 "An unexpected error occurred"** | `InquiryService.delete()` | Should be a 409 with "Delete its inspections first" (or cascade deliberately). Cases TC-B46 / TC-A43. Still open. |
| 7 | **N/A — removed from scope** | ~~Honeypot silently drops a legitimate submission~~ | `js/main.js` | Was fixed, then made moot: the form (and its honeypot field) no longer exist in Phase 1 at all. Nothing to test here anymore. |
| 8 | **P1** | **Timezone ambiguity**: `LocalDateTime` end to end, browser wall-clock vs server clock | `InspectionService.requireFutureDate()`, `utils/datetime.js` | Breaks when the server isn't in Alberto's timezone. Case TC-X1. Still open — unrelated to the public-inquiry endpoint, which has no date field. |
| 9 | **P1** | Inspection rows resolve customer names from a **`limit=100`** inquiry fetch | `InspectionsPage.jsx`, `services/inspections.js` | Silently wrong past 100 inquiries. Needs a real backend `?inquiryId=` filter eventually. Still open. |
| 10 | **P1** — deferred by design | **Customer inquiries never reach the database automatically.** A customer calls, texts, or DMs (Phase 1 has no form/email step at all); Alberto manually creates the matching inquiry in the dashboard | `index.html` (no lead-capture mechanism) / `InquiryController` | Not an oversight — the fix (`PublicInquiryController`, fully built and tested) exists in the repo as **Phase 2**, deliberately not wired up yet: Phase 1 is scoped to be informative-only with zero backend dependency for the public site. Connect it in a later phase per `BACKEND_PLAN.md` §10. |
| 11 | **P2** | Inquiry edit exposes only `{status, notes}` — no way to correct a mistyped customer email or phone | `InquiryUpdateRequest`, `InquiryForm.jsx` | Known open product question. Backend work if wanted. Still open. |
| 12 | **P2** | Logout is client-side only; a copied token stays valid for its full 7 days | `AuthContext.jsx` | Acceptable for a single-user tool; document it. Still open. |
| 13 | **P2** | Modals have no focus trap, no `role="dialog"`, no Esc-to-close; no 404 route | `ConfirmDialog.jsx`, the modals, `App.jsx` | Accessibility and polish. Still open. |
| 14 | **P2** | No server-side `limit` cap on the admin endpoints; no double-booking check on inspections | `InquiryService` / `InspectionService` | Low risk behind auth, but easy hardening. Still open. Note the Phase 2 public endpoint *does* have `@Size` request caps — the gap here is specifically the admin/authenticated path. |
| 15 | **P1** (Phase 2 only, not live) | `RateLimiterService` is **in-memory and single-instance only** | `RateLimiterService.java` | If the backend is ever scaled to more than one instance, each instance tracks its own counters and the effective rate limit multiplies by the instance count. Move to a shared store (e.g. Redis) before scaling horizontally. Only matters once Phase 2 is connected — see TC-B107-109. |
| 16 | **P2** (Phase 2 only, not live) | Rate-limit key uses `request.getRemoteAddr()`, with no `X-Forwarded-For` support | `PublicInquiryController.clientIp()` | Correct today (no reverse proxy in front). If one is ever added, every caller will appear to share the proxy's IP and one bucket — needs updating at that point, not before. Only matters once Phase 2 is connected — see TC-S34. |

---

## 9. Regression matrix

Run this short set after **any** change, before every deploy:

- [ ] TC-B1, TC-B8 — login works, unauthenticated is denied
- [ ] TC-B20, TC-B39, TC-B45 — inquiry create / update / delete
- [ ] TC-B60, TC-B61, TC-B74 — inspection create, past-date rejection, mark complete
- [ ] TC-B95 — no stack traces in 500 responses
- [ ] TC-A3, TC-A9, TC-A10 — login, expired-token redirect, logout
- [ ] TC-A32, TC-A38, TC-A42 — inquiry CRUD through the UI
- [ ] TC-A61, TC-A64, TC-A67 — inspection create, complete, reschedule
- [ ] TC-A48 — a failed action keeps the loaded table visible (historical regression)
- [ ] TC-A80 — mobile layout
- [ ] TC-C1a, TC-C1b, TC-C9, TC-C12 — call/text CTA, Instagram link, phone links sitewide, lightbox
- [ ] TC-B100, TC-B106, TC-B107 — Phase 2 only: public inquiry create, honeypot drop, rate-limit 429 (skip entirely — Phase 2 isn't connected to the live site — see §2.5)
- [ ] TC-S1, TC-S10 — auth sweep, XSS payload renders inert
- [ ] `cd backend && ./mvnw test` — green (includes `PublicInquiryControllerIntegrationTest`)
- [ ] `cd admin-dashboard && npm run build` — clean
