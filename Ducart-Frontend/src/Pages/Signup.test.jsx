import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import Signup from "./Signup";

function renderSignup() {
  return render(
    <MemoryRouter initialEntries={["/signup"]}>
      <Routes>
        <Route path="/signup" element={<Signup />} />
        <Route path="/login" element={<div>Login destination</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

function enterValidSignup(overrides = {}) {
  const placeholders = {
    name: "Full Name",
    username: "User Name",
    email: "Email Address",
    phone: "Phone Number",
    password: "Password",
    cpassword: "Confirm Password",
  };
  const values = {
    name: "Demo Buyer",
    username: "demo-buyer",
    email: "buyer@example.com",
    phone: "+353871234567",
    password: "StrongPass1",
    cpassword: "StrongPass1",
    ...overrides,
  };

  Object.entries(values).forEach(([name, value]) => {
    fireEvent.change(screen.getByPlaceholderText(placeholders[name]), {
      target: { name, value },
    });
  });
}

describe("Signup", () => {
  beforeEach(() => {
    global.fetch = jest.fn();
    jest.spyOn(window, "alert").mockImplementation(() => {});
    jest.spyOn(console, "log").mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("renders the signup form and login link", () => {
    renderSignup();

    expect(
      screen.getByRole("heading", { name: "Create Your Free Account" }),
    ).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Full Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Confirm Password")).toHaveAttribute(
      "type",
      "password",
    );
    expect(
      screen.getByRole("link", { name: "Already Have an Account?Login" }),
    ).toHaveAttribute("href", "/login");
  });

  test("shows required validation errors without calling the API", () => {
    renderSignup();

    fireEvent.click(screen.getByRole("button", { name: "Signup" }));

    expect(screen.getByText("Name Field is Mandatory")).toBeInTheDocument();
    expect(screen.getByText("User Name Field is Mandatory")).toBeInTheDocument();
    expect(screen.getByText("Email Field is Mandatory")).toBeInTheDocument();
    expect(screen.getByText("Phone Field is Mandatory")).toBeInTheDocument();
    expect(screen.getByText("Password Field is Mandatory")).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  test("validates field formats before submission", () => {
    renderSignup();
    enterValidSignup({
      email: "not-an-email",
      phone: "123",
      password: "weak",
      cpassword: "weak",
    });

    fireEvent.click(screen.getByRole("button", { name: "Signup" }));

    expect(screen.getByText("Invalid Email Address")).toBeInTheDocument();
    expect(screen.getByText("Invalid Phone Number")).toBeInTheDocument();
    expect(screen.getByText(/Invalid Password!/)).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  test("blocks submission when password confirmation does not match", () => {
    renderSignup();
    enterValidSignup({ cpassword: "DifferentPass1" });

    fireEvent.click(screen.getByRole("button", { name: "Signup" }));

    expect(
      screen.getByText("Password and Confirm Password do not match"),
    ).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  test("shows the matching field error for a duplicate account", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 409,
      json: jest.fn().mockResolvedValue({ message: "Username already exists" }),
    });
    renderSignup();
    enterValidSignup();

    fireEvent.click(screen.getByRole("button", { name: "Signup" }));

    expect(await screen.findByText("Username is already taken")).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith(
      `${process.env.REACT_APP_SERVER}/user`,
      expect.objectContaining({
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          name: "Demo Buyer",
          username: "demo-buyer",
          email: "buyer@example.com",
          phone: "+353871234567",
          password: "StrongPass1",
          role: "Buyer",
        }),
      }),
    );
  });

  test("shows a server response error for a failed signup", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 500,
      json: jest.fn().mockResolvedValue({ message: "Signup unavailable" }),
    });
    renderSignup();
    enterValidSignup();

    fireEvent.click(screen.getByRole("button", { name: "Signup" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Signup unavailable",
    );
  });

  test("shows a useful message when the server cannot be reached", async () => {
    global.fetch.mockRejectedValue(new Error("Network unavailable"));
    renderSignup();
    enterValidSignup();

    fireEvent.click(screen.getByRole("button", { name: "Signup" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Server error. Please try again later.",
    );
  });

  test("accepts an empty success body and navigates to login", async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      status: 201,
      json: jest.fn().mockRejectedValue(new SyntaxError("Empty response")),
    });
    renderSignup();
    enterValidSignup();

    fireEvent.click(screen.getByRole("button", { name: "Signup" }));

    expect(await screen.findByText("Login destination")).toBeInTheDocument();
    expect(window.alert).toHaveBeenCalledWith("Signup successful. Please login.");
  });
});
