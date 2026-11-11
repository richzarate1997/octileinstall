// @ts-check
const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests',
  timeout: 30000,
  fullyParallel: false,
  retries: 0,
  reporter: [['list'], ['json', { outputFile: 'test-results/results.json' }]],
  outputDir: 'test-results',
  use: {
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    actionTimeout: 10000,
    // This machine's bundled Playwright Chromium fails to launch (missing VC++ runtime
    // dependency - "side-by-side configuration is incorrect"). Use the system-installed
    // Chrome instead, which has the dependency satisfied via its own installer.
    channel: 'chrome',
  },
  projects: [
    {
      name: 'public-site-desktop',
      testMatch: /public-site.*\.spec\.js/,
      use: { ...devices['Desktop Chrome'], baseURL: 'http://localhost:4173' },
    },
    {
      name: 'public-site-mobile',
      testMatch: /public-site.*\.spec\.js/,
      use: { ...devices['Pixel 7'], baseURL: 'http://localhost:4173' },
    },
    {
      name: 'admin-desktop',
      testMatch: /admin.*\.spec\.js/,
      use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 }, baseURL: 'http://localhost:5177' },
    },
    {
      name: 'admin-mobile',
      testMatch: /admin.*\.spec\.js/,
      use: { ...devices['Desktop Chrome'], viewport: { width: 375, height: 812 }, isMobile: true, hasTouch: true, baseURL: 'http://localhost:5177' },
    },
  ],
});
