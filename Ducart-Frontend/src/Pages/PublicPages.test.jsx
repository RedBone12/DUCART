import { configureStore } from "@reduxjs/toolkit";
import { fireEvent, render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter } from "react-router-dom";

import About from "../Components/About";
import CategorySlider from "../Components/CategorySlider";
import Facts from "../Components/Facts";
import Features from "../Components/Features";
import Footer from "../Components/Footer";
import Testimonial from "../Components/Testimonial";
import AboutUsPage from "./AboutUsPage";
import CartPage from "./CartPage";
import ContactUsPage from "./ContactUsPage";
import ErrorPage from "./ErrorPage";
import FeaturePage from "./FeaturePage";
import Home from "./Home";
import TestimonialPage from "./TestimonialPage";
import {
  CREATE_CONTACT_US,
  GET_BRAND,
  GET_MAINCATEGORY,
  GET_PRODUCT,
  GET_SUBCATEGORY,
  GET_TESTIMONIAL,
} from "../Redux/Constants";

jest.mock("react-owl-carousel", () => ({ children }) => (
  <div data-testid="owl-carousel">{children}</div>
));

const initialState = {
  MaincategoryStateData: [
    { id: 1, name: "Fashion & Style", pic: "/fashion.jpg", active: true },
    { id: 2, name: "Hidden category", pic: "/hidden.jpg", active: false },
  ],
  SubcategoryStateData: [
    { id: 3, name: "Men's Wear", pic: "/men.jpg", active: true },
  ],
  BrandStateData: [{ id: 4, name: "Top Brand", pic: "/brand.jpg", active: true }],
  ProductStateData: [
    {
      id: 5,
      name: "Summer Shirt",
      maincategory: "Fashion & Style",
      pic1: "/shirt.jpg",
      finalPrice: 25,
      stockQuantity: 4,
      active: true,
    },
    {
      id: 6,
      name: "Hidden Product",
      maincategory: "Fashion & Style",
      active: false,
    },
  ],
  TestimonialStateData: [
    { id: 7, name: "Happy Buyer", message: "Excellent service", pic: "/buyer.jpg", active: true },
    { id: 8, name: "Hidden Buyer", message: "Hidden", pic: "/hidden.jpg", active: false },
  ],
  CartStateData: [],
  CheckoutStateData: [],
};

function renderWithApp(ui, state = initialState) {
  const store = configureStore({
    reducer: (current = state) => current,
    preloadedState: state,
  });
  const dispatchSpy = jest.spyOn(store, "dispatch");
  const result = render(
    <Provider store={store}>
      <MemoryRouter>{ui}</MemoryRouter>
    </Provider>,
  );
  return { ...result, dispatchSpy };
}

