# Quality gates

Every pull request and every push to main runs the Quality workflow. There are no
path filters or skipped checks for documentation-only changes. The five stable
required-check names are `Backend quality`, `Frontend quality`, `MySQL integration`, `Browser E2E` and `Deployment recovery`.

| Check | Enforced requirement |
| --- | --- |
| Backend tests and package | Maven clean verify succeeds on Java 17 |
| JaCoCo | At least 90% line and 80% branch coverage across the application bundle |
| Checkstyle | Zero violations of the checked-in 13-rule configuration |
| SpotBugs | Zero high-priority findings, maximum analysis effort |
| Frontend ESLint | Zero errors and zero warnings in application and test files |
| Jest | At least 90% statements, lines and functions; 80% branches |
| Frontend build | Production build succeeds with CI=true |
| MySQL integration | Real MySQL query, constraint, checkout, JWT and Flyway migration tests; separate coverage report |
| Browser E2E | Four Chromium journeys against real Spring Boot and MySQL |
| Deployment recovery | Real HTTPS stack, persistent purchase/image, outage readiness, backup validation, fresh-volume restore and rollback |

JaCoCo has no package exclusions. Its standard generated-bytecode filters still
apply. Jest collects every JS/JSX file under src, including unimported files;
only test files and setupTests.js are excluded. Checkstyle checks main Java
sources with naming, modifier order, type layout and correctness rules; it does
not claim compliance with the entire Google or Sun style guide. SpotBugs gates
high-priority findings; medium/low-priority findings are not part of this gate.
There are no SpotBugs exclusions or ESLint warning baselines.

## Local commands

From Ducart-Backend in PowerShell:

```powershell
./mvnw.cmd clean verify
```

From Ducart-Frontend:

```powershell
npm.cmd ci
npm.cmd run lint
$env:CI = 'true'
npm.cmd run coverage
npm.cmd run build
```

On Linux CI, the Maven wrapper is invoked with `sh ./mvnw` so its existing Git
file mode does not prevent execution. Run clean before verify: the existing
JaCoCo configuration instruments classes offline, then restores them after tests.

## Reports and measured baseline

Local verification on 25 October 2026 passed 471 backend tests and 331 frontend
tests, Checkstyle, SpotBugs, ESLint and the CI=true Pages production build.

The integration update verified on 27 October passed 473 backend unit tests,
22 real MySQL integration tests, 332 frontend tests and all three Chromium
journeys. ESLint and the CI=true browser production build also passed. The
coverage table below records the original 25 October baseline.

The production update adds two production-origin CORS tests and a MySQL Flyway
migration/validation test. A concurrency regression also ensures fresh password
reset limit buckets cannot merge over another request's counter. That production
baseline recorded 476 backend tests and 38 MySQL integration tests.
These are historical counts, not the size of the current suites. The catalogue
update adds combined-search and pagination tests and a fourth browser journey.
See [PR CI evidence](ci-evidence.md) for a successful run on that revision. The recovery job
separately executes real deployment and storage drills;
its report distinguishes local TLS verification from a public cloud deployment.

| Coverage | Backend | Frontend |
| --- | --- | --- |
| Lines | 96.84% | 99.10% |
| Branches | 90.38% | 88.06% |
| Statements | Not a JaCoCo metric | 97.92% |
| Functions | Not the selected JaCoCo gate | 97.22% |

Backend reports live in target/site/jacoco, target/checkstyle-result.xml,
target/spotbugsXml.xml and target/surefire-reports. Frontend reports live under
coverage. GitHub Actions uploads both sets of reports for 14 days, including
available reports after a failure.

Negative probes also confirmed that JaCoCo fails when temporarily asked for 100%
line coverage and Jest fails with a 100% line threshold and only one test file.
Those probes used command-line overrides; the committed thresholds remain 90/80.
SpotBugs initially failed on a silently swallowed picture-setter exception.
The implementation now checks optional bean properties explicitly and propagates
real property-access failures; a regression test verifies it never saves the
incomplete item after a setter failure.

## Pull requests and publishing

Verified in GitHub repository settings on 29 October 2026: main requires a pull
request, all five checks from GitHub Actions, an up-to-date branch and resolved
conversations before merging. Administrators cannot bypass these settings;
force pushes and branch deletion are disabled. Approval reviews are not required,
so a solo maintainer can merge after inspecting a change and passing the checks.
See [PR CI evidence](ci-evidence.md) for the settings link and successful run.

The old pages.yml workflow is replaced by quality.yml. On main, the frontend
job creates the Pages artifact. Publishing depends on all five test and quality jobs succeeding. Pull requests run the same quality commands but never deploy.
No CI=false or lint-disable environment override is used.

Jenkinsfile invokes the same Maven command and frontend lint, coverage and build
commands. This is an optional Windows-host pipeline definition, without verified
Jenkins server execution, Multibranch discovery or webhook delivery evidence.
GitHub Actions supplies the active required PR checks through its native
pull_request trigger; a Jenkins webhook is not needed for that implementation.
Do not describe Actions runs as Jenkins runs. If Jenkins is adopted later, verify
its own controller, webhook deliveries, PR discovery and published build status.

For isolated database and browser commands, scope and reports, see [integration testing](integration-testing.md).
