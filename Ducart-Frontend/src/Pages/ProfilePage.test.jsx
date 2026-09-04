import { configureStore } from "@reduxjs/toolkit";
import { fireEvent, render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import ProfilePage from "./ProfilePage";
import { DELETE_WISHLIST, GET_CHECKOUT, GET_WISHLIST } from "../Redux/Constants";
import { formatCurrency } from "../config/siteConfig";

const profile = {
  name: "Demo Buyer",
  username: "buyer",
  phone: "+3531234567",
  email: "buyer@example.com",
  address: "1 Shop Street",
  pin: "H91TEST",
  city: "Galway",
  state: "County Galway",
};

const wishlistItem = {
  id: 5,
  product: "1",
  name: "Premium Dog Food",
  brand: "Acme",
  color: "Brown",
  size: "2 kg",
  price: 24,
  stockQuantity: 3,
  pic: "/dog-food.jpg",
};

const order = {
  id: 100,
  orderStatus: "Order is Shipped",
  paymentMode: "COD",
  paymentStatus: "Pending",
  subtotal: 48,
  shipping: 4.99,
  total: 52.99,
  date: "2026-08-19T10:00:00.000Z",
  products: [{ ...wishlistItem, id: 10, qty: 2, total: 48 }],
};

function setupProfilePage({ wishlist = [wishlistItem], orders = [order] } = {}) {
  const initialState = {
    WishlistStateData: wishlist.map((item) => ({ ...item })),
    CheckoutStateData: orders.map((item) => ({ ...item })),
  };
  const store = configureStore({
    reducer: (state = initialState) => state,
    preloadedState: initialState,
  });
  const dispatchSpy = jest.spyOn(store, "dispatch");

  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={["/profile"]}>
        <Routes>
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );

  return { dispatchSpy };
}

describe("ProfilePage", () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem("userid", "42");
    localStorage.setItem("token", "buyer-token");
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue(profile),
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("requests profile data, wishlist, and the current buyer order history", async () => {
    const { dispatchSpy } = setupProfilePage();

    expect(await screen.findByRole("cell", { name: "Demo Buyer" })).toBeInTheDocument();
    expect(dispatchSpy).toHaveBeenCalledWith({ type: GET_WISHLIST });
    expect(dispatchSpy).toHaveBeenCalledWith({ type: GET_CHECKOUT });
  });

  test("renders wishlist and complete order history information", async () => {
    setupProfilePage();
    await screen.findByRole("cell", { name: "Demo Buyer" });

    expect(screen.getByRole("link", { name: "View Premium Dog Food" })).toHaveAttribute(
      "href",
      "/product/1",
    );
    expect(screen.getByRole("cell", { name: "Order is Shipped" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: formatCurrency(52.99) })).toBeInTheDocument();
    expect(screen.getAllByRole("img", { name: "Premium Dog Food" })).toHaveLength(2);
  });

  test("shows empty states when wishlist and order history are empty", async () => {
    setupProfilePage({ wishlist: [], orders: [] });
    await screen.findByRole("cell", { name: "Demo Buyer" });

    expect(screen.getByRole("heading", { name: "No Items in Wishlist" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "No Order History Found" })).toBeInTheDocument();
  });

  test("removes a wishlist item after confirmation", async () => {
    jest.spyOn(window, "confirm").mockReturnValue(true);
    const { dispatchSpy } = setupProfilePage();
    const removeButton = await screen.findByRole("button", {
      name: "Remove Premium Dog Food from wishlist",
    });
    dispatchSpy.mockClear();

    fireEvent.click(removeButton);

    expect(dispatchSpy).toHaveBeenCalledWith({
      type: DELETE_WISHLIST,
      payload: { id: 5 },
    });
  });
});
