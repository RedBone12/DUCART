import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import Profile from "./Profile";

const user = {
  name: "Demo Buyer",
  username: "buyer",
  phone: "+3531234567",
  email: "buyer@example.com",
  address: "1 Shop Street",
  pin: "H91TEST",
  city: "Galway",
  state: "County Galway",
  pic: "buyer.jpg",
};

function setupProfile(profile = user, title = "Buyer Profile") {
  global.fetch.mockResolvedValue({
    ok: true,
    json: jest.fn().mockResolvedValue(profile),
  });

  return render(
    <MemoryRouter initialEntries={["/profile"]}>
      <Routes>
        <Route path="/profile" element={<Profile title={title} />} />
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("Profile", () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem("userid", "42");
    localStorage.setItem("token", "buyer-token");
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("loads buyer details and resolves a stored profile filename", async () => {
    setupProfile();

    expect(await screen.findByRole("cell", { name: "Demo Buyer" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "buyer@example.com" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "1 Shop Street" })).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Profile" })).toHaveAttribute(
      "src",
      `${process.env.REACT_APP_SERVER}/uploads/users/buyer.jpg`,
    );
    expect(screen.getByRole("link", { name: "Update Profile" })).toHaveAttribute(
      "href",
      "/update-profile",
    );
  });

  test("uses an uploaded path as-is and falls back when the image fails", async () => {
    setupProfile({ ...user, pic: "/uploads/users/buyer.jpg" });
    const image = await screen.findByRole("img", { name: "Profile" });

    expect(image).toHaveAttribute(
      "src",
      `${process.env.REACT_APP_SERVER}/uploads/users/buyer.jpg`,
    );

    fireEvent.error(image);
    expect(image).toHaveAttribute("src", "/img/noimage.png");
  });

  test("supports absolute and missing profile pictures", async () => {
    const { unmount } = setupProfile(
      { ...user, pic: "https://cdn.example.com/profile.jpg" },
      "Admin Profile",
    );
    expect(await screen.findByRole("img", { name: "Profile" })).toHaveAttribute(
      "src",
      "https://cdn.example.com/profile.jpg",
    );
    unmount();

    setupProfile({ ...user, pic: "" });
    expect(await screen.findByRole("img", { name: "No Profile" })).toHaveAttribute(
      "src",
      "/img/noimage.png",
    );
  });

  test("redirects to login when the profile response is unsuccessful", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      json: jest.fn().mockResolvedValue(null),
    });
    render(
      <MemoryRouter initialEntries={["/profile"]}>
        <Routes>
          <Route path="/profile" element={<Profile title="Buyer Profile" />} />
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>,
    );
    expect(await screen.findByText("Login page")).toBeInTheDocument();
  });

  test("uses the checkout layout and handles a network failure", async () => {
    const { unmount } = setupProfile(user, "Checkout");
    expect(await screen.findByRole("cell", { name: "Demo Buyer" })).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Profile" }).parentElement).toHaveClass("d-none");
    unmount();

    jest.restoreAllMocks();
    const error = new Error("offline");
    const consoleSpy = jest.spyOn(console, "error").mockImplementation(() => {});
    global.fetch = jest.fn().mockRejectedValue(error);
    render(<MemoryRouter><Profile title="Retry Profile" /></MemoryRouter>);
    expect(await screen.findByText("Retry Profile")).toBeInTheDocument();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(consoleSpy).toHaveBeenCalledWith("Failed to fetch profile:", error);
  });
});
