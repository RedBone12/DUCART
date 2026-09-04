import { configureStore } from "@reduxjs/toolkit";
import { fireEvent, render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import ShopPage from "./ShopPage";

const productState = [
  {
    id: 1,
    name: "Premium Dog Food",
    maincategory: "Pets",
    subcategory: "Food",
    brand: "Acme",
    color: "Brown",
    description: "Healthy daily food",
    basePrice: 30,
    finalPrice: 24,
    discount: 20,
    active: true,
    pics: ["/dog-food.jpg"],
  },
  {
    id: 2,
    name: "Wireless Headphones",
    maincategory: "Electronics",
    subcategory: "Audio",
    brand: "SoundCo",
    color: "Black",
    description: "Noise cancelling headphones",
    basePrice: 120,
    finalPrice: 90,
    discount: 25,
    active: true,
    pics: ["/headphones.jpg"],
  },
  {
    id: 3,
    name: "Hidden Product",
    maincategory: "Pets",
    subcategory: "Food",
    brand: "Acme",
    color: "Blue",
    description: "Inactive",
    basePrice: 10,
    finalPrice: 8,
    discount: 20,
    active: false,
    pics: [],
  },
];

function renderShop(route = "/shop?mc=All&sc=All&br=All") {
  const initialState = {
    ProductStateData: productState,
    MaincategoryStateData: [
      { id: 1, name: "Pets", active: true },
      { id: 2, name: "Hidden Category", active: false },
    ],
    SubcategoryStateData: [{ id: 1, name: "Food", active: true }],
    BrandStateData: [{ id: 1, name: "Acme", active: true }],
  };
  const store = configureStore({
    reducer: (state = initialState) => state, preloadedState: initialState,
  });

  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path="/shop" element={<ShopPage />} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );

  return store;
}

describe("ShopPage", () => {
  test("shows active products and active filter choices only", async () => {
    renderShop();

    expect(await screen.findByText("Premium Dog Food")).toBeInTheDocument();
    expect(screen.getByText("Wireless Headphones")).toBeInTheDocument();
    expect(screen.queryByText("Hidden Product")).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Pets" })).toBeInTheDocument();
    expect(screen.queryByText("Hidden Category")).not.toBeInTheDocument();
  });

  test("filters products from category query parameters", async () => {
    renderShop("/shop?mc=Pets&sc=All&br=All");

    expect(await screen.findByText("Premium Dog Food")).toBeInTheDocument();
    expect(screen.queryByText("Wireless Headphones")).not.toBeInTheDocument();
  });

  test("searches across product fields", async () => {
    renderShop();
    await screen.findByText("Premium Dog Food");

    fireEvent.change(
      screen.getByPlaceholderText(/Search Products by Name/i),
      { target: { value: "noise cancelling" } },
    );
    fireEvent.click(screen.getByRole("button", { name: "Search products" }));

    expect(await screen.findByText("Wireless Headphones")).toBeInTheDocument();
    expect(screen.queryByText("Premium Dog Food")).not.toBeInTheDocument();
  });

  test("sorts visible products by price", async () => {
    renderShop();
    await screen.findByText("Premium Dog Food");

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "2" },
    });

    const highToLow = screen.getAllByRole("heading", { level: 6 });
    expect(highToLow.map((heading) => heading.textContent)).toEqual([
      "Wireless Headphones",
      "Premium Dog Food",
    ]);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "3" },
    });

    const lowToHigh = screen.getAllByRole("heading", { level: 6 });
    expect(lowToHigh.map((heading) => heading.textContent)).toEqual([
      "Premium Dog Food",
      "Wireless Headphones",
    ]);
  });

  test("shows an empty state when no product matches", async () => {
    renderShop("/shop?mc=Unknown&sc=All&br=All");

    expect(await screen.findByText("No products found")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Show All Products" }),
    ).toHaveAttribute("href", "/shop?mc=All&sc=All&br=All");
  });

  test("applies subcategory, brand, price, newest, and blank-search filters", async () => {
    renderShop("/shop?mc=All&sc=Food&br=Acme");
    await screen.findByText("Premium Dog Food");

    fireEvent.change(screen.getByPlaceholderText("Min Amount"), {
      target: { value: "20" },
    });
    fireEvent.change(screen.getByPlaceholderText("Max Amount"), {
      target: { value: "30" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Apply Filter" }));
    expect(screen.getByText("Premium Dog Food")).toBeInTheDocument();

    fireEvent.change(screen.getByRole("combobox"), { target: { value: "1" } });
    fireEvent.change(screen.getByPlaceholderText(/Search Products by Name/i), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Search products" }));
    expect(screen.queryByText("Wireless Headphones")).not.toBeInTheDocument();
  });

  test("combines a text search with a price range", async () => {
    renderShop();
    await screen.findByText("Wireless Headphones");
    fireEvent.change(screen.getByPlaceholderText(/Search Products by Name/i), {
      target: { value: "headphones" },
    });
    fireEvent.change(screen.getByPlaceholderText("Min Amount"), {
      target: { value: "80" },
    });
    fireEvent.change(screen.getByPlaceholderText("Max Amount"), {
      target: { value: "100" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Apply Filter" }));
    expect(screen.getByText("Wireless Headphones")).toBeInTheDocument();
    expect(screen.queryByText("Premium Dog Food")).not.toBeInTheDocument();
  });
});
