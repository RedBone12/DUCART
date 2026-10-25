# Password reset and user responses

The phone-based password replacement endpoint has been removed. The browser now
requests an email link and then submits that link's token with a new password.

## API and token lifecycle

POST `/user/forgot-password` accepts `{"usernameOrEmail":"buyer@example.com"}`.
It returns HTTP 202 and the same generic message for registered and unknown
accounts, account throttling, and SMTP failures. IP throttling returns HTTP 429
with Retry-After: 900. Tokens never appear in API responses or application logs.

Each link uses 32 cryptographically random bytes encoded as 43 URL-safe characters.
Only its SHA-256 hash is stored. Links expire after 15 minutes, can be used once,
and are replaced when a new link is successfully sent. If SMTP delivery fails,
the database transaction rolls back, retaining a previously delivered valid link.
Sending mail and committing the database are not a distributed transaction: a
rare commit failure after delivery can produce an unusable link; request another.

POST `/user/reset-password` accepts `{"token":"...","newPassword":"..."}`.
Passwords require at least 12 characters and at most 72 UTF-8 bytes for BCrypt.
The user row and token are locked in consistent order. Password replacement and
token consumption commit together. Parallel use of one link succeeds once.
Invalid, expired and consumed links return the same HTTP 400 message.

The existing JWT authentication design is unchanged: changing a password does
not revoke previously issued JWTs; their existing expiry still applies.

## Request limits

The database stores hashed rate-limit bucket keys, not raw client addresses or
account identifiers. Limits are shared across backend instances and persist over
restarts. Each 15-minute window permits ten reset-link requests per remote IP,
three emails per account (username and email share a bucket), and thirty password
confirmation attempts per remote IP. Expired limit rows are cleaned after a day
when processing requests. First-insert conflicts and transient database deadlocks
are retried in fresh transactions.

The server uses the actual remote address and ignores client-supplied forwarding
headers. Behind a reverse proxy, configure trusted proxy address forwarding before
deployment so legitimate clients do not all share the proxy's IP quota. Generic
responses reduce account disclosure but synchronous mail delivery is not a
constant-time operation.

## Local email capture

Run from the project root:

```powershell
docker compose --env-file .env --profile mail up -d --build
```

Open the app at http://localhost:3000/forgot-password and the Mailpit inbox at
http://localhost:8025. Mailpit SMTP is bound to 127.0.0.1:1025, and its inbox to
127.0.0.1:8025. It captures messages locally instead of delivering external email.
The backend container connects to `mailpit:1025`; a directly launched backend
defaults to `localhost:1025`.

For real email, configure MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD,
MAIL_SMTP_AUTH and MAIL_STARTTLS through the runtime environment. Set
PASSWORD_RESET_MAIL_FROM and PASSWORD_RESET_FRONTEND_URL to the real sender and
HTTPS frontend route. For GitHub Pages hash routing, include `/#/forgot-password`
in that configured route. No request Host header is used to construct links.
The frontend sends no referrer information, avoiding leakage of link query tokens.

## Database and user responses

`database/password-reset.sql` adds the token and limit tables. The Docker Compose
DUCART database was backed up before the checkout migration on 25 October 2026;
both the checkout schema changes and password reset tables have now been applied.
Backups remain under the ignored backups/ directory. The native MySQL95 instance
was not changed.

User listing, detail, profile update and administrator update endpoints return
UserResponseDTO, which has no password field. Signup and login retain their
existing safe response DTOs. The User entity also marks its password write-only
for JSON and excludes it from generated toString output as defense in depth.

PasswordResetIntegrationTest covers token hashing, expiry, replay, replacement,
mail failure rollback, concurrent consumption, rate-limit races, account/IP
limits, rejection of the old endpoint and password-free responses. It uses mocked
SMTP in automated tests; a separate local smoke check exercises real Mailpit.

To repeat the local smoke check after starting Docker Compose, use PowerShell 7:

```powershell
pwsh -File scripts/Test-PasswordResetMailpit.ps1
```

It creates a randomly named test account, verifies SMTP delivery, resets the
password, rejects token replay and the previous password, logs in with the new
password, checks the profile response, and removes only that test account and its
token. Captured test emails remain in Mailpit; their links become unusable after
the account is removed. No passwords or tokens are printed by the script.

Verified on 25 October 2026: all 470 backend tests and all 331 frontend tests
passed; Maven packaging and the frontend production build succeeded. Fourteen
password-reset integration tests also passed against an isolated MySQL 8.4.11
database. The real SMTP smoke check passed against the migrated Compose database
and the local Mailpit service. Temporary test database containers were removed.
The final Compose application was started and the smoke check passed again on
its normal port 8080; the frontend returned HTTP 200 on port 3000. The local
backend runtime image was refreshed from the verified Maven JAR, and the frontend
image was built from source. The separate backend source-image build was stopped
during its initial Maven plugin prefetch; that complete Dockerfile build is not
claimed as verified in this run. The source Dockerfiles remain unchanged.
