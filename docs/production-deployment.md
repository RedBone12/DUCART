# Production deployment and recovery

This configuration retains Spring Boot, MySQL and local image files on one Linux
server using Docker Compose. The root `compose.yaml` is for local development;
`deploy/compose.yaml` is the separate production stack. GitHub Pages publishes
only the static frontend.

Repository configuration and local recovery drills do not establish a public
cloud deployment. Public operation needs a real server, domain DNS and production
SMTP settings. Backup timers run only after installation on that server.

## Ingress and persistence

- Caddy publishes TCP 80/443, redirects HTTP to HTTPS and automatically obtains
  and renews certificates for a public domain.
- React and `/api/*` share an HTTPS origin. The gateway removes `/api` before
  proxying to Spring Boot. `/uploads/*` serves images; `/healthz` exposes database
  readiness without internal details.
- MySQL, backend 8080 and management 8081 are not published to the host.
  MySQL uses a separate internal network.
- Separate persistent volumes retain MySQL, images and Caddy certificate/configuration
  data when containers are recreated.
- The `prod` profile uses Flyway and `ddl-auto=validate` and disables SQL logging.
  CORS allows only `PUBLIC_ORIGIN`, without automatically allowing localhost.
- Backend readiness includes database availability. Liveness does not treat a
  database outage as a dead JVM. Docker does not restart a container merely
  because it becomes unhealthy.

## First deployment

The server needs Docker Engine, Compose v2, Node.js 22 and Git. Domain A/AAAA
records must point to it; public TCP 80/443 must be reachable. Configure SSH
access for the actual port. Run all commands from the repository root using
the same deployment account with Docker access.

```sh
node scripts/ops.cjs init --env deploy/production.env --origin https://shop.example.com
```

This generates random database passwords and a JWT secret and refuses to
overwrite existing files. Linux permissions are 0600; Windows uses directory
ACLs, so store the file in a protected directory. Edit the generated SMTP host,
username, password and sender. Production SMTP uses STARTTLS, default port 587;
placeholder SMTP settings prevent production deployment.
Never commit environment files or publish complete `docker compose config` output.

```sh
node scripts/ops.cjs build --env deploy/production.env --project ducart-production --release v1
node scripts/ops.cjs deploy --env deploy/production.env --project ducart-production --release v1
node scripts/ops.cjs verify --env deploy/production.env --project ducart-production
```

Use a new immutable `--release` name, such as a commit SHA. Builds refuse to
overwrite an existing backend release. Deployment records actual image IDs and
checks HTTPS certificates, redirects, SPA refresh, product API, database readiness,
CORS and frontend `release.json`. Production uses the system CA trust store
without bypassing certificate validation.

Initial startup fetches missing base images without automatically upgrading
existing ones. Validate MySQL 8.4/Caddy upgrades separately; application-image
rollback is not a database-engine downgrade mechanism.

For an existing deployment, the script blocks POST/PUT/PATCH/DELETE, drains and
stops backend requests, backs up database/images, then starts the candidate.
Writes resume only after read-only verification. Failure restores the previous
snapshot's database, images and application images, verifies recovery, resumes
writes and returns a failing exit code. A failed first deployment stops the app
and preserves its database for diagnosis rather than deleting unknown data.
This process includes a short maintenance window; it is not zero-downtime deployment.

## Database migrations

`V1__initial_schema.sql` creates the current 14 application tables, unique
constraints and foreign keys; Flyway creates its own history table. Add later
changes as `V2__...sql` and never edit an applied migration.
MySQL DDL cannot guarantee whole-migration rollback, so the full pre-deployment
snapshot is the recovery path after migration failure.

Before restoration, failed deployments save private backend logs and the
then-current database schema under `deploy/.state/failures/`. These diagnostics
are not uploaded as public CI artifacts.

`baseline-on-migrate` is disabled by default. Switching an existing
Hibernate-managed database directly to prod is rejected. Do not blindly baseline
or run Flyway clean merely to make startup succeed.

For legacy data, preserve the original database: back up it and uploads, let
Flyway create a new database in a separate Compose project, then import reviewed
data. The following export reads the original database:

```sh
docker compose --env-file .env exec -T mysql sh -ec \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -u root --single-transaction --no-create-info --complete-insert --skip-triggers --skip-add-locks --skip-disable-keys --set-gtid-purged=OFF --hex-blob "$MYSQL_DATABASE"' > legacy-data.sql
```

