/**
 * Standalone CDP-based test runner.
 *
 * WHY THIS EXISTS: @playwright/test's normal browser *launch* path is broken on this
 * machine two different ways:
 *   1. Playwright's bundled Chromium fails to spawn at all ("side-by-side configuration
 *      is incorrect" - a missing VC++ runtime dependency for that specific build).
 *   2. The system-installed Chrome CAN be spawned, but Playwright's default launch
 *      transport (--remote-debugging-pipe, inherited fds 3/4) dies immediately in this
 *      sandboxed shell ("Target page, context or browser has been closed").
 * Chrome's classic TCP remote-debugging-port + WebSocket CDP transport works fine here
 * (verified: GET /json/version succeeds). So instead of the @playwright/test runner, this
 * script launches Chrome manually with --remote-debugging-port and drives it via
 * playwright-core's chromium.connectOverCDP(), which only needs the WebSocket, not a
 * piped/inherited-fd launch. Results are still real, automated, assertion-backed checks -
 * just orchestrated by hand instead of by the `playwright test` CLI.
 */
const { chromium } = require('playwright-core');
const { spawn } = require('child_process');
const http = require('http');
const fs = require('fs');
const path = require('path');

const CHROME = 'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe';
const CDP_PORT = 9333;
const PROFILE_DIR = 'C:\\Users\\RICHAR~1\\AppData\\Local\\Temp\\chrome-cdp-profile-' + Date.now();

const results = [];
function record(id, title, status, detail) {
  results.push({ id, title, status, detail: detail || '' });
  console.log(`[${status}] ${id} ${title}${detail ? ' :: ' + detail : ''}`);
}

function waitForPort(port, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  return new Promise((resolve, reject) => {
    function attempt() {
      const req = http.get(`http://localhost:${port}/json/version`, (res) => {
        res.resume();
        resolve();
      });
      req.on('error', () => {
        if (Date.now() > deadline) reject(new Error('timeout waiting for CDP port'));
        else setTimeout(attempt, 300);
      });
    }
    attempt();
  });
}

async function main() {
  const child = spawn(CHROME, [
    '--headless=new',
    '--disable-gpu',
    '--no-sandbox',
    `--remote-debugging-port=${CDP_PORT}`,
    `--user-data-dir=${PROFILE_DIR}`,
    'about:blank',
  ], { detached: true, stdio: 'ignore' });
  child.unref();

  await waitForPort(CDP_PORT, 15000);
  const browser = await chromium.connectOverCDP(`http://localhost:${CDP_PORT}`);

  // ---------- Public site (TC-C), desktop ----------
  await runPublicSiteSuite(browser, { width: 1280, height: 800 }, 'desktop');
  // ---------- Public site (TC-C), mobile viewport ----------
  await runPublicSiteSuite(browser, { width: 375, height: 812 }, 'mobile');

  // ---------- Admin dashboard (TC-A), whatever is reachable without a live backend ----------
  await runAdminSuite(browser);

  await browser.close();
  try { process.kill(-child.pid); } catch (e) {}

  const outDir = path.join(__dirname, 'test-results');
  if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(path.join(outDir, 'cdp-results.json'), JSON.stringify(results, null, 2));

  const fails = results.filter((r) => r.status === 'FAIL');
  console.log(`\n${results.length} checks run, ${results.filter(r=>r.status==='PASS').length} passed, ${fails.length} failed.`);
  process.exit(fails.length ? 1 : 0);
}

