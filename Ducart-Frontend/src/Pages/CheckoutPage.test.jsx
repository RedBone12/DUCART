import { configureStore } from "@reduxjs/toolkit";
import { fireEvent, render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import CheckoutPage from "./CheckoutPage";
import { CREATE_CHECKOUT } from "../Redux/Constants";

const user = {
  id: 42,
  name: "Demo Buyer",
  username: "buyer",
  phone: "+3531234567",
  email: "buyer@example.com",
  address: "1 Shop Street",
  pin: "H91TEST",
  city: "Galway",
  state: "County Galway",
};

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

function setupCheckoutPage({ cart = [cartItem], products = [product] } = {}) {
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
      <MemoryRouter initialEntries={["/checkout"]}>
        <Routes>
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/confirmation" element={<div>Confirmation page</div>} />
          <Route path="/login" element={<div>Login page</div>} />
          <Route path="/update-profile" element={<div>Update profile page</div>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );

  return { dispatchSpy };
}

describe("CheckoutPage", () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem("token", "buyer-token");
    localStorage.setItem("userid", "42");
    localStorage.setItem("username", "buyer");
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue(user),
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("loads and displays buyer, cart, and checkout information", async () => {
    setupCheckoutPage();

    expect(screen.getByRole("heading", { name: "Place Order" })).toBeInTheDocument();
    expect(await screen.findByRole("cell", { name: "Demo Buyer" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "buyer@example.com" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "1 Shop Street" })).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Premium Dog Food" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Place Order" })).toBeInTheDocument();
    expect(screen.queryByRole("columnheader", { name: "Stock Quantity" }),).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Remove Premium Dog Food from cart" }),).not.toBeInTheDocument();
  });

  test("requests the authenticated buyer profile", async () => {
    setupCheckoutPage();
    await screen.findByRole("cell", { name: "Demo Buyer" });

    expect(global.fetch).toHaveBeenCalledWith(
      `${process.env.REACT_APP_SERVER}/user/42`,
      {
        method: "GET",
        headers: {
          "content-type": "application/json",
          Authorization: "Bearer buyer-token",
        },
      },
    );
  });

  test("submits a cash-on-delivery order and opens confirmation", async () => {
    const { dispatchSpy } = setupCheckoutPage();
    await screen.findByRole("cell", { name: "Demo Buyer" });
    dispatchSpy.mockClear();

    fireEvent.click(screen.getByRole("button", { name: "Place Order" }));

    expect(await screen.findByText("Confirmation page")).toBeInTheDocument();
    expect(dispatchSpy).toHaveBeenCalledWith({
      type: CREATE_CHECKOUT,
      payload: expect.objectContaining({
        user: "buyer",
        orderStatus: "Order is Placed",
        paymentMode: "COD",
        paymentStatus: "Pending",
        subtotal: 24,
        shipping: 4.99,
        total: 28.99,
        date: expect.any(String),
        products: [expect.objectContaining({ id: 10, qty: 1 })],
      }),
    });
  });

  test("shows an empty-cart state without a place-order action", async () => {
    setupCheckoutPage({ cart: [] });
    await screen.findByRole("cell", { name: "Demo Buyer" });

    expect(screen.getByRole("heading", { name: "No Items in Cart" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Place Order" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Shop Now" })).toHaveAttribute(
      "href",
      "/shop",
    );
  });

  test("redirects to login when the profile request is unauthorized", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      json: jest.fn().mockResolvedValue({ message: "Unauthorized" }),
    });

    setupCheckoutPage();

    expect(await screen.findByText("Login page")).toBeInTheDocument();
  });
});
