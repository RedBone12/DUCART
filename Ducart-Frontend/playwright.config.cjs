const { defineConfig, devices } = require("@playwright/test");
const path = require("path");

module.exports = defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  timeout: 60000,
  expect: { timeout: 15000 },
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: "http://localhost:3001",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: [
    {
      command: `${process.platform === "win32" ? "mvnw.cmd" : "sh ./mvnw"} --batch-mode --no-transfer-progress -Pbrowser-server -Djacoco.skip=true test-compile exec:java`,
      cwd: path.resolve(__dirname, "../Ducart-Backend"),
      url: "http://127.0.0.1:18080/product/1",
      timeout: 240000,
      reuseExistingServer: false,
      gracefulShutdown: { signal: "SIGTERM", timeout: 10000 },
    },
    {
      command: "npm run e2e:serve",
      url: "http://127.0.0.1:3001",
      timeout: 30000,
      reuseExistingServer: false,
    },
  ],
});
