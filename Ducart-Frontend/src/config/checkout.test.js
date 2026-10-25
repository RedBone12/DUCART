import { submitOrder, pendingOrder, clearPendingOrder } from "./checkout";

beforeEach(() => {
  localStorage.clear();
  sessionStorage.clear();
  let sequence = 0;
  Object.defineProperty(window, "crypto", {
    configurable: true, value: { randomUUID: () => `request-key-${++sequence}` },
  });
  global.fetch = jest.fn();
});

test("sends bearer authentication and returns the server-confirmed order", async () => {
  localStorage.setItem("token", "buyer-token");
  const payload = pendingOrder("COD", [{ productId: 1, qty: 2 }]);
  global.fetch.mockResolvedValue({ ok: true, json: async () => ({ id: 9, total: 14.99 }) });
  await expect(submitOrder(payload)).resolves.toEqual({ id: 9, total: 14.99 });
  expect(global.fetch).toHaveBeenCalledWith(`${process.env.REACT_APP_SERVER}/checkout`, {
    method: "POST", headers: { "Content-Type": "application/json", Authorization: "Bearer buyer-token" },
    body: JSON.stringify(payload),
  });
});

test.each([401, 409, 500])("HTTP %s rejects instead of confirming checkout", async (status) => {
  global.fetch.mockResolvedValue({ ok: false, status, json: async () => ({ message: "Order rejected" }) });
  await expect(submitOrder({})).rejects.toThrow("Order rejected");
});

test("lost or malformed confirmations leave the retry key intact", async () => {
  const first = pendingOrder("COD", [{ productId: 1, qty: 1 }]);
  global.fetch.mockRejectedValueOnce(new Error("Network lost"));
  await expect(submitOrder(first)).rejects.toThrow("Network lost");
  expect(pendingOrder("COD", [{ productId: 1, qty: 1 }])).toEqual(first);
  global.fetch.mockResolvedValueOnce({ ok: true, json: async () => ({}) });
  await expect(submitOrder(first)).rejects.toThrow("confirmation was not received");
  expect(pendingOrder("COD", [{ productId: 1, qty: 1 }])).toEqual(first);
});

test("new quantities, payment modes and buyers do not reuse a pending request", () => {
  localStorage.setItem("username", "alice");
  const first = pendingOrder("COD", [{ productId: 1, qty: 1 }]);
  const changed = pendingOrder("COD", [{ productId: 1, qty: 2 }]);
  expect(changed.idempotencyKey).not.toBe(first.idempotencyKey);
  const payment = pendingOrder("Bank Transfer", [{ productId: 1, qty: 2 }]);
  expect(payment.idempotencyKey).not.toBe(changed.idempotencyKey);
  localStorage.setItem("username", "bob");
  expect(pendingOrder("COD", [{ productId: 1, qty: 1 }]).idempotencyKey).not.toBe(first.idempotencyKey);
  localStorage.setItem("username", "alice");
  clearPendingOrder();
  expect(sessionStorage.getItem("ducart-order:alice")).toBeNull();
  expect(sessionStorage.getItem("ducart-order:bob")).not.toBeNull();
});
