import { configureStore } from "@reduxjs/toolkit";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import AdminCheckout from "./Checkout/AdminCheckout";
import AdminCheckoutShow from "./Checkout/AdminCheckoutShow";
import AdminContactUs from "./ContactUs/AdminContactUs";
import AdminContactUsShow from "./ContactUs/AdminContactUsShow";
import AdminNewsletter from "./Newsletter/AdminNewsletter";
import AdminUser from "./User/AdminUser";
import AdminHome from "./Home/AdminHome";
import {
  DELETE_CONTACT_US,
  DELETE_NEWSLETTER,
  UPDATE_CHECKOUT,
  UPDATE_CONTACT_US,
  UPDATE_NEWSLETTER,
} from "../Redux/Constants";

jest.mock("jquery", () => () => ({ DataTable: jest.fn() }));
jest.mock("datatables.net", () => ({}));
jest.mock("../Components/OrderProducts", () => ({ data }) => (
  <div>Products: {data?.length || 0}</div>
));
jest.mock("../Components/Profile", () => ({ title }) => <div>{title}</div>);

const checkout = {
  id: 11,
  user: "u1",
  orderStatus: "Order is Placed",
  paymentMode: "COD",
  paymentStatus: "Pending",
  subtotal: 100,
  shipping: 10,
  total: 110,
  date: "2026-01-02T10:00:00.000Z",
  products: [{ id: 1 }],
};
const contact = {
  id: 21,
  name: "Jamie",
  email: "jamie@example.com",
  phone: "1234567890",
  subject: "Question about an existing order",
  message: "Please provide an update about my existing order and delivery status.",
  date: "2026-01-03T10:00:00.000Z",
  active: true,
};

function renderPage(element, route, pattern = route, overrides = {}) {
  const initialState = {
    CheckoutStateData: [checkout],
    ContactUsStateData: [contact],
    NewsletterStateData: [{ id: 31, email: "news@example.com", active: true }],
    ...overrides,
  };
  const store = configureStore({
    reducer: (state = initialState) => state,
    preloadedState: initialState,
    middleware: () => [],
  });
  const dispatchSpy = jest.spyOn(store, "dispatch");
  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path={pattern} element={element} />
          <Route path="/admin/contactus" element={<div>Contact destination</div>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );
  return dispatchSpy;
}

function actionOf(spy, type) {
  return spy.mock.calls.map(([action]) => action).find((action) => action.type === type);
}

