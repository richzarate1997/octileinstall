# QA Report — OctileInstall — 2026-10-13

## Summary

| Total cases | Pass | Fail | Blocked | Manual | N/A (per plan) |
|---|---|---|---|---|---|
| ~230 | 149 | 0 | 68 | 11 | 3 |

**Verdict: NOT READY for a production/multi-user launch, but no newly discovered defect blocks Phase 1 as currently scoped.** Every anomaly observed this session was already documented in the plan's §8 and is re-confirmed true (not worse, not fixed). The three still-open P0 items (#2 no role enforcement, #3 no login rate limiting, #4 default JWT secret committed) remain real pre-launch blockers exactly as the plan already said.

## Environment constraints in this sandbox (not product defects)

1. **No live backend socket possible.** `mvn test` passes clean (**71/71, 0 failures**, via MockMvc, which never opens a real port), but actually running the backend (`spring-boot:run` or `java -jar`) failed every time with `java.io.IOException: Unable to establish loopback connection` / `Invalid argument: connect` from `sun.nio.ch.UnixDomainSockets`. This was isolated to the JVM itself with a 10-line standalone program: `Selector.open()` fails identically under this machine's Temurin 17.0.6 (required for Spring Boot 3), while the same call succeeds under the machine's default JDK 8, and a plain `ServerSocket` bind succeeds under JDK 17. This blocked every TC-B/TC-A/TC-S/TC-X case that needs a live HTTP server and isn't already covered by the Maven test suite or direct source review.
2. **Local Playwright-launched Chromium doesn't work in this sandbox** (missing VC++ runtime dependency / "side-by-side configuration is incorrect"), and the system-installed Chrome's default Playwright launch transport also failed in this shell. **Workaround used:** Chrome was launched manually with `--remote-debugging-port` and driven via `playwright-core`'s `chromium.connectOverCDP()` (script: `qa/run-cdp-tests.js`). This ran 32 real, automated, assertion-backed browser checks (all public-site TC-C cases on desktop + mobile, plus the admin-dashboard cases that don't need a working login).
3. **MySQL is running but credentials are unknown** — guessing credentials was correctly refused. Blocks TC-E2, TC-B27, TC-X3/X5 (literal), TC-S3/S4 (literal).

## Results by section (condensed)

