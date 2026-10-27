# MySQL integration and browser acceptance tests

These suites use disposable Testcontainers MySQL 8.4 databases. They never connect
to the Compose database, reuse its volumes, or fall back to H2. Docker must be
running; an unavailable Docker daemon fails the suite. Testcontainers cleans up
the database when the test process exits.

## MySQL integration

From `Ducart-Backend` in PowerShell:

```powershell
./mvnw.cmd --batch-mode --no-transfer-progress clean verify -Pmysql-it
```

The normal unit tests, Checkstyle, SpotBugs and coverage gate run first. Failsafe
then runs 22 MySQL tests: repository queries and user uniqueness constraints,
picture collection reload, server-side checkout calculation, idempotency,
concurrent stock deduction and transaction rollback. One full MockMvc journey
uses actual registration, password authentication and signed JWTs to purchase
and verify order ownership; it does not replace authentication with mock users.
Only the deliberate persistence-failure scenario uses a repository spy to inject
the failure that must roll back the transaction.

Reports: `target/failsafe-reports` and `target/site/jacoco-it/index.html`.
Integration coverage is reported separately from the existing unit coverage
gate; there is no claim that these focused journeys cover the entire application.
The default `clean verify` remains independent of Docker.

## Chromium acceptance

From `Ducart-Frontend`:

```powershell
npm.cmd ci
npx.cmd playwright install chromium
npm.cmd run e2e:build
npm.cmd run test:e2e
```

Playwright starts a real Spring Boot server on `127.0.0.1:18080`, a fresh MySQL
container and a production frontend on `localhost:3001`. Keep both ports free.
Existing servers are rejected rather than reused. Do not run another Maven build
against the same backend target directory while this server starts.

The test-only bootstrap seeds a catalogue and an administrator. Its fixed
credentials exist only in the disposable database; the bootstrap is under
`src/test` and is not packaged in the production JAR. Uploaded test images use a
separate temporary directory, with cleanup on graceful shutdown. Browser users
have generated identities under `example.test`. No reset emails are sent.

Three Chromium journeys exercise real requests without intercepted API fixtures:

- Buyer registration, login, search, cart, checkout, persisted order history and
  actual inventory deduction.
- Administrator product creation with a real image upload, public image retrieval,
  update and deletion, including persisted API assertions.
- Buyer exclusion from administrator pages and product-write APIs.

Reports are in `playwright-report`; failures retain screenshots, videos and traces
under `test-results`. Open a report with `npx.cmd playwright show-report`.
Firefox, WebKit, payment gateways and production deployments are outside this
suite's scope.

The browser suite exposed an expired bundled RichTextEditor license that made
product descriptions read-only. Product creation and editing now use a native
textarea and submit the stored description unchanged, including existing HTML.
These two forms no longer offer the old formatting toolbar. The tests enter the
description through the real accessible input and check the persisted value.
The update form initializes a product once per route ID, so a later catalogue
refresh cannot replace unsaved edits. A frontend regression test covers this race.

## Pull request enforcement

The Quality workflow runs separate `MySQL integration` and `Browser E2E` jobs
alongside the two existing quality jobs. Reports are retained for 14 days,
including available failure evidence. Pages publishing waits for all four jobs.
See [quality gates](quality-gates.md) for coverage thresholds and branch rules.