async function runPublicSiteSuite(browser, viewport, tag) {
  const context = await browser.newContext({ viewport, baseURL: 'http://localhost:4173' });
  const page = await context.newPage();
  const consoleErrors = [];
  page.on('console', (msg) => { if (msg.type() === 'error') consoleErrors.push(msg.text()); });
  const failedRequests = [];
  page.on('response', (resp) => {
    const u = resp.url();
    if (/\/(images|favicon)/.test(new URL(u).pathname) && resp.status() >= 400) {
      failedRequests.push(`${resp.status()} ${u}`);
    }
  });

  await page.goto('http://localhost:4173/', { waitUntil: 'networkidle' });

  record('TC-E8', `Public site loads w/o console errors [${tag}]`,
    consoleErrors.length === 0 ? 'PASS' : 'FAIL', consoleErrors.join(' | '));

  // TC-C1
  try {
    const heroHref = await page.locator('.hero-btns a.btn-primary').getAttribute('href');
    record('TC-C1', `Hero CTA href=#quote [${tag}]`, heroHref === '#quote' ? 'PASS' : 'FAIL', heroHref);
  } catch (e) { record('TC-C1', `Hero CTA href=#quote [${tag}]`, 'FAIL', e.message); }

  // TC-C1a
  try {
    const t = await page.locator('#quote a.btn-primary').textContent();
    const href = await page.locator('#quote a.btn-primary').getAttribute('href');
    const ok = /Call or Text \(714\) 624-3132/.test(t) && href === 'tel:+17146243132';
    record('TC-C1a', `Quote call/text button [${tag}]`, ok ? 'PASS' : 'FAIL', `text="${t}" href=${href}`);
  } catch (e) { record('TC-C1a', `Quote call/text button [${tag}]`, 'FAIL', e.message); }

  // TC-C1b
  try {
    const loc = page.locator('#quote a.btn-secondary');
    const href = await loc.getAttribute('href');
    const target = await loc.getAttribute('target');
    const rel = await loc.getAttribute('rel');
    const ok = href === 'https://instagram.com/octileinstall' && target === '_blank' && rel === 'noopener';
    record('TC-C1b', `Quote Instagram button [${tag}]`, ok ? 'PASS' : 'FAIL', `href=${href} target=${target} rel=${rel}`);
  } catch (e) { record('TC-C1b', `Quote Instagram button [${tag}]`, 'FAIL', e.message); }

  // TC-C2
  try {
    const formCount = await page.locator('form').count();
    const honeypot = await page.locator('[name="company"]').count();
    const qf = await page.locator('#quote-form').count();
    const ok = formCount === 0 && honeypot === 0 && qf === 0;
    record('TC-C2', `No form/honeypot anywhere [${tag}]`, ok ? 'PASS' : 'FAIL', `forms=${formCount} honeypot=${honeypot} qf=${qf}`);
  } catch (e) { record('TC-C2', `No form/honeypot anywhere [${tag}]`, 'FAIL', e.message); }

  // TC-C9
  try {
    const hrefs = await page.locator('a[href^="tel:"]').evaluateAll((els) => els.map((e) => e.getAttribute('href')));
    const ok = hrefs.length === 5 && hrefs.every((h) => h === 'tel:+17146243132');
    record('TC-C9', `All tel: links identical (5x) [${tag}]`, ok ? 'PASS' : 'FAIL', JSON.stringify(hrefs));
  } catch (e) { record('TC-C9', `All tel: links identical (5x) [${tag}]`, 'FAIL', e.message); }

  // TC-C10
  try {
    const links = page.locator('a[href="https://instagram.com/octileinstall"]');
    const count = await links.count();
    let ok = count === 4;
    for (let i = 0; i < count && ok; i++) {
      const target = await links.nth(i).getAttribute('target');
      const rel = await links.nth(i).getAttribute('rel');
      if (target !== '_blank' || rel !== 'noopener') ok = false;
    }
    record('TC-C10', `Instagram links (4x) share target/rel [${tag}]`, ok ? 'PASS' : 'FAIL', `count=${count}`);
  } catch (e) { record('TC-C10', `Instagram links (4x) share target/rel [${tag}]`, 'FAIL', e.message); }

  // TC-C11 mobile nav (mobile viewport only)
  if (tag === 'mobile') {
    try {
      const toggle = page.locator('#navbar-toggle');
      const nav = page.locator('#primary-nav');
      const beforeClass = await nav.getAttribute('class');
      await toggle.click();
      const afterOpenClass = await nav.getAttribute('class');
      const expandedAfterOpen = await toggle.getAttribute('aria-expanded');
      await nav.locator('a').first().click();
      const afterCloseClass = await nav.getAttribute('class');
      const expandedAfterClose = await toggle.getAttribute('aria-expanded');
      const ok = !(beforeClass||'').includes('is-open')
        && (afterOpenClass||'').includes('is-open') && expandedAfterOpen === 'true'
        && !(afterCloseClass||'').includes('is-open') && expandedAfterClose === 'false';
      record('TC-C11', `Mobile nav opens then closes on link click [${tag}]`, ok ? 'PASS' : 'FAIL',
        `before=${beforeClass} afterOpen=${afterOpenClass}/${expandedAfterOpen} afterClose=${afterCloseClass}/${expandedAfterClose}`);
    } catch (e) { record('TC-C11', `Mobile nav opens then closes on link click [${tag}]`, 'FAIL', e.message); }
  }

  // TC-C12/C13 lightbox
  try {
    const firstItem = page.locator('.gallery-item').first();
    await firstItem.scrollIntoViewIfNeeded();
    await firstItem.click();
    const lightbox = page.locator('#lightbox');
    const caption = await page.locator('#lightbox-caption').textContent();
    const openClass1 = await lightbox.getAttribute('class');
    await page.locator('#lightbox-close').click();
    const closedClass1 = await lightbox.getAttribute('class');
    await firstItem.click();
    const openClass2 = await lightbox.getAttribute('class');
    await lightbox.click({ position: { x: 5, y: 5 } });
    const closedClass2 = await lightbox.getAttribute('class');
    await firstItem.click();
    const openClass3 = await lightbox.getAttribute('class');
    await page.keyboard.press('Escape');
    const closedClass3 = await lightbox.getAttribute('class');
    const ok = caption === 'Custom tile installation'
      && (openClass1||'').includes('is-open') && !(closedClass1||'').includes('is-open')
      && (openClass2||'').includes('is-open') && !(closedClass2||'').includes('is-open')
      && (openClass3||'').includes('is-open') && !(closedClass3||'').includes('is-open');
    record('TC-C12/13', `Lightbox opens w/ correct caption, closes via X/backdrop/Esc [${tag}]`,
      ok ? 'PASS' : 'FAIL', `caption="${caption}"`);
  } catch (e) { record('TC-C12/13', `Lightbox opens/closes [${tag}]`, 'FAIL', e.message); }

  // TC-C15 image load failures (collected via response listener above, need a fresh nav to catch all)
  record('TC-C15', `All gallery/hero/logo images return 200 [${tag}]`,
    failedRequests.length === 0 ? 'PASS' : 'FAIL', failedRequests.join(' | '));

  // TC-C17 scroll-reveal
  try {
    const reveal = page.locator('.reveal').nth(1);
    await reveal.scrollIntoViewIfNeeded();
    await page.waitForTimeout(500);
    const cls = await reveal.getAttribute('class');
    record('TC-C17', `Scroll-reveal gains .visible [${tag}]`, (cls||'').includes('visible') ? 'PASS' : 'FAIL', cls);
  } catch (e) { record('TC-C17', `Scroll-reveal gains .visible [${tag}]`, 'FAIL', e.message); }

  // TC-C18 footer year
  try {
    const year = await page.locator('#current-year').textContent();
    record('TC-C18', `Footer year renders 2026 [${tag}]`, year === '2026' ? 'PASS' : 'FAIL', year);
  } catch (e) { record('TC-C18', `Footer year renders 2026 [${tag}]`, 'FAIL', e.message); }

  // TC-C20 meta tags
  try {
    const title = await page.title();
    const desc = await page.locator('meta[name="description"]').getAttribute('content');
    const canonical = await page.locator('link[rel="canonical"]').getAttribute('href');
    const ogImage = await page.locator('meta[property="og:image"]').getAttribute('content');
    const ok = /Custom Shower & Tile Installation/.test(title) && !!desc && canonical === 'https://www.octileinstall.com/' && ogImage === 'images/logo.jpg';
    record('TC-C20', `Meta tags present (title/desc/canonical/og:image) [${tag}]`, ok ? 'PASS' : 'FAIL', `title="${title}" canonical=${canonical} ogImage=${ogImage}`);
  } catch (e) { record('TC-C20', `Meta tags present [${tag}]`, 'FAIL', e.message); }

  await context.close();
}

