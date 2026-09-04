import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import Login from "./Login";

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={["/login"]}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/profile" element={<div>Buyer profile page</div>} />
        <Route path="/admin" element={<div>Admin dashboard</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

function submitCredentials(username = "buyer", password = "buyer123") {
  fireEvent.change(screen.getByPlaceholderText("User Name"), {
    target: { value: username },
  });
  fireEvent.change(screen.getByPlaceholderText("Password"), {
    target: { value: password },
  });
  fireEvent.click(screen.getByRole("button", { name: "Login" }));
}
//describe用来把相关测试分成一组
describe("Login", () => {
  beforeEach(() => {
    localStorage.clear();
    global.fetch = jest.fn();
    jest.spyOn(console, "log").mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("renders the login form and account links", () => {
    renderLogin();

    expect(
      screen.getByRole("heading", { name: "Login to Your Account" }),
    ).toBeInTheDocument();
    expect(screen.getByPlaceholderText("User Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Password")).toHaveAttribute(
      "type",
      "password",
    );
    expect(screen.getByRole("link", { name: "Forgot Password?" })).toHaveAttribute(
      "href",
      "/forgot-password",
    );
    expect(
      screen.getByRole("link", { name: "Don't Have an Account? Signup" }),
    ).toHaveAttribute("href", "/signup");
  });

  test("toggles password visibility", () => {
    renderLogin();
    const passwordInput = screen.getByPlaceholderText("Password");
    const toggleButton = screen.getByRole("button", {
      name: "Toggle password visibility",
    });

    fireEvent.click(toggleButton);
    expect(passwordInput).toHaveAttribute("type", "text");

    fireEvent.click(toggleButton);
    expect(passwordInput).toHaveAttribute("type", "password");
  });

  test("submits the credentials and displays an error when login is rejected", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 401,
      json: jest.fn().mockResolvedValue({ message: "Invalid credentials" }),
    });

    renderLogin();
    submitCredentials("wrong-user", "wrong-password");

    expect(
      await screen.findByText("Invalid Username or Password"),
    ).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith(
      `${process.env.REACT_APP_SERVER}/user/login`,
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          username: "wrong-user",
          password: "wrong-password",
        }),
      },
    );
  });

  test("displays an error when the login request cannot reach the server", async () => {
    global.fetch.mockRejectedValue(new Error("Network unavailable"));

    renderLogin();
    submitCredentials();

    expect(
      await screen.findByText("Invalid Username or Password"),
    ).toBeInTheDocument();
    expect(localStorage.getItem("token")).toBeNull();
  });

  test("stores the authenticated buyer and navigates to the profile page", async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: jest.fn().mockResolvedValue({
        token: "buyer-token",
        userid: "42",
        username: "buyer",
        name: "Demo Buyer",
        role: "Buyer",
      }),
    });

    renderLogin();
    submitCredentials();

    expect(await screen.findByText("Buyer profile page")).toBeInTheDocument();
    expect(localStorage.getItem("token")).toBe("buyer-token");
    expect(localStorage.getItem("userid")).toBe("42");
    expect(localStorage.getItem("username")).toBe("buyer");
    expect(localStorage.getItem("name")).toBe("Demo Buyer");
    expect(localStorage.getItem("role")).toBe("Buyer");
  });

  test("navigates an authenticated admin to the admin dashboard", async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: jest.fn().mockResolvedValue({
        token: "admin-token",
        userid: "1",
        username: "admin",
        name: "Demo Admin",
        role: "Admin",
      }),
    });

    renderLogin();
    submitCredentials("admin", "admin123");

    expect(await screen.findByText("Admin dashboard")).toBeInTheDocument();
    expect(localStorage.getItem("role")).toBe("Admin");
  });
});
