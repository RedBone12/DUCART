#!/usr/bin/env node
// Cross-platform deployment operations. Docker arguments never contain credentials.
const fs = require("node:fs");
const path = require("node:path");
const crypto = require("node:crypto");
const https = require("node:https");
const http = require("node:http");
const { spawnSync } = require("node:child_process");

const root = path.resolve(__dirname, "..");
const composeFile = path.join(root, "deploy/compose.yaml");
const docker = process.env.DOCKER_EXE || "docker";
const options = {};
const args = require.main === module ? process.argv.slice(2) : [];
const command = args.shift();
while (args.length) {
  const key = args.shift();
  if (!key.startsWith("--")) throw new Error("Expected a named option");
  options[key.slice(2)] = args[0] && !args[0].startsWith("--") ? args.shift() : true;
}
const envPath = path.resolve(root, options.env || "deploy/production.env");
const project = options.project || "ducart-production";
if (!/^[a-z][a-z0-9-]{2,40}$/.test(project)) throw new Error("Invalid Compose project name");
const waitTimeout = Number(options["wait-timeout"] || 240);
if (!Number.isInteger(waitTimeout) || waitTimeout < 30 || waitTimeout > 900) throw new Error("Wait timeout must be between 30 and 900 seconds");
const stateDir = path.join(root, "deploy/.state");
const statePath = path.join(stateDir, project + ".json");
let settings;

function run(argv, opts = {}) {
  const inputFd = opts.inputFile ? fs.openSync(opts.inputFile, "r") : null;
  const outputFd = opts.outputFile ? fs.openSync(opts.outputFile, "wx", 0o600) : null;
  try {
    const result = spawnSync(docker, argv, {
      cwd: root,
      env: { ...process.env, ...settings, ...opts.env },
      input: opts.input,
      stdio: [inputFd ?? "pipe", outputFd ?? (opts.capture ? "pipe" : "inherit"), "inherit"],
      maxBuffer: 16 * 1024 * 1024,
      timeout: opts.timeout || 20 * 60 * 1000,
      windowsHide: true,
    });
    if (result.error) throw result.error;
    if (result.status !== 0) throw new Error("Docker operation failed: " + argv.slice(0, 2).join(" "));
    return result.stdout?.toString("utf8").trim();
  } finally {
    if (inputFd !== null) fs.closeSync(inputFd);
    if (outputFd !== null) fs.closeSync(outputFd);
  }
}

function compose(argv, images, opts = {}) {
  return run(["compose", "--env-file", envPath, "-f", composeFile, "-p", project, ...argv], {
    ...opts,
    env: {
      BACKEND_IMAGE: images?.backend || "ducart-backend:unselected",
      FRONTEND_IMAGE: images?.frontend || "ducart-frontend:unselected",
      RELEASE_ID: images?.release || "unselected",
      COMPOSE_PROFILES: settings.TLS_DIRECTIVE === "tls internal" ? "local" : "",
      ...opts.env,
    },
  });
}

function readEnv() {
  const values = {};
  for (const line of fs.readFileSync(envPath, "utf8").split(/\r?\n/)) {
    if (!line.trim() || line.trim().startsWith("#")) continue;
    const match = line.match(/^([A-Z][A-Z0-9_]*)=(.*)$/);
    if (!match) throw new Error("Use KEY=value syntax in the environment file");
    let value = match[2].trim();
    if (value.startsWith('"')) value = JSON.parse(value);
    else if (value.startsWith("'")) value = value.slice(1, -1);
    values[match[1]] = value;
  }
  for (const key of ["MYSQL_DATABASE", "MYSQL_USER"]) {
    if (!/^[a-z][a-z0-9_]{0,63}$/.test(values[key] || "")) throw new Error("Invalid " + key);
  }
  for (const key of ["MYSQL_PASSWORD", "MYSQL_ROOT_PASSWORD", "JWT_SECRET", "MAIL_HOST", "PASSWORD_RESET_MAIL_FROM"]) {
    if (!values[key] || values[key].startsWith("GENERATE_")) throw new Error("Configure " + key);
  }
  const origin = new URL(values.PUBLIC_ORIGIN);
  if (origin.protocol !== "https:" || origin.origin !== values.PUBLIC_ORIGIN || origin.hostname !== values.SITE_HOST) {
    throw new Error("PUBLIC_ORIGIN must be an HTTPS origin matching SITE_HOST");
  }
  if (Number(origin.port || 443) !== Number(values.HTTPS_PORT || 443)) throw new Error("HTTPS port must match PUBLIC_ORIGIN");
  if (values.TLS_DIRECTIVE && values.TLS_DIRECTIVE !== "tls internal") throw new Error("Invalid TLS_DIRECTIVE");
  if (values.TLS_DIRECTIVE && !["localhost", "127.0.0.1"].includes(origin.hostname)) {
    throw new Error("Internal certificates are restricted to the local drill");
  }
  return values;
}