async function runAdminSuite(browser) {
  const context = await browser.newContext({ viewport: { width: 1280, height: 800 }, baseURL: 'http://localhost:5177' });
  const page = await context.newPage();
  const consoleErrors = [];
  page.on('console', (msg) => { if (msg.type() === 'error') consoleErrors.push(msg.text()); });

  // TC-E7: dev server reachable / loads
  try {
    const resp = await page.goto('http://localhost:5177/', { waitUntil: 'networkidle' });
    record('TC-E7', 'Admin dashboard Vite dev server reachable', resp && resp.ok() ? 'PASS' : 'FAIL', `status=${resp && resp.status()}`);
  } catch (e) { record('TC-E7', 'Admin dashboard Vite dev server reachable', 'FAIL', e.message); }

  // TC-A1: visit / logged out -> redirected to /login
  try {
    await page.goto('http://localhost:5177/', { waitUntil: 'networkidle' });
    const url = page.url();
    record('TC-A1', 'Visit / logged out redirects to /login', /\/login$/.test(url) ? 'PASS' : 'FAIL', url);
  } catch (e) { record('TC-A1', 'Visit / logged out redirects to /login', 'FAIL', e.message); }

  // TC-A2: visit /inquiries logged out -> redirected to /login, "from" preserved (can't fully verify without login;
  // check at least that it redirects to /login rather than / )
  try {
    await page.goto('http://localhost:5177/inquiries', { waitUntil: 'networkidle' });
    const url = page.url();
    record('TC-A2(partial)', 'Visit /inquiries logged out redirects to /login (from-preservation needs a working login - BLOCKED, see notes)',
      /\/login$/.test(url) ? 'PASS' : 'FAIL', url);
  } catch (e) { record('TC-A2(partial)', 'Visit /inquiries logged out redirects to /login', 'FAIL', e.message); }

  // TC-A5: submit login with empty fields -> HTML5 required validation blocks it, no request fires
  try {
    await page.goto('http://localhost:5177/login', { waitUntil: 'networkidle' });
    let requestFired = false;
    page.on('request', (req) => { if (req.url().includes('/api/auth/login')) requestFired = true; });
    const submitBtn = page.locator('button[type="submit"]');
    await submitBtn.click();
    await page.waitForTimeout(500);
    const emailInput = page.locator('input[type="email"], input[name="email"]').first();
    const validationMessage = await emailInput.evaluate((el) => el.validationMessage);
    record('TC-A5', 'Empty login submit blocked by HTML5 validation, no request fires',
      (!requestFired && validationMessage) ? 'PASS' : 'FAIL', `requestFired=${requestFired} validationMessage="${validationMessage}"`);
  } catch (e) { record('TC-A5', 'Empty login submit blocked by HTML5 validation', 'FAIL', e.message); }

  // TC-A7 / TC-A23 / TC-X6: backend is down in this environment (see TC-E4) -> dashboard must show a
  // friendly error, not a raw crash. This precondition is genuinely true right now, so this is a real test.
  try {
    await page.goto('http://localhost:5177/login', { waitUntil: 'networkidle' });
    await page.fill('input[type="email"], input[name="email"]', 'alberto@octileinstall.com');
    await page.fill('input[type="password"], input[name="password"]', 'whatever-the-backend-is-down');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(2000);
    const bodyText = await page.locator('body').innerText();
    const hasFriendlyMessage = /something went wrong|try again|error/i.test(bodyText);
    const isBlankPage = bodyText.trim().length < 5;
    record('TC-A7', 'Login attempt with backend down shows friendly error, not blank page',
      (hasFriendlyMessage && !isBlankPage) ? 'PASS' : 'FAIL', bodyText.slice(0, 300));
  } catch (e) { record('TC-A7', 'Login attempt with backend down shows friendly error', 'FAIL', e.message); }

  // TC-A80 mobile layout
  try {
    const context2 = await browser.newContext({ viewport: { width: 375, height: 812 }, baseURL: 'http://localhost:5177' });
    const page2 = await context2.newPage();
    await page2.goto('http://localhost:5177/login', { waitUntil: 'networkidle' });
    const overflow = await page2.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 5);
    record('TC-A80(login only)', 'Login page has no horizontal overflow at 375px (full dashboard layout needs working auth - BLOCKED)',
      !overflow ? 'PASS' : 'FAIL', `scrollWidth overflow=${overflow}`);
    await context2.close();
  } catch (e) { record('TC-A80(login only)', 'Login page mobile layout', 'FAIL', e.message); }

  record('TC-A89(partial)', 'Console errors observed during the above admin checks', consoleErrors.length === 0 ? 'PASS' : 'FAIL', consoleErrors.join(' | '));

  await context.close();
}

main().catch((e) => { console.error('FATAL', e); process.exit(2); });