describe("public components and pages", () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("renders the static about, facts, and features content", () => {
    renderWithApp(
      <>
        <About />
        <Facts />
        <Features />
      </>,
    );

    expect(screen.getByText(/Welcome To Ducart/)).toBeInTheDocument();
    expect(screen.getByText("Happy Customers")).toBeInTheDocument();
    expect(screen.getByText("100% Genuine and Original")).toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: "Shop Now" })[0]).toHaveAttribute(
      "href",
      "/shop",
    );
  });

  test("builds category, subcategory, and brand filter links", () => {
    const consoleSpy = jest.spyOn(console, "log").mockImplementation(() => {});
    const { rerender } = renderWithApp(
      <CategorySlider
        title="Maincategory"
        data={[{ id: 1, name: "Men & Women", pic: "/category.jpg" }]}
      />,
    );
    expect(screen.getByRole("link", { name: /Men & Women/ })).toHaveAttribute(
      "href",
      "/shop?mc=Men%20%26%20Women&sc=All&br=All",
    );
    fireEvent.error(screen.getByRole("img", { name: "Men & Women" }));
    expect(consoleSpy).toHaveBeenCalledWith(
      "Image load failed:",
      expect.stringContaining("/category.jpg"),
    );

    rerender(
      <Provider store={configureStore({ reducer: (state = initialState) => state })}>
        <MemoryRouter>
          <CategorySlider
            title="Subcategory"
            data={[{ id: 2, name: "Formal Shirts", pic: "/formal.jpg" }]}
          />
        </MemoryRouter>
      </Provider>,
    );
    expect(screen.getByRole("link", { name: /Formal Shirts/ })).toHaveAttribute(
      "href",
      "/shop?mc=All&sc=Formal%20Shirts&br=All",
    );

    rerender(
      <Provider store={configureStore({ reducer: (state = initialState) => state })}>
        <MemoryRouter>
          <CategorySlider
            title="Brand"
            data={[{ id: 3, name: "Demo Brand", pic: "/brand.jpg" }]}
          />
        </MemoryRouter>
      </Provider>,
    );
    expect(screen.getByRole("link", { name: /Demo Brand/ })).toHaveAttribute(
      "href",
      "/shop?mc=All&sc=All&br=Demo%20Brand",
    );
  });

  test("renders only active testimonials and requests testimonial data", () => {
    const { dispatchSpy } = renderWithApp(<Testimonial />);

    expect(screen.getByText("Happy Buyer")).toBeInTheDocument();
    expect(screen.queryByText("Hidden Buyer")).not.toBeInTheDocument();
    expect(dispatchSpy).toHaveBeenCalledWith({ type: GET_TESTIMONIAL });
  });

  test("home loads active catalog data and dispatches every catalog request", () => {
    const { dispatchSpy } = renderWithApp(<Home />);

    expect(screen.getAllByRole("link", { name: "Shop Now" })[0]).toHaveAttribute(
      "href",
      "/shop",
    );
    expect(screen.getAllByText("Fashion & Style").length).toBeGreaterThan(0);
    expect(screen.getByText("Summer Shirt")).toBeInTheDocument();
    expect(screen.queryByText("Hidden category")).not.toBeInTheDocument();
    expect(screen.queryByText("Hidden Product")).not.toBeInTheDocument();
    [GET_MAINCATEGORY, GET_SUBCATEGORY, GET_BRAND, GET_PRODUCT].forEach((type) => {
      expect(dispatchSpy).toHaveBeenCalledWith({ type });
    });
  });

  test("contact form validates and dispatches a valid customer query", () => {
    const { dispatchSpy } = renderWithApp(<ContactUsPage />);

    fireEvent.click(screen.getByRole("button", { name: "Submit" }));
    expect(screen.getByText("Name Field is Mandatory")).toBeInTheDocument();

    const values = {
      "Full Name": "Demo Buyer",
      "Email Address": "buyer@example.com",
      "Phone Number": "+353871234567",
      Subject: "Order question",
      "Message...": "I would like some more information about my recent order please.",
    };
    Object.entries(values).forEach(([placeholder, value]) => {
      fireEvent.change(screen.getByPlaceholderText(placeholder), {
        target: { value },
      });
    });
    fireEvent.click(screen.getByRole("button", { name: "Submit" }));

    expect(dispatchSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        type: CREATE_CONTACT_US,
        payload: expect.objectContaining({
          name: "Demo Buyer",
          email: "buyer@example.com",
          active: true,
          date: expect.any(String),
        }),
      }),
    );
    expect(screen.getByText(/Thanks to Share Your Query/)).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Full Name")).toHaveValue("");
  });

  test("footer handles duplicate and successful newsletter subscriptions", async () => {
    global.fetch.mockResolvedValueOnce({
      json: jest.fn().mockResolvedValue([{ email: "buyer@example.com" }]),
    });
    const { unmount } = renderWithApp(<Footer />);
    fireEvent.change(screen.getByPlaceholderText("Your email"), {
      target: { value: "buyer@example.com" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Subscribe" }));
    expect(
      await screen.findByText("Your Email Address is Already Registered"),
    ).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledTimes(1);
    unmount();

    global.fetch.mockReset();
    global.fetch
      .mockResolvedValueOnce({ json: jest.fn().mockResolvedValue([]) })
      .mockResolvedValueOnce({ json: jest.fn().mockResolvedValue({ id: 2 }) });
    renderWithApp(<Footer />);
    fireEvent.change(screen.getByPlaceholderText("Your email"), {
      target: { value: "new@example.com" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Subscribe" }));
    expect(
      await screen.findByText("Thanks To Subscribe Our Newsletter Service"),
    ).toBeInTheDocument();
    expect(global.fetch).toHaveBeenLastCalledWith(
      `${process.env.REACT_APP_SERVER}/newsletter`,
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ email: "new@example.com", active: true }),
      }),
    );
  });

  test("renders wrapper pages, empty cart, and the 404 destination", () => {
    const pages = [
      <AboutUsPage key="about" />,
      <FeaturePage key="features" />,
      <TestimonialPage key="testimonials" />,
      <CartPage key="cart" />,
      <ErrorPage key="error" />,
    ];
    renderWithApp(<>{pages}</>);

    expect(screen.getAllByText("About Us").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Features").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Testimonials").length).toBeGreaterThan(0);
    expect(
      screen.getByRole("heading", { name: "No Items in Cart" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Page Not Found" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Go Back To Home" })).toHaveAttribute(
      "href",
      "/",
    );
  });
});
