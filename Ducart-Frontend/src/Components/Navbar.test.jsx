import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import Navbar from "./Navbar";

function setupNavbar() {
  render(
    <MemoryRouter initialEntries={["/"]}>
      <Navbar />
      <Routes>
        <Route path="/" element={<div>Home page</div>} />
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

function authenticate(role, name) {
  localStorage.setItem("token", `${role.toLowerCase()}-token`);
  localStorage.setItem("userid", role === "Buyer" ? "42" : "1");
  localStorage.setItem("username", role.toLowerCase());
  localStorage.setItem("name", name);
  localStorage.setItem("role", role);
}

describe("Navbar", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  test("shows public navigation and login to a guest", () => {
    setupNavbar();

    expect(screen.getByRole("link", { name: "Home" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Shop" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Login" })).toHaveAttribute(
      "href",
      "/login",
    );
    expect(screen.queryByText("Logout")).not.toBeInTheDocument();
  });

  test("shows buyer profile, cart, and checkout links", () => {
    authenticate("Buyer", "Demo Buyer");
    setupNavbar();

    expect(screen.getByText("Demo Buyer")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Profile" })).toHaveAttribute(
      "href",
      "/profile",
    );
    expect(screen.getByRole("link", { name: "Cart" })).toHaveAttribute(
      "href",
      "/cart",
    );
    expect(screen.getByRole("link", { name: "Checkout" })).toHaveAttribute(
      "href",
      "/checkout",
    );
    expect(screen.queryByRole("link", { name: "Login" })).not.toBeInTheDocument();
  });

  test("shows only the admin profile destination to an admin", () => {
    authenticate("Admin", "Demo Admin");
    setupNavbar();

    expect(screen.getByText("Demo Admin")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Profile" })).toHaveAttribute(
      "href",
      "/admin",
    );
    expect(screen.queryByRole("link", { name: "Cart" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Checkout" })).not.toBeInTheDocument();
  });

  test("clears authentication and navigates to login on logout", async () => {
    authenticate("Buyer", "Demo Buyer");
    setupNavbar();

    fireEvent.click(screen.getByRole("button", { name: "Logout" }));

    expect(await screen.findByText("Login page")).toBeInTheDocument();
    expect(localStorage.getItem("token")).toBeNull();
    expect(localStorage.getItem("userid")).toBeNull();
    expect(localStorage.getItem("role")).toBeNull();
  });
});
