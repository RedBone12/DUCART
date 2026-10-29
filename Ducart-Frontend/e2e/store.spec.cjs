const { test, expect } = require("@playwright/test");
const { randomUUID } = require("crypto");
const api = "http://127.0.0.1:18080";

async function login(page, username, password) {
  await page.goto("/login");
  await page.getByPlaceholder("User Name", { exact: true }).fill(username);
  await page.getByPlaceholder("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Login", exact: true }).click();
  await expect(page).toHaveURL(/\/(profile|admin)$/);
}

async function registerBuyer(page) {
  const username = "buyer" + randomUUID().slice(0, 8);
  const password = "BrowserBuyer123!";
  await page.goto("/signup");
  await page.getByPlaceholder("Full Name").fill("Browser Buyer");
  await page.getByPlaceholder("User Name", { exact: true }).fill(username);
  await page.getByPlaceholder("Email Address").fill(username + "@example.test");
  await page.getByPlaceholder("Phone Number").fill(Date.now().toString().slice(-10));
  await page.getByPlaceholder("Password", { exact: true }).fill(password);
  await page.getByPlaceholder("Confirm Password").fill(password);
  await page.getByRole("button", { name: "Signup", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await login(page, username, password);
}

test.beforeEach(async ({ page }) => {
  page.on("dialog", (dialog) => dialog.accept());
});

test("buyer registers, searches, adds to cart, purchases and reads persisted order history", async ({ page, request }) => {
  await registerBuyer(page);
  const before = await (await request.get(api + "/product/1")).json();
  await page.goto("/shop");
  await page.getByRole("searchbox").fill("Browser Seed Product");
  await page.getByRole("button", { name: "Search products" }).click();
  const card = page.locator(".card").filter({ has: page.getByText("Browser Seed Product", { exact: true }) });
  await card.getByRole("link", { name: "Add To Cart", exact: true }).click();
  await page.getByRole("button", { name: "Add to Cart", exact: true }).click();
  await expect(page).toHaveURL(/\/cart$/);
  await expect(page.getByRole("row").filter({ hasText: "Browser Seed Product" })).toBeVisible();
  await page.getByRole("link", { name: "Checkout", exact: true }).click();
  const confirmed = page.waitForResponse((response) => response.url() === api + "/checkout" && response.request().method() === "POST");
  await page.getByRole("button", { name: "Place Order", exact: true }).click();
  const response = await confirmed;
  expect(response.status()).toBe(201);
  const order = await response.json();
  expect(order.total).toBe(29.98);
  expect(order.paymentStatus).toBe("Pending");
  await expect(page).toHaveURL(/\/confirmation$/);
  await expect(page.getByText(new RegExp("Order #" + order.id))).toBeVisible();
  await page.goto("/profile");
  await expect(page.getByRole("row", { name: "Order Id " + order.id, exact: true })).toBeVisible();
  await expect(page.getByText("Browser Seed Product", { exact: true })).toBeVisible();
  const after = await (await request.get(api + "/product/1")).json();
  expect(after.stockQuantity).toBe(before.stockQuantity - 1);
});

test("administrator creates, edits and deletes a product through the real UI", async ({ page, request }) => {
  await login(page, "e2e-admin", "BrowserAdmin123!");
  await page.goto("/admin/product");
  await page.getByRole("link", { name: "Create product", exact: true }).click();
  const name = "Browser Managed " + Date.now();
  await page.getByRole("textbox", { name: "Product name", exact: true }).fill(name);
  await page.locator('select[name="maincategory"]').selectOption({ label: "BrowserCategory" });
  await page.locator('select[name="subcategory"]').selectOption({ label: "BrowserSubcategory" });
  await page.locator('select[name="brand"]').selectOption({ label: "BrowserBrand" });
  await page.getByPlaceholder("Product Color").fill("Green");
  await page.getByPlaceholder("Product Size").fill("Large");
  await page.getByPlaceholder("Product Base Price").fill("40");
  await page.getByPlaceholder("Product Discount").fill("10");
  await page.locator('input[name="stockQuantity"]').fill("4");
  const description = "A browser-created product with a real database and image.";
  await page.getByRole("textbox", { name: "Description", exact: true }).fill(description);
  const pictureBytes = Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jF1sAAAAASUVORK5CYII=", "base64");
  await page.locator('input[type="file"]').setInputFiles({
    name: "browser.png", mimeType: "image/png",
    buffer: pictureBytes,
  });
  const created = page.waitForResponse((response) => response.url() === api + "/product" && response.request().method() === "POST");
  await page.getByRole("button", { name: "Create", exact: true }).click();
  const creation = await created;
  expect(creation.status()).toBe(201);
  const product = await creation.json();
  expect(product.finalPrice).toBe(36);
  expect(product.description).toBe(description);
  const picture = await request.get(api + product.pics[0]);
  expect(picture.status()).toBe(200);
  expect(picture.headers()["content-type"]).toMatch(/image\/png/);
  expect(await picture.body()).toEqual(pictureBytes);
  await page.getByRole("link", { name: "Edit " + name, exact: true }).click();
  const updatedName = name + " Updated";
  await expect(page.getByRole("textbox", { name: "Product name", exact: true })).toHaveValue(name);
  await page.getByRole("textbox", { name: "Product name", exact: true }).fill(updatedName);
  const updated = page.waitForResponse((response) => response.url() === api + "/product/" + product.id && response.request().method() === "PUT");
  await page.getByRole("button", { name: "Update", exact: true }).click();
  expect((await updated).status()).toBe(200);
  const persisted = await request.get(api + "/product/" + product.id);
  expect((await persisted.json()).name).toBe(updatedName);
  await expect(page.getByRole("link", { name: "Edit " + updatedName, exact: true })).toBeVisible();
  const deleted = page.waitForResponse((response) => response.url() === api + "/product/" + product.id && response.request().method() === "DELETE");
  await page.getByRole("button", { name: "Delete " + updatedName, exact: true }).click();
  expect((await deleted).status()).toBe(200);
  await expect(page.getByRole("button", { name: "Delete " + updatedName, exact: true })).toHaveCount(0);
  expect((await request.get(api + "/product/" + product.id)).status()).toBe(404);
});

test("a buyer cannot use administrator pages or product-write APIs", async ({ page, request }) => {
  await registerBuyer(page);
  await page.goto("/admin/product/create");
  await expect(page).not.toHaveURL(/\/admin\//);
  const token = await page.evaluate(() => localStorage.getItem("token"));
  const blocked = await request.post(api + "/product", {
    headers: { Authorization: "Bearer " + token },
    multipart: { data: JSON.stringify({ name: "Unauthorized product" }) },
  });
  expect(blocked.status()).toBe(403);
});
