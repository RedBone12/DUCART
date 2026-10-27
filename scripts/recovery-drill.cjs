#!/usr/bin/env node
// Synthetic fixtures only. All destructive operations target generated drill projects.
const fs = require("node:fs");
const path = require("node:path");
const net = require("node:net");
const assert = require("node:assert/strict");
const crypto = require("node:crypto");
const { spawnSync } = require("node:child_process");
const { request } = require("./ops.cjs");
const root = path.resolve(__dirname, "..");
const docker = process.env.DOCKER_EXE || "docker";
const suffix = crypto.randomBytes(4).toString("hex");
const project = "ducart-drill-" + suffix;
const clone = "ducart-drill-restore-" + suffix;
const releaseIndex = process.argv.indexOf("--release");
const release = releaseIndex < 0 ? "drill-" + Date.now() : process.argv[releaseIndex + 1];
const started = Date.now();
const checks = [];
const configurations = new Map();
const reportDir = path.join(root, "deploy/drill-results");
fs.mkdirSync(reportDir, { recursive: true });

function execute(binary, argv, opts = {}) {
  const result = spawnSync(binary, argv, { cwd: root, stdio: opts.capture ? ["pipe", "pipe", "inherit"] : "inherit", windowsHide: true, ...opts });
  if (result.error) throw result.error;
  if (opts.fail) { assert.notEqual(result.status, 0, "Operation should have been rejected"); return; }
  assert.equal(result.status, 0, "Operation failed: " + argv.slice(0, 2).join(" "));
  return result.stdout?.toString("utf8").trim();
}

function ops(action, target, extra = [], opts = {}) {
  execute(process.execPath, [path.join(root, "scripts/ops.cjs"), action, "--env", configurations.get(target).file, "--project", target, ...extra], opts);
}

function current(target) {
  return JSON.parse(fs.readFileSync(path.join(root, "deploy/.state", target + ".json"), "utf8")).current;
}

function dc(target, argv, opts = {}) {
  const config = configurations.get(target);
  const images = fs.existsSync(path.join(root, "deploy/.state", target + ".json")) ? current(target) : { backend: "ducart-backend:" + release, frontend: "ducart-frontend:" + release };
  return execute(docker, ["compose", "--env-file", config.file, "-f", path.join(root, "deploy/compose.yaml"), "-p", target, ...argv], {
    ...opts,
    env: { ...process.env, BACKEND_IMAGE: images.backend, FRONTEND_IMAGE: images.frontend, COMPOSE_PROFILES: "local" },
  });
}

function sql(target, statement) {
  dc(target, ["exec", "-T", "mysql", "sh", "-ec", 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root "$MYSQL_DATABASE"'], { input: statement, stdio: ["pipe", "inherit", "inherit"] });
}

async function freePort() {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once("error", reject);
    server.listen(0, "127.0.0.1", () => { const port = server.address().port; server.close(() => resolve(port)); });
  });
}

async function init(target) {
  const httpsPort = await freePort();
  const httpPort = await freePort();
  const file = path.join(root, "deploy", target + ".env");
  for (const existing of [file, path.join(root, "deploy/.state", target + ".json"), path.join(root, "backups", target)]) {
    if (fs.existsSync(existing)) throw new Error("Drill namespace already has files");
  }
  for (const argv of [["ps", "-aq"], ["volume", "ls", "-q"], ["network", "ls", "-q"]]) {
    if (execute(docker, [...argv, "--filter", "label=com.docker.compose.project=" + target], { capture: true })) throw new Error("Drill namespace is already in use");
  }
  execute(process.execPath, [path.join(root, "scripts/ops.cjs"), "init", "--env", file, "--project", target, "--origin", "https://localhost:" + httpsPort, "--local"]);
  configurations.set(target, { file, origin: "https://localhost:" + httpsPort });
  const text = fs.readFileSync(file, "utf8").replace(/^HTTP_PORT=.*$/m, "HTTP_PORT=" + httpPort);
  fs.writeFileSync(file, text, { mode: 0o600 });
}

function ca(target) { return fs.readFileSync(path.join(root, "deploy/.state", target + "-root.crt")); }

