import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import ErrorPage from "./ErrorPage";
import { isLoggedIn } from "../config/auth";

jest.mock("../config/auth", () => ({ isLoggedIn: jest.fn() }));

function renderError(pathname) {
  window.history.pushState({}, "", pathname);
  render(
    <MemoryRouter initialEntries={["/missing"]}>
      <Routes>
        <Route path="/missing" element={<ErrorPage />} />
        <Route path="/admin" element={<div>Admin destination</div>} />
        <Route path="/profile" element={<div>Profile destination</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("ErrorPage route recovery", () => {
  afterEach(() => {
    jest.clearAllMocks();
    window.history.pushState({}, "", "/");
  });

  test.each([
    ["/admin", "Admin destination"],
    ["/profile", "Profile destination"],
  ])("returns an authenticated user to %s", async (pathname, destination) => {
    isLoggedIn.mockReturnValue(true);
    renderError(pathname);
    expect(await screen.findByText(destination)).toBeInTheDocument();
  });

  test("keeps an anonymous user on the 404 page", () => {
    isLoggedIn.mockReturnValue(false);
    renderError("/admin");
    expect(screen.getByRole("heading", { name: "404" })).toBeInTheDocument();
  });
});
