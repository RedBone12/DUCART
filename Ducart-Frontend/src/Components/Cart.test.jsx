import { configureStore } from "@reduxjs/toolkit";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import Cart from "./Cart";
import { DELETE_CART, UPDATE_CART } from "../Redux/Constants";
import { submitOrder } from "../config/checkout";
import { formatCurrency } from "../config/siteConfig";

jest.mock("../config/checkout", () => ({
  ...jest.requireActual("../config/checkout"),
  submitOrder: jest.fn(),
}));


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
    sessionStorage.clear();
    Object.defineProperty(window, "crypto", {
      configurable: true, value: { randomUUID: jest.fn(() => "test-key-12345678") },
    });
    submitOrder.mockReset();
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

  test("sends only IDs and quantities and waits for server confirmation", async () => {
    localStorage.setItem("username", "buyer");
    let resolve;
    submitOrder.mockImplementation(() => new Promise((done) => { resolve = done; }));
    const { dispatchSpy } = renderCart({ title: "Checkout" });
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "Card Demo" } });
    fireEvent.click(await screen.findByRole("button", { name: "Place Order" }));
    expect(submitOrder).toHaveBeenCalledWith({
      paymentMode: "Card Demo", idempotencyKey: "test-key-12345678",
      products: [{ productId: 1, qty: 1 }],
    });
    expect(screen.getByRole("button", { name: "Placing Order..." })).toBeDisabled();
    expect(screen.queryByText("Confirmation page")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Placing Order..." }));
    expect(submitOrder).toHaveBeenCalledTimes(1);
    resolve({ id: 7, total: 28.99 });
    expect(await screen.findByText("Confirmation page")).toBeInTheDocument();
    expect(dispatchSpy).toHaveBeenCalledWith({ type: DELETE_CART, payload: { id: 10 } });
  });

  test("server rejection keeps the cart and retries with the same key", async () => {
    submitOrder.mockRejectedValueOnce(new Error("Insufficient stock"))
      .mockResolvedValueOnce({ id: 8, total: 28.99 });
    const { dispatchSpy } = renderCart({ title: "Checkout" });
    dispatchSpy.mockClear();
    fireEvent.click(await screen.findByRole("button", { name: "Place Order" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Insufficient stock");
    expect(screen.queryByText("Confirmation page")).not.toBeInTheDocument();
    expect(dispatchSpy).not.toHaveBeenCalledWith(expect.objectContaining({ type: DELETE_CART }));
    fireEvent.click(screen.getByRole("button", { name: "Place Order" }));
    expect(await screen.findByText("Confirmation page")).toBeInTheDocument();
    expect(submitOrder.mock.calls[0][0].idempotencyKey).toEqual(submitOrder.mock.calls[1][0].idempotencyKey);
  });

  test("a missing catalogue product does not send an order", async () => {
    renderCart({ title: "Checkout", products: [] });
    fireEvent.click(await screen.findByRole("button", { name: "Place Order" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("was not found");
    expect(submitOrder).not.toHaveBeenCalled();
  });

  test("uses the saved product ID even when the catalogue name has changed", async () => {
    submitOrder.mockResolvedValue({ id: 10 });
    renderCart({ title: "Checkout", cart: [{ ...cartItem, productId: 42 }], products: [] });
    fireEvent.click(await screen.findByRole("button", { name: "Place Order" }));
    expect(await screen.findByText("Confirmation page")).toBeInTheDocument();
    expect(submitOrder).toHaveBeenCalledWith(expect.objectContaining({ products: [{ productId: 42, qty: 1 }] }));
  });
});