async function api(target, route, { method = "GET", data, token, bytes, contentType } = {}) {
  const body = bytes || (data === undefined ? undefined : JSON.stringify(data));
  const response = await request(configurations.get(target).origin + "/api" + route, ca(target), {
    method,
    headers: { ...(body ? { "Content-Type": contentType || "application/json" } : {}), ...(token ? { Authorization: "Bearer " + token } : {}) },
    body,
  });
  assert.ok(response.status >= 200 && response.status < 300, route + " returned HTTP " + response.status + ": " + response.body.slice(0, 500));
  return JSON.parse(response.body);
}

const password = "RecoveryDrill123!";
const image = Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jF1sAAAAASUVORK5CYII=", "base64");
let product;
let order;

async function login(target, username) { return (await api(target, "/user/login", { method: "POST", data: { username, password } })).token; }

async function upload(target, name) {
  const token = await login(target, "recovery-admin");
  const boundary = "DucartDrill" + suffix;
  const data = { name, maincategory: "Recovery", subcategory: "Recovery", brand: "Recovery", color: "Blue", size: "M", basePrice: 19.99, discount: 0, stock: true, stockQuantity: 5, description: "Persistent recovery fixture", active: true };
  const bytes = Buffer.concat([
    Buffer.from("--" + boundary + '\r\nContent-Disposition: form-data; name="data"\r\nContent-Type: application/json\r\n\r\n' + JSON.stringify(data) + "\r\n--" + boundary + '\r\nContent-Disposition: form-data; name="pic"; filename="recovery.png"\r\nContent-Type: image/png\r\n\r\n'),
    image, Buffer.from("\r\n--" + boundary + "--\r\n"),
  ]);
  return api(target, "/product", { method: "POST", bytes, contentType: "multipart/form-data; boundary=" + boundary, token });
}

async function seed() {
  for (const [username, phone] of [["recovery-admin", "1111111111"], ["recovery-buyer", "2222222222"]]) {
    await api(project, "/user", { method: "POST", data: { name: "Recovery User", username, email: username + "@example.test", phone, password } });
  }
  sql(project, "UPDATE users SET role='Admin' WHERE username='recovery-admin';");
  product = await upload(project, "Recovery fixture");
  order = await api(project, "/checkout", { method: "POST", token: await login(project, "recovery-buyer"), data: { idempotencyKey: "recovery-order-001", paymentMode: "COD", products: [{ productId: product.id, qty: 2 }] } });
  assert.equal(order.total, 44.97);
}

async function fixture(target) {
  const saved = await api(target, "/product/" + product.id);
  assert.equal(saved.name, "Recovery fixture");
  assert.equal(saved.stockQuantity, 3);
  const orders = await api(target, "/checkout/me", { token: await login(target, "recovery-buyer") });
  assert.equal(orders.length, 1);
  assert.equal(orders[0].id, order.id);
  assert.equal(orders[0].total, 44.97);
  const response = await request(configurations.get(target).origin + product.pics[0], ca(target));
  assert.equal(response.status, 200);
  assert.deepEqual(response.bytes, image);
}

function latestBackup(target) {
  const parent = path.join(root, "backups", target);
  const directories = fs.readdirSync(parent).sort().reverse().map(name => path.join(parent, name));
  return directories.find(directory => fs.existsSync(path.join(directory, "manifest.json")));
}

function makeRelease(name, broken) {
  const source = current(project);
  for (const service of ["backend", "frontend"]) {
    const container = "ducart-drill-image-" + service + "-" + suffix;
    execute(docker, ["create", "--name", container, source[service]], { capture: true });
    try {
      const changes = ["--change", "LABEL org.opencontainers.image.revision=" + name];
      if (broken && service === "backend") {
        const directory = path.join(reportDir, "migration-" + suffix);
        fs.mkdirSync(directory);
        const migration = path.join(directory, "V2__drill_partial_failure.sql");
        fs.writeFileSync(migration, "CREATE TABLE drill_partial_change (id INT PRIMARY KEY);\nALTER TABLE product ADD COLUMN drill_added INT;\nINSERT INTO drill_missing_table VALUES (1);\n");
        execute(docker, ["cp", directory, container + ":/app/drill-migrations"]);
        fs.unlinkSync(migration);
        fs.rmdirSync(directory);
        changes.push("--change", "ENV SPRING_FLYWAY_LOCATIONS=classpath:db/migration,filesystem:/app/drill-migrations");
      }
      if (service === "frontend") {
        const file = path.join(reportDir, "release-" + suffix + ".json");
        fs.writeFileSync(file, JSON.stringify({ release: name }));
        execute(docker, ["cp", file, container + ":/usr/share/nginx/html/release.json"]);
        fs.unlinkSync(file);
      }
      execute(docker, ["commit", ...changes, container, "ducart-" + service + ":" + name], { capture: true });
    } finally { execute(docker, ["rm", container], { capture: true }); }
  }
}

