# Quality gates

Every pull request and every push to main runs the Quality workflow. There are no
path filters or skipped checks for documentation-only changes. The four stable
required-check names are `Backend quality`, `Frontend quality`, `MySQL integration` and `Browser E2E`.

| Check | Enforced requirement |
| --- | --- |
| Backend tests and package | Maven clean verify succeeds on Java 17 |
| JaCoCo | At least 90% line and 80% branch coverage across the application bundle |
| Checkstyle | Zero violations of the checked-in 13-rule configuration |
| SpotBugs | Zero high-priority findings, maximum analysis effort |
| Frontend ESLint | Zero errors and zero warnings in application and test files |
| Jest | At least 90% statements, lines and functions; 80% branches |
| Frontend build | Production build succeeds with CI=true |
| MySQL integration | 22 real MySQL query, constraint, checkout and JWT tests; separate coverage report |
| Browser E2E | Three Chromium journeys against real Spring Boot and MySQL |

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

Configure main to require a pull request and all four checks, with the branch
up to date before merging. Apply the rules to administrators too, and disallow
force pushes and branch deletion. Zero approval reviews are required so a solo
maintainer can merge a reviewed change without needing a second account.

The old pages.yml workflow is replaced by quality.yml. On main, the frontend
job creates the Pages artifact. Publishing depends on all four test and quality jobs succeeding. Pull requests run the same quality commands but never deploy.
No CI=false or lint-disable environment override is used.

Jenkinsfile invokes the same Maven command and frontend lint, coverage and build
commands. This does not prove a Jenkins server has executed the pipeline. The
repository webhook audit on 25 October 2026 found zero hooks; GitHub Actions
supplies the required PR checks without a Jenkins webhook. A future Jenkins
integration should provide its own verified run rather than duplicate this claim.

For isolated database and browser commands, scope and reports, see [integration testing](integration-testing.md).