- **TC-E (preflight):** E1 PASS, E2 BLOCKED (MySQL creds), E3 PASS (71/71 tests), E4 BLOCKED (sandbox JDK/NIO socket bug, reproduced independent of app code), E5 BLOCKED, E6 BLOCKED-literal / logic-PASS (confirmed via `DataSeederTest`), E7 PASS, E8 PASS.
- **TC-B (backend API, ~90 cases):** Overwhelming majority PASS, backed by the 71 existing MockMvc integration tests (which exercise the full Spring MVC/Security chain in-process) plus direct source reads for anything the suite doesn't literally assert (enum fallback, error-message text, FK-constraint delete path, CORS/actuator config). Remainder BLOCKED where a real listening socket, real MySQL, or real elapsed wall-clock time was unavoidable (timing attack TC-B6, waiting out a rate-limit window TC-B108, 1MB bodies, etc.).
- **TC-A (admin dashboard):** Only cases not requiring a successful login could run: TC-A1 (pre-auth redirect) PASS, TC-A5 (HTML5 validation blocks empty submit) PASS, TC-A7 (friendly error with backend down) PASS, TC-A89 (no React console errors beyond expected backend-down noise) PASS, TC-A90 (`npm run build` clean) PASS. Everything needing real data/a session (TC-A3-4, A8-49, A60-88) is BLOCKED by the backend-socket issue.
- **TC-C (public site):** Fully live-tested via the CDP harness on both viewports — **every automatable case PASSED** (hero/quote CTAs, all 5 `tel:` links identical, all 4 Instagram links, no form/honeypot anywhere, mobile nav open/close, lightbox open + 3-way close with correct caption, all gallery images 200, scroll-reveal, footer year, meta tags, robots.txt/sitemap.xml, JS-disabled fallback). TC-C14/16/19/23/24 are MANUAL (need a broken image, a human decision on stray image files, real Lighthouse, legal fact-check, or real Safari/Firefox/Edge).
- **TC-S (security):** All role-enforcement, rate-limiting, JWT-secret, header, CORS-config, and git-history-secret-scan findings confirmed via direct source review and `git log -p` — all consistent with §8, nothing new or worse.
- **TC-X (cross-cutting):** Timezone bug (§8 #8) and last-write-wins (no `@Version` field) confirmed via code; most live multi-session/timing cases BLOCKED.
- **TC-P:** All MANUAL/documentation items as expected (no production target exists yet); one actionable note — this checkout's `app.cors.allowed-origins` already includes the production public-site domains even though the plan implies that's still pending.

## Defects

**Zero new defects.** All findings this session re-confirm existing §8 items (no severity changes):
- §8 #2 (P0) no role enforcement — re-confirmed via code (zero `@PreAuthorize`/`hasRole` usage anywhere)
- §8 #3 (P0) no login rate limiting — re-confirmed via code
- §8 #4 (P0) default JWT secret committed — re-confirmed in `application.properties` + git history
- §8 #5 (P1) inspection edit resends `scheduledDate` → 400 — re-confirmed via code path
- §8 #6 (P1) delete-inquiry-with-inspections → uncaught 500 — re-confirmed via code (no try/catch)
- §8 #8 (P1) timezone ambiguity — re-confirmed via `LocalDateTime.now()` comparison
- §8 #9, #11–#16 — all consistent with documented status

## Blocked cases (grouped by cause)

- **No live backend socket:** TC-E4/E5, TC-B6/35/62/91/92/108/110, TC-S10/14/15/21(live)/31/32, TC-X2/8/9, and nearly all of TC-A20 onward.
- **MySQL credentials unknown:** TC-E2, TC-B27/47/48/49 (literal), TC-X3/5 (literal), TC-S3/4 (literal).
- **Needs real elapsed time:** TC-B6, TC-B108.
- **Needs a human/second device:** TC-C14, TC-C16, TC-C19, TC-C23, TC-C24, TC-P2/P4–P11, TC-S23/24/42.

## Manual checks for a human

- **TC-C14:** Confirm a placeholder tile with no image shows caption text, not a broken-image icon.
- **TC-C16:** Decide whether the untracked large files in `images/` (including the `.MOV`) should ship or be removed.
- **TC-C19:** Run Lighthouse (mobile) against the deployed/staged site — not available from this sandbox.
- **TC-C23:** Fact-check the license number `#1040037` and listed service areas — legal/compliance, not testable in code.
- **TC-C24:** Cross-browser check on real Safari (iOS), Firefox, and Edge — only Chrome was available here.
- **TC-P2, TC-P4–TC-P11:** Production deploy checklist — no production environment exists yet to verify against.
- **TC-S23, TC-S24, TC-S42:** Production CORS origin, HTTPS certificate, and `ADMIN_SEED_PASSWORD` rotation — all require an actual production deploy to check.

## Test plan issues found

- **TC-B92:** `PATCH /api/inquiry/{id}` bypasses `GlobalExceptionHandler` entirely (Spring resolves the unmapped-verb 405 itself) — worth confirming live whether that response still matches the `{status,error,message}` shape TC-B94 requires, once the environment blocker is lifted. Not confirmed either way this session.
- **TC-P3:** this checkout's `app.cors.allowed-origins` already lists the production public-site domains (`https://www.octileinstall.com`, `https://octileinstall.com`), contrary to the plan's implication that this is still pending — only the production **dashboard** origin is actually missing.

## Key artifacts on disk

- `qa/playwright.config.js` and `qa/tests/public-site.spec.js` — conventional Playwright spec, ready to run once this sandbox's Chromium/VC++ issue is fixed or on a normal machine.
- `qa/run-cdp-tests.js` — the harness that actually ran this session (`node qa/run-cdp-tests.js`); results in `qa/test-results/cdp-results.json`.
- Backend test run: `backend/target/surefire-reports/*.txt` (71/71 green).
