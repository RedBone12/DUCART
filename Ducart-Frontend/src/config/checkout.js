import { getToken } from "./auth";

export async function submitOrder(payload) {
  const response = await fetch(`${process.env.REACT_APP_SERVER}/checkout`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${getToken()}`,
    },
    body: JSON.stringify(payload),
  });
  const order = await response.json();
  if (!response.ok) throw new Error(order.message || "Unable to place order. Please retry.");
  if (!order.id) throw new Error("Order confirmation was not received. Please retry.");
  return order;
}

export function pendingOrder(paymentMode, products) {
  const storageKey = `ducart-order:${localStorage.getItem("username")}`;
  const body = { paymentMode, products };
  const fingerprint = JSON.stringify(body);
  let previous;
  try {
    previous = JSON.parse(sessionStorage.getItem(storageKey));
  } catch {
    previous = null;
  }
  const idempotencyKey = previous?.fingerprint === fingerprint
    ? previous.idempotencyKey
    : (window.crypto.randomUUID?.() || Array.from(
      window.crypto.getRandomValues(new Uint8Array(16)),
      (byte) => byte.toString(16).padStart(2, "0"),
    ).join(""));
  sessionStorage.setItem(storageKey, JSON.stringify({ fingerprint, idempotencyKey }));
  return { ...body, idempotencyKey };
}

export function clearPendingOrder() {
  sessionStorage.removeItem(`ducart-order:${localStorage.getItem("username")}`);
}