async function main() {
  await init(project);
  if (releaseIndex < 0) ops("build", project, ["--release", release]);
  ops("deploy", project, ["--release", release]);
  checks.push("HTTPS certificate verification, redirect, CORS, readiness and private ports");
  await seed();
  await fixture(project);
  checks.push("Real API registration, image upload and persisted purchase");

  const originalEnv = fs.readFileSync(configurations.get(project).file, "utf8");
  try {
    fs.writeFileSync(configurations.get(project).file, originalEnv.replace(/^MYSQL_ROOT_PASSWORD=.*$/m, "MYSQL_ROOT_PASSWORD=unexpected-drift"));
    ops("backup", project, [], { fail: true });
  } finally { fs.writeFileSync(configurations.get(project).file, originalEnv); }
  await fixture(project);
  checks.push("Database credential drift is rejected before touching existing volumes");

  dc(project, ["run", "--rm", "-T", "--no-deps", "maintenance", "touch", "/data/active"]);
  const blocked = await request(configurations.get(project).origin + "/api/user/login", ca(project), { method: "POST", body: "{}", headers: { "Content-Type": "application/json" } });
  assert.equal(blocked.status, 503);
  dc(project, ["run", "--rm", "-T", "--no-deps", "maintenance", "rm", "-f", "/data/active"]);
  checks.push("Maintenance rejects writes before backup/deploy/restore");

  dc(project, ["up", "-d", "--no-build", "--pull", "never", "--force-recreate", "--wait", "--wait-timeout", "240", "mysql", "backend", "frontend", "gateway"]);
  ops("verify", project);
  await fixture(project);
  checks.push("Container recreation preserves database and uploaded image");

  dc(project, ["stop", "mysql"]);
  let unhealthy;
  try { unhealthy = (await request(configurations.get(project).origin + "/healthz", ca(project))).status >= 500; }
  catch { unhealthy = true; }
  assert.equal(unhealthy, true);
  dc(project, ["up", "-d", "--wait", "--wait-timeout", "180", "mysql"]);
  ops("verify", project);
  checks.push("Readiness fails during database outage and recovers");

  ops("backup", project);
  const backup = latestBackup(project);
  const database = path.join(backup, "database.sql");
  const original = fs.readFileSync(database);
  try {
    fs.appendFileSync(database, "\n-- corrupted drill backup\n");
    ops("restore", project, ["--backup", backup, "--confirm-project", project], { fail: true });
  } finally { fs.writeFileSync(database, original); }
  await fixture(project);
  checks.push("Corrupt backup is rejected before changing live data");

  sql(project, "UPDATE product SET name='Damaged', stock_quantity=0;");
  const picturePath = product.pics[0].replace(/^\/uploads/, "");
  assert.ok(/^\/products\/[a-zA-Z0-9_.-]+$/.test(picturePath));
  dc(project, ["run", "--rm", "-T", "--no-deps", "files", "rm", "/data" + picturePath]);
  assert.equal((await api(project, "/product/" + product.id)).name, "Damaged");
  const restoredAt = Date.now();
  ops("restore", project, ["--backup", backup, "--confirm-project", project]);
  await fixture(project);
  const restoreSeconds = (Date.now() - restoredAt) / 1000;
  checks.push("Database and image restore repairs deliberately damaged fixtures");

  await init(clone);
  const recoveredAt = Date.now();
  ops("restore", clone, ["--backup", backup, "--confirm-project", clone, "--allow-cross-project", "--restore-images"]);
  await fixture(clone);
  const newProduct = await upload(clone, "Recovered upload");
  assert.ok(newProduct.id > product.id);
  assert.equal((await request(configurations.get(clone).origin + newProduct.pics[0], ca(clone))).status, 200);
  const freshRestoreSeconds = (Date.now() - recoveredAt) / 1000;
  checks.push("Fresh-volume recovery loads archived images, migrates/validates and serves the original order/image");

  const next = "drill-next-" + suffix;
  makeRelease(next, false);
  ops("deploy", project, ["--release", next]);
  assert.equal(current(project).release, next);
  sql(project, "UPDATE product SET name='Changed after deployment';");
  ops("rollback", project, ["--confirm-project", project, "--restore-data"]);
  assert.equal(current(project).release, release);
  await fixture(project);
  checks.push("Explicit rollback restores previous images and pre-deploy data");

  const bad = "drill-broken-" + suffix;
  makeRelease(bad, true);
  ops("deploy", project, ["--release", bad, "--wait-timeout", "45"], { fail: true });
  assert.equal(current(project).release, release);
  await fixture(project);
  const failures = path.join(root, "deploy/.state/failures");
  const schemas = fs.readdirSync(failures).filter(name => name.startsWith(project + "-") && name.endsWith(".schema.sql")).sort();
  assert.match(fs.readFileSync(path.join(failures, schemas.at(-1)), "utf8"), /drill_partial_change/);
  const result = dc(project, ["exec", "-T", "mysql", "sh", "-ec", 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root "$MYSQL_DATABASE" -Nse "SELECT (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=\'drill_partial_change\') + (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND column_name=\'drill_added\') + (SELECT COUNT(*) FROM flyway_schema_history WHERE version=\'2\')"'], { capture: true });
  assert.equal(result, "0");
  checks.push("Partial nontransactional Flyway DDL failure automatically restores the previous schema, images, data and uploads");

  const report = { passed: true, testedAt: new Date().toISOString(), gitHead: execute("git", ["rev-parse", "HEAD"], { capture: true }), workingTreeDirty: Boolean(execute("git", ["status", "--porcelain"], { capture: true })), project, recoveryProject: clone, release, checks, restoreSeconds, freshRestoreSeconds, totalSeconds: (Date.now() - started) / 1000, publicCloudDeployed: false, tls: "Local CA verified by the HTTPS client; no certificate validation bypass" };
  fs.writeFileSync(path.join(reportDir, "latest.json"), JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report, null, 2));
}

main().catch(error => {
  fs.writeFileSync(path.join(reportDir, "latest.json"), JSON.stringify({ passed: false, testedAt: new Date().toISOString(), project, checks, error: error.message }, null, 2));
  console.error(error.stack);
  process.exitCode = 1;
}).finally(() => {
  if (!process.argv.includes("--keep")) {
    let cleanedUp = true;
    for (const [target, config] of configurations) {
      if (!/^ducart-drill-(restore-)?[a-f0-9]{8}$/.test(target)) throw new Error("Refusing cleanup of a non-drill project");
      try {
        dc(target, ["down", "--volumes", "--remove-orphans"]);
        const backupBase = path.resolve(root, "backups");
        const backupDirectory = path.resolve(backupBase, target);
        if (path.dirname(backupDirectory) !== backupBase) throw new Error("Invalid drill backup cleanup path");
        fs.rmSync(backupDirectory, { recursive: true, force: true });
        for (const file of [config.file, path.join(root, "deploy/.state", target + ".json"), path.join(root, "deploy/.state", target + "-root.crt")]) {
          if (fs.existsSync(file)) fs.unlinkSync(file);
        }
      } catch (error) {
        cleanedUp = false;
        process.exitCode = 1;
        console.error("Drill cleanup failed: " + error.message);
      }
    }
    const reportFile = path.join(reportDir, "latest.json");
    if (fs.existsSync(reportFile)) {
      const report = JSON.parse(fs.readFileSync(reportFile, "utf8"));
      if (report.project === project) fs.writeFileSync(reportFile, JSON.stringify({ ...report, cleanedUp }, null, 2));
    }
  }
});