function state() {
  if (!fs.existsSync(statePath)) return null;
  const value = JSON.parse(fs.readFileSync(statePath, "utf8"));
  if (value.envPath !== envPath) throw new Error("Project already belongs to another environment file");
  if (value.databaseIdentity && value.databaseIdentity !== databaseIdentity()) throw new Error("Database identity changed for existing volumes; restore the original environment or recover into a new project");
  return value;
}

function databaseIdentity() {
  return crypto.createHash("sha256").update(JSON.stringify([settings.MYSQL_DATABASE, settings.MYSQL_USER, settings.MYSQL_PASSWORD, settings.MYSQL_ROOT_PASSWORD])).digest("hex");
}

function saveState(current, previous = null) {
  fs.mkdirSync(stateDir, { recursive: true, mode: 0o700 });
  const temporary = statePath + ".tmp";
  fs.writeFileSync(temporary, JSON.stringify({ envPath, databaseIdentity: databaseIdentity(), current, previous }, null, 2), { mode: 0o600 });
  fs.renameSync(temporary, statePath);
}

function inspect(reference) {
  return JSON.parse(run(["image", "inspect", reference], { capture: true }))[0];
}

function assertOwnership() {
  const ids = run(["ps", "-aq", "--filter", "label=com.docker.compose.project=" + project], { capture: true });
  if (!ids) return;
  const containers = JSON.parse(run(["inspect", ...ids.split(/\s+/)], { capture: true }));
  for (const container of containers) {
    const files = container.Config.Labels?.["com.docker.compose.project.config_files"] || "";
    if (!files.split(",").some(file => path.resolve(file) === composeFile)) {
      throw new Error("Project contains containers from a different Compose file; choose a separate project name");
    }
  }
}

function releaseImages(release) {
  if (typeof release !== "string" || !/^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,63}$/.test(release)) throw new Error("Specify --release with an immutable release name");
  const backend = inspect("ducart-backend:" + release);
  const frontend = inspect("ducart-frontend:" + release);
  for (const image of [backend, frontend]) {
    if (image.Config.Labels?.["org.opencontainers.image.revision"] !== release) throw new Error("Image release label mismatch");
  }
  return { release, backend: backend.Id, frontend: frontend.Id };
}

function maintenance(enabled, images) {
  compose(["run", "--rm", "-T", "--no-deps", "maintenance", ...(enabled ? ["touch", "/data/active"] : ["rm", "-f", "/data/active"])], images);
}

function ensureInfrastructure() {
  const references = ["mysql:8.4", "caddy:2-alpine", "alpine:3.24"];
  if (settings.TLS_DIRECTIVE === "tls internal") references.push("axllent/mailpit:v1");
  for (const reference of references) {
    if (!run(["image", "ls", "-q", reference], { capture: true })) run(["pull", reference]);
  }
}

function start(images) {
  compose(["up", "-d", "--no-build", "--pull", "never", "--wait", "--wait-timeout", String(waitTimeout)], images);
}

async function hash(file) {
  const digest = crypto.createHash("sha256");
  for await (const chunk of fs.createReadStream(file)) digest.update(chunk);
  return digest.digest("hex");
}

