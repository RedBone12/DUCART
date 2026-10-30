import { configureStore } from "@reduxjs/toolkit";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import ShopPage from "./ShopPage";

const product = { id: 1, name: "Blue Shoe", finalPrice: 20, basePrice: 20, pics: [], active: true };
const page = (overrides = {}) => ({ content: [product], page: 0, size: 12, totalElements: 25, totalPages: 3, ...overrides });
const response = (data, ok = true) => ({ ok, json: async () => data });
const requested = () => new URL(global.fetch.mock.calls.at(-1)[0], "http://localhost").searchParams;
function renderShop(route = "/shop") {
  const state = {
    MaincategoryStateData: [{ id: 1, name: "Clothes", active: true }, { id: 2, name: "Hidden", active: false }],
    SubcategoryStateData: [{ id: 1, name: "Shoes", active: true }],
    BrandStateData: [{ id: 1, name: "Acme", active: true }],
  };
  const store = configureStore({ reducer: () => state });
  render(<Provider store={store}><MemoryRouter initialEntries={[route]}><Routes>
    <Route path="/shop" element={<ShopPage />} />
  </Routes></MemoryRouter></Provider>);
}
const originalFetch = global.fetch;
beforeEach(() => { global.fetch = jest.fn().mockResolvedValue(response(page())); });
afterEach(() => { global.fetch = originalFetch; });

test("loads a server page without the full catalogue and preserves filter links", async () => {
  renderShop("/shop?q=blue&mc=Clothes&br=Acme&min=10&max=40&sort=priceAsc&page=1");
  expect(await screen.findByText("Blue Shoe")).toBeInTheDocument();
  expect(global.fetch.mock.calls[0][0]).toContain("/product/search?");
  expect(requested().get("q")).toBe("blue");
  const link = new URL(screen.getByRole("link", { name: "Shoes" }).href);
  expect(link.searchParams.get("mc")).toBe("Clothes");
  expect(link.searchParams.get("br")).toBe("Acme");
  expect(link.searchParams.get("q")).toBe("blue");
  expect(link.searchParams.has("page")).toBe(false);
  expect(screen.queryByText("Hidden")).not.toBeInTheDocument();
});

test("search, price and sort remain combined and reset pagination", async () => {
  renderShop("/shop?mc=Clothes&sc=Shoes&br=Acme&page=2");
  await screen.findByText("Blue Shoe");
  fireEvent.change(screen.getByPlaceholderText(/Search Products by Name/), { target: { value: " blue " } });
  fireEvent.click(screen.getByRole("button", { name: "Search products" }));
  await waitFor(() => expect(requested().get("q")).toBe("blue"));
  expect(requested().get("br")).toBe("Acme"); expect(requested().has("page")).toBe(false);
  await screen.findByText("Blue Shoe");
  fireEvent.change(screen.getByPlaceholderText("Min Amount"), { target: { value: "10" } });
  fireEvent.change(screen.getByPlaceholderText("Max Amount"), { target: { value: "40" } });
  fireEvent.click(screen.getByRole("button", { name: "Apply Filter" }));
  await waitFor(() => expect(requested().get("min")).toBe("10"));
  await screen.findByText("Blue Shoe");
  fireEvent.change(screen.getByRole("combobox"), { target: { value: "priceDesc" } });
  await waitFor(() => expect(requested().get("sort")).toBe("priceDesc"));
  expect(requested().get("q")).toBe("blue"); expect(requested().get("max")).toBe("40");
  await screen.findByText("Blue Shoe");
});

test("navigates pages with the same query and disables boundaries", async () => {
  global.fetch.mockResolvedValueOnce(response(page())).mockResolvedValueOnce(response(page({ page: 1 })))
    .mockResolvedValueOnce(response(page({ page: 0 })));
  renderShop("/shop?q=blue&br=Acme"); await screen.findByText("Blue Shoe");
  expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
  fireEvent.click(screen.getByRole("button", { name: "Next" }));
  await screen.findByText(/Page 2 of 3/);
  expect(requested().get("page")).toBe("1"); expect(requested().get("q")).toBe("blue");
  fireEvent.click(screen.getByRole("button", { name: "Previous" }));
  await screen.findByText(/Page 1 of 3/);
});

test("shows empty state and resets all filters", async () => {
  global.fetch.mockResolvedValue(response(page({ content: [], totalElements: 0, totalPages: 0 })));
  renderShop(); await screen.findByText("No products found");
  expect(screen.getByRole("link", { name: "Show All Products" })).toHaveAttribute("href", "/shop");
  expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
});

test("reports server failure and retries", async () => {
  global.fetch.mockResolvedValueOnce(response({ message: "Service unavailable" }, false));
  renderShop(); expect(await screen.findByRole("alert")).toHaveTextContent("Service unavailable");
  fireEvent.click(screen.getByRole("button", { name: "Retry" })); await screen.findByText("Blue Shoe");
});

test("reports network failure and rejects reversed prices", async () => {
  global.fetch.mockRejectedValueOnce(new Error("Network unavailable"));
  renderShop(); await screen.findByText("Network unavailable");
  fireEvent.click(screen.getByRole("button", { name: "Retry" })); await screen.findByText("Blue Shoe");
  fireEvent.change(screen.getByPlaceholderText("Min Amount"), { target: { value: "40" } });
  fireEvent.change(screen.getByPlaceholderText("Max Amount"), { target: { value: "10" } });
  fireEvent.click(screen.getByRole("button", { name: "Apply Filter" }));
  expect(screen.getByRole("alert")).toHaveTextContent("Minimum price must not exceed maximum price");
});

test("ignores stale responses after navigation and clears one facet without clearing others", async () => {
  let finishOld;
  global.fetch.mockImplementationOnce(() => new Promise(resolve => { finishOld = resolve; }));
  renderShop("/shop?mc=Clothes&br=Acme&q=blue&page=1");
  expect(screen.getByRole("status")).toHaveTextContent("Loading products");
  fireEvent.click(screen.getByRole("link", { name: "Shoes" }));
  await screen.findByText("Blue Shoe");
  expect(global.fetch.mock.calls[0][1].signal.aborted).toBe(true);
  finishOld(response(page({ content: [{ ...product, name: "Old result" }] })));
  await waitFor(() => expect(screen.queryByText("Old result")).not.toBeInTheDocument());
  const allLinks = screen.getAllByRole("link", { name: "All" });
  fireEvent.click(allLinks[0]);
  await waitFor(() => expect(requested().has("mc")).toBe(false));
  expect(requested().get("br")).toBe("Acme"); expect(requested().get("q")).toBe("blue");
  await screen.findByText("Blue Shoe");
});

test("blank search clears the keyword while retaining the price and category", async () => {
  renderShop("/shop?q=blue&mc=Clothes&min=10&max=40"); await screen.findByText("Blue Shoe");
  fireEvent.change(screen.getByRole("searchbox"), { target: { value: "   " } });
  fireEvent.click(screen.getByRole("button", { name: "Search products" }));
  await waitFor(() => expect(requested().has("q")).toBe(false));
  expect(requested().get("min")).toBe("10"); expect(requested().get("mc")).toBe("Clothes");
  await screen.findByText("Blue Shoe");
});