describe("admin orders, messages, subscribers, and users", () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("renders the admin dashboard profile", () => {
    renderPage(<AdminHome />, "/admin");
    expect(screen.getByText("Admin Profile")).toBeInTheDocument();
  });

  test("lists checkout totals and links to the order details", () => {
    renderPage(<AdminCheckout />, "/admin/checkout");
    expect(screen.getByText("COD")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "" })).toHaveAttribute(
      "href",
      "/admin/checkout/show/11",
    );
  });

  test("renders empty checkout and contact tables", () => {
    renderPage(<AdminCheckout />, "/admin/checkout", undefined, {
      CheckoutStateData: [],
    });
    expect(screen.queryAllByRole("row")).toHaveLength(1);
    renderPage(<AdminContactUs />, "/admin/contactus", undefined, {
      ContactUsStateData: [],
    });
    expect(screen.queryAllByRole("row")).toHaveLength(2);
  });

  test("loads a checkout customer and updates both statuses", async () => {
    global.fetch = jest.fn().mockResolvedValue({
      json: async () => ({
        name: "Buyer One",
        phone: "1234567890",
        email: "buyer@example.com",
        address: "1 Main Road",
        pin: "A1",
        city: "Dublin",
        state: "Leinster",
      }),
    });
    jest.spyOn(window, "confirm").mockReturnValue(true);
    const spy = renderPage(
      <AdminCheckoutShow />,
      "/admin/checkout/show/11",
      "/admin/checkout/show/:id",
    );
    expect(await screen.findByText(/Buyer One/)).toBeInTheDocument();
    spy.mockClear();
    const selects = screen.getAllByRole("combobox");
    fireEvent.change(selects[0], { target: { value: "Delivered" } });
    fireEvent.change(selects[1], { target: { value: "Done" } });
    fireEvent.click(screen.getByRole("button", { name: "Update Status" }));
    expect(actionOf(spy, UPDATE_CHECKOUT).payload).toEqual(
      expect.objectContaining({ orderStatus: "Delivered", paymentStatus: "Done" }),
    );
  });

  test("hides checkout controls when payment and delivery are complete", async () => {
    global.fetch = jest.fn().mockResolvedValue({
      json: async () => ({ name: "Completed Buyer" }),
    });
    renderPage(
      <AdminCheckoutShow />,
      "/admin/checkout/show/11",
      "/admin/checkout/show/:id",
      {
        CheckoutStateData: [{
          ...checkout,
          orderStatus: "Delivered",
          paymentStatus: "Done",
          rppid: "PAY-11",
        }],
      },
    );
    expect(await screen.findByText(/Completed Buyer/)).toBeInTheDocument();
    expect(screen.getByText("PAY-11")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Update Status" })).not.toBeInTheDocument();
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument();
  });

  test("lists contact messages and deletes an inactive one", () => {
    jest.spyOn(window, "confirm").mockReturnValue(true);
    const spy = renderPage(<AdminContactUs />, "/admin/contactus", undefined, {
      ContactUsStateData: [{ ...contact, active: false }],
    });
    expect(screen.getByText("Question about an existing order")).toBeInTheDocument();
    spy.mockClear();
    fireEvent.click(screen.getByRole("button"));
    expect(actionOf(spy, DELETE_CONTACT_US).payload).toEqual({ id: 21 });
  });

  test("shows an active message and marks it inactive", async () => {
    jest.spyOn(window, "confirm").mockReturnValue(true);
    const spy = renderPage(
      <AdminContactUsShow />,
      "/admin/contactus/show/21",
      "/admin/contactus/show/:id",
    );
    expect(await screen.findByText(contact.message)).toBeInTheDocument();
    spy.mockClear();
    fireEvent.click(screen.getByRole("button", { name: "Update Status" }));
    expect(actionOf(spy, UPDATE_CONTACT_US).payload.active).toBe(false);
  });

  test("deletes an inactive message from its detail page", async () => {
    jest.spyOn(window, "confirm").mockReturnValue(true);
    const spy = renderPage(
      <AdminContactUsShow />,
      "/admin/contactus/show/21",
      "/admin/contactus/show/:id",
      { ContactUsStateData: [{ ...contact, active: false }] },
    );
    expect(await screen.findByRole("button", { name: "Delete" })).toBeInTheDocument();
    spy.mockClear();
    fireEvent.click(screen.getByRole("button", { name: "Delete" }));
    expect(await screen.findByText("Contact destination")).toBeInTheDocument();
    expect(actionOf(spy, DELETE_CONTACT_US).payload).toEqual({ id: "21" });
  });

  test("toggles and deletes a newsletter subscriber", () => {
    jest.spyOn(window, "confirm").mockReturnValue(true);
    const spy = renderPage(<AdminNewsletter />, "/admin/newsletter");
    spy.mockClear();
    fireEvent.click(screen.getByTitle("Click to Change Status"));
    expect(actionOf(spy, UPDATE_NEWSLETTER).payload.active).toBe(false);
    fireEvent.click(screen.getByRole("button"));
    expect(actionOf(spy, DELETE_NEWSLETTER).payload).toEqual({ id: 31 });
  });

  test("does not mutate subscribers when confirmations are cancelled", () => {
    jest.spyOn(window, "confirm").mockReturnValue(false);
    const spy = renderPage(<AdminNewsletter />, "/admin/newsletter");
    spy.mockClear();
    fireEvent.click(screen.getByTitle("Click to Change Status"));
    fireEvent.click(screen.getByRole("button"));
    expect(actionOf(spy, UPDATE_NEWSLETTER)).toBeUndefined();
    expect(actionOf(spy, DELETE_NEWSLETTER)).toBeUndefined();
  });

  test("keeps an active contact message in the list without a delete control", () => {
    renderPage(<AdminContactUs />, "/admin/contactus");
    expect(screen.getByText("Question about an existing order")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  test("loads users and sends the selected user delete request", async () => {
    jest.spyOn(window, "confirm").mockReturnValue(true);
    global.fetch = jest
      .fn()
      .mockResolvedValueOnce({
        json: async () => [
          {
            userid: "u7",
            name: "Admin User",
            username: "admin7",
            email: "admin@example.com",
            phone: "1234567890",
            role: "Admin",
          },
        ],
      })
      .mockResolvedValue({ json: async () => [] });
    renderPage(<AdminUser />, "/admin/user");
    expect(await screen.findByText("admin7")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button"));
    await waitFor(() =>
      expect(global.fetch).toHaveBeenCalledWith(
        `${process.env.REACT_APP_SERVER}/user/u7`,
        expect.objectContaining({ method: "DELETE" }),
      ),
    );
  });

  test("reports a failed user list response", async () => {
    const alertSpy = jest.spyOn(window, "alert").mockImplementation(() => {});
    global.fetch = jest.fn().mockResolvedValue({ json: async () => null });
    renderPage(<AdminUser />, "/admin/user");
    await waitFor(() =>
      expect(alertSpy).toHaveBeenCalledWith("Something Went Wrong"),
    );
  });
});
