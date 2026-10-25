import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import ForgotPassword from "./ForgotPassword";

const token = "a".repeat(43);
function setup(withToken = false) {
  render(<MemoryRouter initialEntries={["/forgot-password" + (withToken ? "?token=" + token : "")]}><ForgotPassword /></MemoryRouter>);
}
function enterPassword(value = "ReplacementPassword123", confirmation = value) {
  fireEvent.change(screen.getByLabelText("New Password"), { target: { value } });
  fireEvent.change(screen.getByLabelText("Confirm Password"), { target: { value: confirmation } });
}
beforeEach(() => { global.fetch = jest.fn(); });

test("requests only a reset email and never asks for a phone or new password", async () => {
  global.fetch.mockResolvedValue({ ok: true, json: async () => ({ message: "If the account exists, a reset link will be sent." }) });
  setup();
  expect(screen.queryByLabelText("New Password")).not.toBeInTheDocument();
  expect(screen.queryByText(/Phone Number/)).not.toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Username or Email"), { target: { value: " buyer@example.test " } });
  fireEvent.click(screen.getByRole("button", { name: "Send Reset Link" }));
  expect(await screen.findByRole("status")).toHaveTextContent("If the account exists");
  expect(global.fetch).toHaveBeenCalledWith(process.env.REACT_APP_SERVER + "/user/forgot-password", {
    method: "POST", headers: { "content-type": "application/json" },
    body: JSON.stringify({ usernameOrEmail: "buyer@example.test" }),
  });
});

test("requires an identity before requesting email", async () => {
  setup();
  fireEvent.click(screen.getByRole("button", { name: "Send Reset Link" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Username or email is required");
  expect(global.fetch).not.toHaveBeenCalled();
});

test("uses the emailed token to reset the password", async () => {
  global.fetch.mockResolvedValue({ ok: true, json: async () => ({ message: "Password reset successfully. Please sign in." }) });
  setup(true);
  expect(screen.queryByLabelText("Username or Email")).not.toBeInTheDocument();
  enterPassword();
  fireEvent.click(screen.getByRole("button", { name: "Reset Password" }));
  expect(await screen.findByRole("status")).toHaveTextContent("Password reset successfully");
  expect(global.fetch).toHaveBeenCalledWith(process.env.REACT_APP_SERVER + "/user/reset-password", {
    method: "POST", headers: { "content-type": "application/json" },
    body: JSON.stringify({ token, newPassword: "ReplacementPassword123" }),
  });
  expect(screen.queryByRole("button", { name: "Reset Password" })).not.toBeInTheDocument();
});

test("rejects short and mismatched passwords without contacting the server", async () => {
  setup(true);
  enterPassword("short");
  fireEvent.click(screen.getByRole("button", { name: "Reset Password" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("between 12 and 72");
  enterPassword("ReplacementPassword123", "DifferentPassword123");
  fireEvent.click(screen.getByRole("button", { name: "Reset Password" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Passwords do not match");
  expect(global.fetch).not.toHaveBeenCalled();
});

test.each([400, 429])("shows expired-link and rate-limit errors from HTTP %s", async (status) => {
  global.fetch.mockResolvedValue({ ok: false, status, json: async () => ({ message: "Reset link is invalid or expired" }) });
  setup(true); enterPassword();
  fireEvent.click(screen.getByRole("button", { name: "Reset Password" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("invalid or expired");
  expect(screen.getByRole("link", { name: "Request a new link" })).toHaveAttribute("href", "/forgot-password");
});

test("disables duplicate submission and recovers after network failure", async () => {
  let reject;
  global.fetch.mockImplementation(() => new Promise((resolve, fail) => { reject = fail; }));
  setup(true); enterPassword();
  fireEvent.click(screen.getByRole("button", { name: "Reset Password" }));
  expect(screen.getByRole("button", { name: "Please wait..." })).toBeDisabled();
  fireEvent.click(screen.getByRole("button", { name: "Please wait..." }));
  expect(global.fetch).toHaveBeenCalledTimes(1);
  reject(new Error("Network unavailable"));
  expect(await screen.findByRole("alert")).toHaveTextContent("Network unavailable");
  expect(screen.getByRole("button", { name: "Reset Password" })).toBeEnabled();
});

test("toggles password visibility", () => {
  setup(true);
  expect(screen.getByLabelText("New Password")).toHaveAttribute("type", "password");
  fireEvent.click(screen.getByRole("button", { name: "Toggle password visibility" }));
  expect(screen.getByLabelText("New Password")).toHaveAttribute("type", "text");
});
