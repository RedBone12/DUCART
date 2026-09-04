import { configureStore } from "@reduxjs/toolkit";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import Cart from "./Cart";
import { CREATE_CHECKOUT, DELETE_CART, UPDATE_CART } from "../Redux/Constants";
import { formatCurrency } from "../config/siteConfig";

const cartItem = {
  id: 10,
  product: "Premium Dog Food",
  name: "Premium Dog Food",
  brand: "Acme",
  color: "Brown",
  size: "2 kg",
  price: 24,
  stockQuantity: 2,
  pic: "/dog-food.jpg",
  qty: 1,
  total: 24,
};

const product = {
  id: 1,
  name: "Premium Dog Food",
  stockQuantity: 2,
};

function renderCart({
  cart = [cartItem],
  products = [product],
  title = "Cart",
} = {}) {
  const initialState = {
    CartStateData: cart.map((item) => ({ ...item })),
    ProductStateData: products.map((item) => ({ ...item })),
  };
  const store = configureStore({
    reducer: (state = initialState) => state,
    preloadedState: initialState,
  });
  const dispatchSpy = jest.spyOn(store, "dispatch");

  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={["/cart"]}>
        <Routes>
          <Route path="/cart" element={<Cart title={title} />} />
          <Route path="/checkout" element={<div>Checkout page</div>} />
          <Route path="/confirmation" element={<div>Confirmation page</div>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );

  return { dispatchSpy };
}

describe("Cart", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("renders an empty-cart message and shop link", () => {
    renderCart({ cart: [] });

    expect(screen.getByRole("heading", { name: "No Items in Cart" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Shop Now" })).toHaveAttribute(
      "href",
      "/shop",
    );
  });

  test("renders cart contents and calculates paid shipping below the threshold", async () => {
    renderCart();

    expect(
      await screen.findByRole("img", { name: "Premium Dog Food" }),
    ).toHaveAttribute(
      "src",
      `${process.env.REACT_APP_SERVER}/dog-food.jpg`,
    );
    expect(
      screen.getByRole("row", {
        name: `Subtotal ${formatCurrency(24)}`,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("row", {
        name: `Shipping ${formatCurrency(4.99)}`,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("row", {
        name: `Total ${formatCurrency(28.99)}`,
      }),
    ).toBeInTheDocument();
  });

  test("provides free shipping when the subtotal reaches the threshold", async () => {
    renderCart({
      cart: [{ ...cartItem, price: 60, qty: 2, total: 120 }],
    });

    expect(
      await screen.findByRole("row", {
        name: `Shipping ${formatCurrency(0)}`,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("row", {
        name: `Total ${formatCurrency(120)}`,
      }),
    ).toBeInTheDocument();
  });

  test("increments quantity, updates the total, and stops at available stock", async () => {
    const { dispatchSpy } = renderCart();
    const incrementButton = await screen.findByRole("button", {
      name: "Increase Premium Dog Food quantity",
    });
    dispatchSpy.mockClear();

    fireEvent.click(incrementButton);

    expect(dispatchSpy).toHaveBeenCalledWith({
      type: UPDATE_CART,
      payload: expect.objectContaining({ id: 10, qty: 2, total: 48 }),
    });
    const productRow = screen.getByRole("row", { name: /Premium Dog Food/ });
    expect(within(productRow).getByRole("heading", { level: 5, name: "2" }),).toBeInTheDocument();
    expect(screen.getByRole("row", {name: `Subtotal ${formatCurrency(48)}`,}),).toBeInTheDocument();

    fireEvent.click(incrementButton);
    expect(dispatchSpy).toHaveBeenCalledTimes(1);
  });

  test("does not decrease quantity below one", async () => {
    const { dispatchSpy } = renderCart();
    const decrementButton = await screen.findByRole("button", {
      name: "Decrease Premium Dog Food quantity",
    });
    dispatchSpy.mockClear();

    fireEvent.click(decrementButton);

    expect(dispatchSpy).not.toHaveBeenCalled();
  });

  test("deletes an item after user confirmation", async () => {
    jest.spyOn(window, "confirm").mockReturnValue(true);
    const { dispatchSpy } = renderCart();
    const removeButton = await screen.findByRole("button", {
      name: "Remove Premium Dog Food from cart",
    });
    dispatchSpy.mockClear();

    fireEvent.click(removeButton);

    expect(window.confirm).toHaveBeenCalledWith(
      "Are You Sure to Delete that Item : ",
    );
    expect(dispatchSpy).toHaveBeenCalledWith({
      type: DELETE_CART,
      payload: { id: 10 },
    });
  });

  test("links a populated cart to checkout", async () => {
    renderCart();

    expect(await screen.findByRole("link", { name: "Checkout" })).toHaveAttribute(
      "href",
      "/checkout",
    );
  });

  test("places a valid demo-card order and navigates to confirmation", async () => {
    localStorage.setItem("username", "buyer");
    const { dispatchSpy } = renderCart({ title: "Checkout" });
    await screen.findByRole("button", { name: "Place Order" });
    dispatchSpy.mockClear();

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "Card Demo" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Place Order" }));

    expect(await screen.findByText("Confirmation page")).toBeInTheDocument();
    expect(dispatchSpy).toHaveBeenCalledWith({
      type: CREATE_CHECKOUT,
      payload: expect.objectContaining({
        user: "buyer",
        paymentMode: "Card Demo",
        paymentStatus: "Paid (Demo)",
        subtotal: 24,
        shipping: 4.99,
        total: 28.99,
        date: expect.any(String),
        products: [expect.objectContaining({ id: 10 })],
      }),
    });
  });

  test("blocks checkout when requested quantity exceeds current stock", async () => {
    const alertSpy = jest.spyOn(window, "alert").mockImplementation(() => {});
    const { dispatchSpy } = renderCart({
      cart: [{ ...cartItem, qty: 2, total: 48 }],
      products: [{ ...product, stockQuantity: 1 }],
      title: "Checkout",
    });
    await screen.findByRole("button", { name: "Place Order" });
    dispatchSpy.mockClear();

    fireEvent.click(screen.getByRole("button", { name: "Place Order" }));

    expect(alertSpy).toHaveBeenCalledWith(
      "Premium Dog Food has only 1 items left in stock",
    );
    expect(dispatchSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: CREATE_CHECKOUT }),
    );
    expect(screen.queryByText("Confirmation page")).not.toBeInTheDocument();
  });
});
