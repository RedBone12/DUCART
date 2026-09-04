import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { act } from "react";

import ForgotPassword from "./ForgotPassword";

function renderForgotPassword() {
  return render(
    <MemoryRouter initialEntries={["/forgot-password"]}>
      <Routes>
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/login" element={<div>Login destination</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

function enterResetDetails(overrides = {}) {
  const placeholders = {
    usernameOrEmail: "Enter username or email",
    phone: "Enter registered phone number",
    newPassword: "New password",
    confirmPassword: "Confirm password",
  };
  const values = {
    usernameOrEmail: "buyer@example.com",
    phone: "+353871234567",
    newPassword: "NewPass1",
    confirmPassword: "NewPass1",
    ...overrides,
  };

  Object.entries(values).forEach(([name, value]) => {
    fireEvent.change(screen.getByPlaceholderText(placeholders[name]), {
      target: { name, value },
    });
  });
}

describe("ForgotPassword", () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  test("renders reset fields, navigation links, and password controls", () => {
    renderForgotPassword();

    expect(
      screen.getByRole("heading", { name: "Reset Your Password" }),
    ).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Enter username or email")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to Login" })).toHaveAttribute(
      "href",
      "/login",
    );
    expect(screen.getByRole("link", { name: "Create an Account" })).toHaveAttribute(
      "href",
      "/signup",
    );
  });

  test("toggles both password fields between hidden and visible", () => {
    renderForgotPassword();
    const newPassword = screen.getByPlaceholderText("New password");
    const confirmPassword = screen.getByPlaceholderText("Confirm password");

    fireEvent.click(
      screen.getByRole("button", { name: "Toggle password visibility" }),
    );
    expect(newPassword).toHaveAttribute("type", "text");
    expect(confirmPassword).toHaveAttribute("type", "text");

    fireEvent.click(
      screen.getByRole("button", { name: "Toggle password visibility" }),
    );
    expect(newPassword).toHaveAttribute("type", "password");
  });

  test("requires every field and treats whitespace as empty", () => {
    renderForgotPassword();
    fireEvent.change(screen.getByPlaceholderText("Enter username or email"), {
      target: { name: "usernameOrEmail", value: "   " },
    });

    fireEvent.click(screen.getByRole("button", { name: "Reset Password" }));

    expect(screen.getByText("All fields are required")).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  test("rejects a password shorter than six characters", () => {
    renderForgotPassword();
    enterResetDetails({ newPassword: "12345", confirmPassword: "12345" });

    fireEvent.click(screen.getByRole("button", { name: "Reset Password" }));

    expect(
      screen.getByText("Password must be at least 6 characters"),
    ).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  test("rejects different new and confirmation passwords", () => {
    renderForgotPassword();
    enterResetDetails({ confirmPassword: "OtherPass1" });

    fireEvent.click(screen.getByRole("button", { name: "Reset Password" }));

    expect(
      screen.getByText("New password and confirm password do not match"),
    ).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  test("shows an account mismatch returned by the server", async () => {
    global.fetch.mockResolvedValue({ ok: false, status: 400 });
    renderForgotPassword();
    enterResetDetails();

    fireEvent.click(screen.getByRole("button", { name: "Reset Password" }));

    expect(
      await screen.findByText(
        "Account details do not match. Please check and try again.",
      ),
    ).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith(
      `${process.env.REACT_APP_SERVER}/user/forgot-password`,
      {
        method: "PUT",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          usernameOrEmail: "buyer@example.com",
          phone: "+353871234567",
          newPassword: "NewPass1",
        }),
      },
    );
  });

  test("shows a server error when the request cannot be completed", async () => {
    global.fetch.mockRejectedValue(new Error("Network unavailable"));
    renderForgotPassword();
    enterResetDetails();

    fireEvent.click(screen.getByRole("button", { name: "Reset Password" }));

    expect(
      await screen.findByText("Server error. Please try again later."),
    ).toBeInTheDocument();
  });

  test("shows success and redirects to login after the delay", async () => {
    jest.useFakeTimers();
    global.fetch.mockResolvedValue({ ok: true, status: 200 });
    renderForgotPassword();
    enterResetDetails();

    fireEvent.click(screen.getByRole("button", { name: "Reset Password" }));

    expect(
      await screen.findByText("Password reset successfully. Redirecting to login..."),
    ).toBeInTheDocument();

    act(() => {
      jest.advanceTimersByTime(1500);
    });
    expect(screen.getByText("Login destination")).toBeInTheDocument();
  });
});
