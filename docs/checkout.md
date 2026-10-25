# Checkout behavior

POST `/checkout` requires a buyer's bearer token and this JSON body:

```json
{
  "idempotencyKey": "f5ea73b8-26fb-4021-aeaa-e5d015eaa013",
  "paymentMode": "COD",
  "products": [{ "productId": 1, "qty": 2 }]
}
```

The authenticated username owns the order. Supplied prices, totals, stock counts,
owner, dates, payment status and order status are ignored. Old checkout requests
without product IDs and an idempotency key are rejected. Deploy the frontend and
backend together.

New and updated cart rows retain the product ID, so product renames do not change
the item purchased. Legacy rows without an ID use the catalogue-name lookup until
they are updated. The upgrade also adds the nullable cart.product_id column.

The backend loads each product under a pessimistic write lock, verifies active
status and available inventory, calculates the discounted unit price from the
database base price and discount, and rounds to two decimal places with HALF_UP.
Line amounts and order totals use BigDecimal; order totals persist as DECIMAL(19,2).
The existing catalogue/cart money fields remain unchanged; checkout converts the
database catalogue values to decimal before calculation and stores decimal order
line snapshots, independent of later catalogue edits.

Shipping is EUR 4.99 for a positive subtotal below EUR 100, otherwise free. These
rules match the frontend's current estimate in siteConfig.js. Initial order status
is `Order is Placed`, and payment status is `Pending` for every payment mode.
`Card Demo` and `PayPal Demo` do not charge money or verify payment.

Order persistence and inventory deductions share one transaction, including
rollback for checked exceptions. Products are locked in ascending ID order to
avoid reversed-basket deadlocks. Duplicate product lines are aggregated before
validation. Unavailable or insufficient stock returns 409; missing products return
404; invalid request data returns 400. Authentication is required (401 otherwise).

The buyer row is locked before looking up the idempotency key. This serializes
concurrent requests for the same buyer across backend instances. A database unique
constraint on buyer and key provides an additional invariant. The same key and
normalized basket/payment mode returns the original order, even if prices or stock
have changed; reuse for a different request returns 409. Different buyers may use
the same key. Idempotency lasts while the order is retained. Administrative deletion
of an order removes its associated key. A different key represents a new purchase.

The frontend keeps the pending key in sessionStorage, including across a page
refresh after a failed/lost response. It disables submission while waiting and
opens confirmation only after a successful response with an order ID. Failures
leave the cart intact. Successful responses initiate deletion of the purchased
cart rows and show the server-confirmed total on the confirmation page.

## Existing database upgrade

For an existing database, stop the backend and back up the database. Inspect its
schema, then apply `database/checkout-hardening.sql` once before restarting the
backend. Existing orders keep nullable request keys. The script is intentionally
not rerunnable and must not be run after Hibernate has already added the columns.
Converting historical DOUBLE totals rounds to two decimal places; check historical
orders during upgrade. This change does not introduce Flyway or execute a migration
against a running user database automatically.

## Verification

From `Ducart-Backend`, run `mvnw.cmd clean verify`. OrderPlacementIntegrationTest
uses a real Spring context, HTTP requests, repositories, transactions and an H2
database by default. It checks forged amounts/status/owner, decimal calculation,
stock rejection, concurrent buyers, concurrent retries and rollback after a
later-product failure or order persistence failure. The same test class can be run
against an isolated MySQL database by overriding spring.datasource properties;
it deletes test records and creates/drops tables, so never target a real database.

From `Ducart-Frontend`, run `npm.cmd test -- --watchAll=false --runInBand` and
`npm.cmd run build`. Checkout component and API tests check minimal payloads,
confirmed navigation, failures, duplicate-click suppression and retry keys.

Verified locally on 25 October 2026: 461 backend tests and 331 frontend tests
passed; the frontend production build succeeded with existing project warnings.
All 13 order placement integration tests also passed against an isolated MySQL
8.4.11 container. The database upgrade script was verified against old-style
tables and sample historical totals in that container. The container was removed
after verification. In the subsequent authorized database upgrade on 25 October
2026, the existing Docker Compose DUCART database was backed up and this migration
was applied. Its three order amount columns are now DECIMAL(19,2), and the request
key, request fingerprint, unique constraint and cart product ID have been verified.