async function backup(images, keepStopped = false) {
  if (!images) throw new Error("Deploy a verified release before backing up");
  await verify(images);
  const directory = path.join(root, "backups", project, new Date().toISOString().replace(/[:.]/g, "-") + "-" + crypto.randomBytes(3).toString("hex"));
  fs.mkdirSync(directory, { recursive: true, mode: 0o700 });
  run(["image", "save", "--output", path.join(directory, "images.tar"), images.backend, images.frontend]);
  fs.chmodSync(path.join(directory, "images.tar"), 0o600);
  maintenance(true, images);
  compose(["stop", "backend"], images);
  try {
    compose(["exec", "-T", "mysql", "sh", "-ec", 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -u root --single-transaction --routines --events --triggers --no-tablespaces --set-gtid-purged=OFF --hex-blob "$MYSQL_DATABASE"'], images, { outputFile: path.join(directory, "database.sql") });
    compose(["run", "--rm", "-T", "--no-deps", "files", "tar", "-C", "/data", "-cf", "-", "."], images, { outputFile: path.join(directory, "uploads.tar") });
    const hashes = {};
    for (const file of ["database.sql", "uploads.tar", "images.tar"]) hashes[file] = await hash(path.join(directory, file));
    fs.writeFileSync(path.join(directory, "manifest.json"), JSON.stringify({ format: 1, project, database: settings.MYSQL_DATABASE, created: new Date().toISOString(), images, hashes }, null, 2), { mode: 0o600 });
    console.log("Complete backup: " + directory);
    return directory;
  } finally {
    if (!keepStopped) {
      compose(["start", "backend"], images);
      await verify(images);
      maintenance(false, images);
    }
  }
}

async function readBackup(directory) {
  directory = path.resolve(directory);
  const manifest = JSON.parse(fs.readFileSync(path.join(directory, "manifest.json"), "utf8"));
  if (manifest.format !== 1 || manifest.database !== settings.MYSQL_DATABASE) throw new Error("Incompatible backup format or database");
  if (manifest.project !== project && !options["allow-cross-project"]) throw new Error("Backup project differs; explicitly use --allow-cross-project for disaster recovery");
  for (const file of ["database.sql", "uploads.tar", "images.tar"]) {
    if (await hash(path.join(directory, file)) !== manifest.hashes[file]) throw new Error("Backup checksum mismatch: " + file);
  }
  for (const key of ["backend", "frontend"]) {
    if (!/^sha256:[a-f0-9]{64}$/.test(manifest.images[key])) throw new Error("Invalid backup image ID");
  }
  const localImages = run(["image", "ls", "--no-trunc", "--quiet"], { capture: true }).split(/\s+/);
  if (options["restore-images"] || ![manifest.images.backend, manifest.images.frontend].every(image => localImages.includes(image))) {
    run(["image", "load", "--input", path.join(directory, "images.tar")]);
  }
  for (const key of ["backend", "frontend"]) inspect(manifest.images[key]);
  return manifest;
}

async function restore(directory, internal = false) {
  if (!internal && options["confirm-project"] !== project) throw new Error("Restoration replaces database and uploads; specify --confirm-project " + project);
  const manifest = await readBackup(directory); // Validate all files before stopping or changing anything.
  const images = manifest.images;
  maintenance(true, images);
  compose(["stop", "backend"], images);
  compose(["up", "-d", "--wait", "--wait-timeout", "180", "mysql"], images);
  const reset = "DROP DATABASE `" + settings.MYSQL_DATABASE + "`; CREATE DATABASE `" + settings.MYSQL_DATABASE + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;";
  compose(["exec", "-T", "mysql", "sh", "-ec", 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root'], images, { input: reset });
  compose(["exec", "-T", "mysql", "sh", "-ec", 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root "$MYSQL_DATABASE"'], images, { inputFile: path.join(directory, "database.sql") });
  compose(["run", "--rm", "-T", "--no-deps", "files", "sh", "-ec", "find /data -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +; tar -C /data -xf -"], images, { inputFile: path.join(directory, "uploads.tar") });
  start(images);
  await verify(images);
  saveState(images);
  maintenance(false, images);
  console.log("Restored and verified release " + images.release);
}

function request(url, ca, extra = {}) {
  return new Promise((resolve, reject) => {
    const library = url.startsWith("https:") ? https : http;
    const req = library.request(url, { ca, agent: false, timeout: 10000, ...(new URL(url).hostname === "localhost" ? { family: 4 } : {}), ...extra }, response => {
      const buffers = [];
      response.on("data", chunk => buffers.push(chunk));
      response.on("end", () => {
        const bytes = Buffer.concat(buffers);
        resolve({ status: response.statusCode, headers: response.headers, body: bytes.toString("utf8"), bytes });
      });
    });
    req.on("error", reject);
    req.on("timeout", () => req.destroy(new Error("HTTPS verification timed out")));
    req.end(extra.body);
  });
}

function trustedCA(images) {
  if (settings.TLS_DIRECTIVE !== "tls internal") return undefined;
  fs.mkdirSync(stateDir, { recursive: true, mode: 0o700 });
  const target = path.join(stateDir, project + "-root.crt");
  compose(["cp", "gateway:/data/caddy/pki/authorities/local/root.crt", target], images);
  return fs.readFileSync(target);
}

async function verify(images) {
  let failure;
  for (let attempt = 0; attempt < 30; attempt++) {
    try {
      const ca = trustedCA(images);
      for (const route of ["/", "/shop", "/healthz", "/api/product", "/release.json"]) {
        const response = await request(settings.PUBLIC_ORIGIN + route, ca);
        if (response.status !== 200) throw new Error(route + " returned " + response.status);
        if (route === "/healthz" && JSON.parse(response.body).status !== "UP") throw new Error("Database readiness is not UP");
        if (route === "/release.json" && JSON.parse(response.body).release !== images.release) throw new Error("Served release mismatch");
      }
      const denied = await request(settings.PUBLIC_ORIGIN + "/api/user/login", ca, { method: "OPTIONS", headers: { Origin: "https://untrusted.example", "Access-Control-Request-Method": "POST" } });
      const hidden = await request(settings.PUBLIC_ORIGIN + "/api/actuator/health", ca);
      if (hidden.status !== 404) throw new Error("Management endpoint exposed through public API");
      if (denied.status !== 403 || denied.headers["access-control-allow-origin"]) throw new Error("Untrusted CORS origin accepted");
      const allowed = await request(settings.PUBLIC_ORIGIN + "/api/user/login", ca, { method: "OPTIONS", headers: { Origin: settings.PUBLIC_ORIGIN, "Access-Control-Request-Method": "POST" } });
      // A same-origin request may legitimately omit CORS response headers.
      if (allowed.status !== 200 || (allowed.headers["access-control-allow-origin"] && allowed.headers["access-control-allow-origin"] !== settings.PUBLIC_ORIGIN)) throw new Error("Production origin rejected");
      const redirect = await request("http://" + settings.SITE_HOST + ":" + settings.HTTP_PORT + "/shop");
      if (redirect.status !== 308 || redirect.headers.location !== settings.PUBLIC_ORIGIN + "/shop") throw new Error("HTTPS redirect mismatch");
      const containers = JSON.parse(compose(["ps", "--format", "json"], images, { capture: true }).split(/\r?\n/).map(line => line.trim()).filter(Boolean).map(line => line.startsWith("[") ? line : "[" + line + "]").join(",").replace(/\],\[/g, ","));
      for (const name of ["mysql", "backend", "frontend", "gateway"]) {
        const service = containers.find(item => item.Service === name);
        if (!service || service.State !== "running") throw new Error("Service not running: " + name);
        if (["backend", "frontend"].includes(name)) {
          const runningImage = run(["inspect", "--format", "{{.Image}}", service.ID], { capture: true });
          if (runningImage !== inspect(images[name]).Id) throw new Error("Running image differs from verified release: " + name);
        }
        if (["mysql", "backend"].includes(name) && service.Publishers?.some(port => port.PublishedPort)) throw new Error("Internal service has a published port");
      }
      console.log("Verified HTTPS, SPA refresh, API, DB readiness, CORS and release " + images.release);
      return;
    } catch (error) {
      failure = error;
      await new Promise(resolve => setTimeout(resolve, 2000));
    }
  }
  throw failure;
}

async function deploy() {
  const images = releaseImages(options.release);
  const previous = state();
  let snapshot;
  maintenance(true, previous?.current || images);
  try {
    if (previous?.current) snapshot = await backup(previous.current, true);
    start(images);
    await verify(images);
    saveState(images, snapshot ? { images: previous.current, backup: snapshot } : null);
    maintenance(false, images);
  } catch (error) {
    console.error("Deployment failed; writes remain blocked during recovery.");
    try {
      const directory = path.join(stateDir, "failures");
      fs.mkdirSync(directory, { recursive: true, mode: 0o700 });
      const prefix = path.join(directory, project + "-" + Date.now());
      compose(["logs", "--no-color", "--tail", "150", "backend"], images, { outputFile: prefix + ".backend.log" });
      compose(["exec", "-T", "mysql", "sh", "-ec", 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -u root --no-data --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE"'], images, { outputFile: prefix + ".schema.sql" });
      console.error("Private failure diagnostics: " + prefix);
    } catch { console.error("Some failure diagnostics were unavailable; continuing recovery."); }
    if (snapshot) await restore(snapshot, true);
    else if (previous?.current) {
      start(previous.current);
      await verify(previous.current);
      maintenance(false, previous.current);
    } else compose(["stop", "backend", "frontend", "gateway"], images);
    throw error;
  }
}

async function main() {
  if (command === "init") {
    const origin = new URL(options.origin || "https://localhost:8443");
    if (origin.protocol !== "https:" || origin.origin !== origin.href.replace(/\/$/, "")) throw new Error("Specify an HTTPS origin");
    const local = options.local === true;
    if (!local && ["localhost", "127.0.0.1"].includes(origin.hostname)) throw new Error("Use --local for a localhost drill");
    const values = {
      PUBLIC_ORIGIN: origin.origin, SITE_HOST: origin.hostname, BIND_ADDRESS: local ? "127.0.0.1" : "0.0.0.0",
      HTTP_PORT: local ? "8088" : "80", HTTPS_PORT: origin.port || "443", TLS_DIRECTIVE: local ? "tls internal" : "",
      MYSQL_DATABASE: "ducart", MYSQL_USER: "ducart", MYSQL_PASSWORD: crypto.randomBytes(32).toString("hex"),
      MYSQL_ROOT_PASSWORD: crypto.randomBytes(32).toString("hex"), JWT_SECRET: crypto.randomBytes(48).toString("base64"),
      MAIL_HOST: local ? "mailpit" : "smtp.example.com", MAIL_PORT: local ? "1025" : "587",
      MAIL_USERNAME: "", MAIL_PASSWORD: "", MAIL_SMTP_AUTH: local ? "false" : "true", MAIL_STARTTLS: local ? "false" : "true",
      PASSWORD_RESET_MAIL_FROM: local ? "no-reply@example.test" : "no-reply@" + origin.hostname,
    };
    fs.mkdirSync(path.dirname(envPath), { recursive: true });
    fs.writeFileSync(envPath, Object.entries(values).map(([key, value]) => key + "=" + value).join("\n") + "\n", { flag: "wx", mode: 0o600 });
    console.log("Created private environment file: " + envPath);
    if (!local) console.log("Configure real SMTP before deployment; point DNS to the server and open TCP 80/443.");
    return;
  }
  settings = readEnv();
  if (["deploy", "restore", "rollback", "resume"].includes(command) && settings.TLS_DIRECTIVE !== "tls internal") {
    if (settings.MAIL_HOST === "smtp.example.com" || (settings.MAIL_SMTP_AUTH === "true" && (!settings.MAIL_USERNAME || !settings.MAIL_PASSWORD))) {
      throw new Error("Configure real SMTP before production deployment or recovery");
    }
  }
  assertOwnership();
  if (["deploy", "restore", "rollback"].includes(command)) ensureInfrastructure();
  const current = state()?.current;
  if (command === "build") {
    if (!/^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,63}$/.test(options.release || "")) throw new Error("Specify --release");
    const images = { release: options.release, backend: "ducart-backend:" + options.release, frontend: "ducart-frontend:" + options.release };
    const existing = run(["image", "ls", "-q", images.backend], { capture: true });
    if (existing) throw new Error("Release already exists; choose a new immutable release name");
    compose(["build", "backend", "frontend"], images);
    releaseImages(options.release);
  } else if (command === "deploy") await deploy();
  else if (command === "resume") { await verify(current); maintenance(false, current); }
  else if (command === "verify") await verify(current || releaseImages(options.release));
  else if (command === "backup") await backup(current);
  else if (command === "restore") await restore(path.resolve(options.backup || ""));
  else if (command === "rollback") {
    if (!state()?.previous) throw new Error("No previous verified release snapshot");
    if (options["confirm-project"] !== project) throw new Error("Specify --confirm-project " + project);
    if (!options["restore-data"]) throw new Error("Rollback rewinds data to the pre-deploy snapshot; explicitly specify --restore-data");
    await restore(state().previous.backup);
  } else throw new Error("Commands: init, build, deploy, verify, backup, restore, rollback");
}

if (require.main === module) main().catch(error => { console.error(error.message); process.exitCode = 1; });
module.exports = { request };