Review source/target columns, uniqueness, foreign keys, order amounts and image
paths. Import in isolation, compare records and verify login/purchase before
switching traffic. If the source already has Flyway history, do not import that
history as ordinary business data into a different migration history.
The synthetic drill does not migrate an existing business database.

## Backup and restore

```sh
node scripts/ops.cjs backup --env deploy/production.env --project ducart-production
```

Complete snapshots are stored under `backups/<project>/<timestamp>/`:

| File | Contents |
| --- | --- |
| database.sql | Business database and Flyway history, including table definitions |
| uploads.tar | Image snapshot consistent with the database |
| images.tar | Matching application images for loading on a new host |
| manifest.json | Release, image IDs and SHA-256 checksums for the files above |

Application images are saved first; the backend then stops during the maintenance
window while database and uploads are exported. The manifest is written only
after all steps succeed. Restore verifies every checksum and target project
before stopping services, rebuilding the specified database, replacing the
specified uploads volume and restoring matching images. Invalid checksums do not
modify business data.

Snapshots contain user data and password hashes; environment files contain
secrets. Store them securely and copy encrypted backups off-host. Backups on the
same machine cannot protect against total host loss. Keep environment files
separately; changing the JWT secret invalidates existing login tokens.

Before restoring real data, save the current environment and check the target name:

```sh
node scripts/ops.cjs restore --env deploy/production.env --project ducart-production \
  --backup /secure-backups/snapshot-directory --confirm-project ducart-production
```

On a new host with the same architecture, restore source, Node/Docker and the
production environment file, then run the same command. Missing application
images load from `images.tar`. Cross-project drills require
`--allow-cross-project` and the same database name. `--restore-images` forces
the image archive loading path.

Use an empty target or back up its existing data first. Business recovery does
not depend on Caddy's private local CA: public certificates can be reissued,
provided DNS and public ports still meet issuance requirements.

## Rollback and resuming writes

```sh
node scripts/ops.cjs rollback --env deploy/production.env --project ducart-production \
  --confirm-project ducart-production --restore-data
```

Rollback restores the database, uploads and images from before the last
deployment. It rewinds data added after a successful release, so `--restore-data`
is required explicitly. Automatic failed-deployment rollback keeps writes blocked
throughout, avoiding acknowledged writes from the candidate release.
If recovery fails, remain in maintenance mode, diagnose and restore again;
do not simply remove the maintenance marker.

```sh
node scripts/ops.cjs resume --env deploy/production.env --project ducart-production
```

`resume` allows writes only after complete deployment verification.
Never run `down --volumes` for the production project: it deletes persistent data.

## Scheduled backups

`deploy/ducart-backup.service` and `.timer` are Linux systemd templates.
Adjust the account, repository location and Node path, then install and verify:

```sh
sudo cp deploy/ducart-backup.service deploy/ducart-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl start ducart-backup.service
sudo systemctl enable --now ducart-backup.timer
systemctl list-timers ducart-backup.timer
journalctl -u ducart-backup.service --no-pager
```

The template runs daily and includes a short maintenance window; checked-in
files do not prove the timer is installed. Check free disk space and off-host
copies regularly. Retain recent valid snapshots and snapshots referenced by
rollback state. Daily backups can leave the recovery point roughly a day behind;
actual recovery duration comes from the drill report.

## Isolated recovery drill

With Docker running, execute from the repository root in Windows PowerShell or Linux:

```sh
node scripts/recovery-drill.cjs
```

The script builds production images and uses random project names, local ports,
a local CA and synthetic accounts to verify:

1. HTTPS certificates/redirects, same-origin API, production CORS, private ports
   and database readiness.
2. Registration, login, administrator image upload and persisted purchases.
3. Rejected writes during maintenance and persistence after container recreation.
4. Failed readiness during a database outage and recovery afterward.
5. Rejection of corrupt snapshots and repair of deliberately damaged data/images.
6. Recovery into a second project with fresh volumes and archived images, including
   reads of the original order and image bytes.
7. Explicit rollback and a deliberately failing partial MySQL DDL migration,
   verifying removal of the new table and restoration of schema, application
   images, order and uploaded image.

The client trusts the drill CA and verifies certificates without TLS bypasses.
By default, the script removes random test containers, volumes, synthetic backups
and environment files afterward; `--keep` preserves them for diagnosis.
The report is `deploy/drill-results/latest.json`, without secrets or business
backups. `--release <existing-drill-release>` reuses previously built images.
CI's `Deployment recovery` check runs the same drill and uploads the report;
Pages publishing depends on all five checks.
