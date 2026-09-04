import { configureStore } from "@reduxjs/toolkit";
import { fireEvent, render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import SingleProductPage from "./SingleProductPage";
import { CREATE_CART, CREATE_WISHLIST, UPDATE_CART } from "../Redux/Constants";

const selectedProduct = {
  id: 1,
  name: "Premium Dog Food",
  maincategory: "Pets",
  subcategory: "Food",
  brand: "Acme",
  color: "Brown",
  size: "2 kg",
  description: "<strong>Healthy daily food</strong>",
  basePrice: 30,
  finalPrice: 24,
  discount: 20,
  stock: true,
  stockQuantity: 2,
  active: true,
  pics: ["/dog-food-1.jpg", "/dog-food-2.jpg"],
};

const relatedProduct = {
  ...selectedProduct,
  id: 2,
  name: "Dog Treats",
  finalPrice: 8,
  pics: [],
};

function renderProduct({ cart = [], wishlist = [] } = {}) {
  const initialState = {
    ProductStateData: [selectedProduct, relatedProduct],
    CartStateData: cart,
    WishlistStateData: wishlist,
  };
  const store = configureStore({
    reducer: (state = initialState) => state,
    preloadedState: initialState,
  });
  const dispatchSpy = jest.spyOn(store, "dispatch");

  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={["/product/1"]}>
        <Routes>
          <Route path="/product/:id" element={<SingleProductPage />} />
          <Route path="/login" element={<div>Login page</div>} />
          <Route path="/cart" element={<div>Cart page</div>} />
          <Route path="/profile" element={<div>Profile page</div>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );

  return { dispatchSpy };
}

describe("SingleProductPage", () => {
  beforeEach(() => {
    localStorage.clear();
    jest.spyOn(console, "log").mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("renders product details, images, description, and related products", async () => {
    renderProduct();

    expect(
      await screen.findByRole("heading", { name: "Premium Dog Food" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Pets/Food/Acme")).toBeInTheDocument();
    expect(screen.getByText("Brown/2 kg")).toBeInTheDocument();
    expect(screen.getByText("Yes(2 Left In Stock)")).toBeInTheDocument();
    expect(screen.getByText("Healthy daily food")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Premium Dog Food" })).toHaveAttribute(
      "src",
      `${process.env.REACT_APP_SERVER}/dog-food-1.jpg`,
    );
    expect(screen.getByText("Dog Treats")).toBeInTheDocument();
  });

  test("redirects a guest to login instead of adding to cart", async () => {
    const { dispatchSpy } = renderProduct();
    await screen.findByRole("heading", { name: "Premium Dog Food" });
    dispatchSpy.mockClear();

    fireEvent.click(screen.getByRole("button", { name: "Add to Cart" }));

    expect(await screen.findByText("Login page")).toBeInTheDocument();
    expect(dispatchSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: CREATE_CART }),
    );
  });

  test("redirects a guest to login instead of adding to wishlist", async () => {
    const { dispatchSpy } = renderProduct();
    await screen.findByRole("heading", { name: "Premium Dog Food" });
    dispatchSpy.mockClear();
    fireEvent.click(screen.getByRole("button", { name: "Add to Wishlist" }));
    expect(await screen.findByText("Login page")).toBeInTheDocument();
    expect(dispatchSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: CREATE_WISHLIST }),
    );
  });

  test("adds the selected quantity to cart and navigates to the cart page", async () => {
    localStorage.setItem("token", "buyer-token");
    const { dispatchSpy } = renderProduct();
    await screen.findByRole("heading", { name: "Premium Dog Food" });
    dispatchSpy.mockClear();

    fireEvent.click(screen.getByRole("button", { name: "Increase quantity" }));
    fireEvent.click(screen.getByRole("button", { name: "Add to Cart" }));

    expect(await screen.findByText("Cart page")).toBeInTheDocument();
    expect(dispatchSpy).toHaveBeenCalledWith({
      type: CREATE_CART,
      payload: expect.objectContaining({
        product: "Premium Dog Food",
        qty: 2,
        total: 48,
      }),
    });
  });

  test("adds a product to the wishlist and navigates to the profile page", async () => {
    localStorage.setItem("token", "buyer-token");
    const { dispatchSpy } = renderProduct();
    await screen.findByRole("heading", { name: "Premium Dog Food" });
    dispatchSpy.mockClear();

    fireEvent.click(screen.getByRole("button", { name: "Add to Wishlist" }));

    expect(await screen.findByText("Profile page")).toBeInTheDocument();
    expect(dispatchSpy).toHaveBeenCalledWith({
      type: CREATE_WISHLIST,
      payload: expect.objectContaining({
        product: "Premium Dog Food",
        price: 24,
      }),
    });
  });

  test("merges quantity into an existing cart item", async () => {
    localStorage.setItem("token", "buyer-token");
    const existing = {
      product: "Premium Dog Food",
      qty: 1,
      price: 24,
      total: 24,
    };
    const { dispatchSpy } = renderProduct({ cart: [existing] });
    await screen.findByRole("heading", { name: "Premium Dog Food" });
    dispatchSpy.mockClear();
    fireEvent.click(screen.getByRole("button", { name: "Add to Cart" }));
    expect(await screen.findByText("Cart page")).toBeInTheDocument();
    expect(dispatchSpy).toHaveBeenCalledWith({
      type: UPDATE_CART,
      payload: expect.objectContaining({ qty: 2, total: 48 }),
    });
  });

  test("blocks a cart quantity above stock", async () => {
    localStorage.setItem("token", "buyer-token");
    const alertSpy = jest.spyOn(window, "alert").mockImplementation(() => {});
    const existing = { product: "Premium Dog Food", qty: 2, price: 24, total: 48 };
    const { dispatchSpy } = renderProduct({ cart: [existing] });
    await screen.findByRole("heading", { name: "Premium Dog Food" });
    dispatchSpy.mockClear();
    fireEvent.click(screen.getByRole("button", { name: "Add to Cart" }));
    expect(alertSpy).toHaveBeenCalledWith("Only 2 items are available in stock");
    expect(dispatchSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: UPDATE_CART }),
    );
  });

  test("does not add a duplicate wishlist entry and respects quantity limits", async () => {
    localStorage.setItem("token", "buyer-token");
    const alertSpy = jest.spyOn(window, "alert").mockImplementation(() => {});
    const { dispatchSpy } = renderProduct({
      wishlist: [{ product: "Premium Dog Food" }],
    });
    await screen.findByRole("heading", { name: "Premium Dog Food" });
    dispatchSpy.mockClear();
    fireEvent.click(screen.getByRole("button", { name: "Decrease quantity" }));
    expect(screen.getByText("1")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Increase quantity" }));
    fireEvent.click(screen.getByRole("button", { name: "Increase quantity" }));
    expect(screen.getByText("2")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Add to Wishlist" }));
    expect(alertSpy).toHaveBeenCalledWith("This product is already in your wishlist");
    expect(dispatchSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: CREATE_WISHLIST }),
    );
  });
});
