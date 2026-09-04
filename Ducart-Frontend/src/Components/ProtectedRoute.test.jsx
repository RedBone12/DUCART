import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import ProtectedRoute from "./ProtectedRoute";

function authenticate(role) {
  localStorage.setItem("token", `${role.toLowerCase()}-token`);
  localStorage.setItem("role", role);
}

function setupRoute(allowedRoles) {
  render(
    <MemoryRouter initialEntries={["/protected"]}>
      <Routes>
        <Route element={<ProtectedRoute allowedRoles={allowedRoles} />}>
          <Route path="/protected" element={<div>Protected page</div>} />
        </Route>
        <Route path="/login" element={<div>Login page</div>} />
        <Route path="/profile" element={<div>Buyer profile page</div>} />
        <Route path="/admin" element={<div>Admin page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("ProtectedRoute", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  test("redirects an unauthenticated visitor to login", async () => {
    setupRoute(["Buyer"]);

    expect(await screen.findByText("Login page")).toBeInTheDocument();
    expect(screen.queryByText("Protected page")).not.toBeInTheDocument();
  });

  test("allows a buyer to enter buyer routes", () => {
    authenticate("Buyer");
    setupRoute(["Buyer"]);

    expect(screen.getByText("Protected page")).toBeInTheDocument();
  });

  test("redirects an admin away from buyer routes", async () => {
    authenticate("Admin");
    setupRoute(["Buyer"]);

    expect(await screen.findByText("Admin page")).toBeInTheDocument();
  });

  test("allows an admin to enter admin routes", () => {
    authenticate("Admin");
    setupRoute(["Admin"]);

    expect(screen.getByText("Protected page")).toBeInTheDocument();
  });

  test("redirects a buyer away from admin routes", async () => {
    authenticate("Buyer");
    setupRoute(["Admin"]);

    expect(await screen.findByText("Buyer profile page")).toBeInTheDocument();
  });
});
