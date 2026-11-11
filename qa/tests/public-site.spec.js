const { test, expect } = require('@playwright/test');

test.describe('Public customer site (TC-C)', () => {
  let consoleErrors = [];

  test.beforeEach(async ({ page }) => {
    consoleErrors = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') consoleErrors.push(msg.text());
    });
    await page.goto('/');
  });

  test('TC-E8 Loads without console errors', async ({ page }) => {
    await page.waitForLoadState('networkidle');
    expect(consoleErrors, `console errors: ${JSON.stringify(consoleErrors)}`).toEqual([]);
  });

  test('TC-C1 Hero CTA button resolves to #quote', async ({ page }) => {
    const btn = page.locator('.hero-btns a.btn-primary');
    await expect(btn).toHaveAttribute('href', '#quote');
    await btn.click();
    await expect(page.locator('#quote')).toBeInViewport();
  });

  test('TC-C1a Quote section call/text button', async ({ page }) => {
    const btn = page.locator('#quote a.btn-primary');
    await expect(btn).toHaveText(/Call or Text \(714\) 624-3132/);
    await expect(btn).toHaveAttribute('href', 'tel:+17146243132');
  });

  test('TC-C1b Quote section Instagram button', async ({ page }) => {
    const btn = page.locator('#quote a.btn-secondary');
    await expect(btn).toHaveAttribute('href', 'https://instagram.com/octileinstall');
    await expect(btn).toHaveAttribute('target', '_blank');
    await expect(btn).toHaveAttribute('rel', 'noopener');
  });

  test('TC-C2 No form / honeypot exists anywhere on the page', async ({ page }) => {
    expect(await page.locator('form').count()).toBe(0);
    expect(await page.locator('#quote-form').count()).toBe(0);
    expect(await page.locator('#quote-form-message').count()).toBe(0);
    expect(await page.locator('[name="company"]').count()).toBe(0);
  });

  test('TC-C9 All tel: links sitewide are identical', async ({ page }) => {
    const hrefs = await page.locator('a[href^="tel:"]').evaluateAll((els) => els.map((e) => e.getAttribute('href')));
    expect(hrefs.length).toBe(5);
    for (const h of hrefs) expect(h).toBe('tel:+17146243132');
  });

  test('TC-C10 Instagram links all share target=_blank rel=noopener', async ({ page }) => {
    const links = page.locator('a[href="https://instagram.com/octileinstall"]');
    const count = await links.count();
    expect(count).toBe(4);
    for (let i = 0; i < count; i++) {
      await expect(links.nth(i)).toHaveAttribute('target', '_blank');
      await expect(links.nth(i)).toHaveAttribute('rel', 'noopener');
    }
  });

  test('TC-C11 Mobile nav toggle opens and closes on link click', async ({ page, isMobile }) => {
    test.skip(!isMobile, 'mobile-only behavior');
    const toggle = page.locator('#navbar-toggle');
    const nav = page.locator('#primary-nav');
    await expect(nav).not.toHaveClass(/is-open/);
    await toggle.click();
    await expect(nav).toHaveClass(/is-open/);
    await expect(toggle).toHaveAttribute('aria-expanded', 'true');
    await nav.locator('a').first().click();
    await expect(nav).not.toHaveClass(/is-open/);
    await expect(toggle).toHaveAttribute('aria-expanded', 'false');
  });

  test('TC-C12/TC-C13 Gallery lightbox opens with correct image/caption and closes 3 ways', async ({ page }) => {
    const firstItem = page.locator('.gallery-item').first();
    await firstItem.scrollIntoViewIfNeeded();
    await firstItem.click();
    const lightbox = page.locator('#lightbox');
    await expect(lightbox).toHaveClass(/is-open/);
    await expect(page.locator('#lightbox-caption')).toHaveText('Custom tile installation');
    // close via X button
    await page.locator('#lightbox-close').click();
    await expect(lightbox).not.toHaveClass(/is-open/);
    // reopen, close via backdrop click
    await firstItem.click();
    await expect(lightbox).toHaveClass(/is-open/);
    await lightbox.click({ position: { x: 5, y: 5 } });
    await expect(lightbox).not.toHaveClass(/is-open/);
    // reopen, close via Esc
    await firstItem.click();
    await expect(lightbox).toHaveClass(/is-open/);
    await page.keyboard.press('Escape');
    await expect(lightbox).not.toHaveClass(/is-open/);
  });

  test('TC-C15 All gallery/hero images return 200', async ({ page }) => {
    const failures = [];
    page.on('response', (resp) => {
      const url = resp.url();
      if (/\/(images|favicon)/.test(new URL(url).pathname) && resp.status() >= 400) {
        failures.push(`${resp.status()} ${url}`);
      }
    });
    await page.reload();
    await page.waitForLoadState('networkidle');
    expect(failures).toEqual([]);
  });

  test('TC-C17 Scroll-reveal elements gain .visible on scroll', async ({ page }) => {
    const reveal = page.locator('.reveal').nth(1); // services grid, below the fold
    // Before scrolling into view it should not yet be visible (best-effort; IO can fire fast)
    await reveal.scrollIntoViewIfNeeded();
    await expect(reveal).toHaveClass(/visible/, { timeout: 5000 });
  });

  test('TC-C18 Footer copyright year renders 2026', async ({ page }) => {
    await expect(page.locator('#current-year')).toHaveText('2026');
  });

  test('TC-C20 Meta tags present', async ({ page }) => {
    await expect(page).toHaveTitle(/Custom Shower & Tile Installation/);
    const desc = await page.locator('meta[name="description"]').getAttribute('content');
    expect(desc).toContain('Custom shower');
    const canonical = await page.locator('link[rel="canonical"]').getAttribute('href');
    expect(canonical).toBe('https://www.octileinstall.com/');
    const ogImage = await page.locator('meta[property="og:image"]').getAttribute('content');
    expect(ogImage).toBe('images/logo.jpg');
  });

  test('TC-C21 og:image resolves to a real 200 file', async ({ page, request }) => {
    const resp = await request.get('/images/logo.jpg');
    expect(resp.status()).toBe(200);
  });

  test('TC-C22 robots.txt and sitemap.xml return 200', async ({ request }) => {
    const robots = await request.get('/robots.txt');
    expect(robots.status()).toBe(200);
    const robotsBody = await robots.text();
    expect(robotsBody).toMatch(/sitemap/i);
    const sitemap = await request.get('/sitemap.xml');
    expect(sitemap.status()).toBe(200);
    const sitemapBody = await sitemap.text();
    expect(sitemapBody).toContain('https://www.octileinstall.com/');
  });

  test('TC-C25 Content readable with JS disabled (static anchors)', async ({ browser }) => {
    const context = await browser.newContext({ javaScriptEnabled: false });
    const page = await context.newPage();
    await page.goto('/');
    await expect(page.locator('#quote a.btn-primary')).toHaveAttribute('href', 'tel:+17146243132');
    await expect(page.locator('#quote a.btn-secondary')).toHaveAttribute('href', 'https://instagram.com/octileinstall');
    await context.close();
  });
});
