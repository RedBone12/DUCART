import { act, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import UpdateProfile from "./UpdateProfile";

const profile = {
  name: "Demo Buyer",
  phone: "+353871234567",
  address: "1 Shop Street",
  pin: "H91TEST",
  city: "Galway",
  state: "County Galway",
  pic: "buyer.jpg",
};

function renderUpdateProfile() {
  return render(
    <MemoryRouter initialEntries={["/update-profile"]}>
      <Routes>
        <Route path="/update-profile" element={<UpdateProfile />} />
        <Route path="/profile" element={<div>Buyer profile destination</div>} />
        <Route path="/admin" element={<div>Admin destination</div>} />
        <Route path="/login" element={<div>Login destination</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

function successfulProfileResponse() {
  return {
    ok: true,
    status: 200,
    json: jest.fn().mockResolvedValue(profile),
  };
}

describe("UpdateProfile", () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem("userid", "42");
    localStorage.setItem("token", "buyer-token");
    localStorage.setItem("role", "Buyer");
    global.fetch = jest.fn();
    jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("loads the authenticated user's current profile into the form", async () => {
    global.fetch.mockResolvedValue(successfulProfileResponse());

    renderUpdateProfile();

    expect(await screen.findByDisplayValue("Demo Buyer")).toBeInTheDocument();
    expect(screen.getByDisplayValue("+353871234567")).toBeInTheDocument();
    expect(screen.getByDisplayValue("1 Shop Street")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Galway")).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith(
      `${process.env.REACT_APP_SERVER}/user/42`,
      {
        method: "GET",
        headers: {
          "content-type": "application/json",
          Authorization: "Bearer buyer-token",
        },
      },
    );
  });

  test("redirects to login when the current profile cannot be loaded", async () => {
    global.fetch.mockResolvedValue({ ok: false, status: 401 });

    renderUpdateProfile();

    expect(await screen.findByText("Login destination")).toBeInTheDocument();
  });

  test("validates required fields at submit time while profile is loading", async () => {
    let resolveFetch;
    global.fetch.mockImplementation(
      () => new Promise((resolve) => {
        resolveFetch = resolve;
      }),
    );
    const { unmount } = renderUpdateProfile();

    fireEvent.click(screen.getByRole("button", { name: "Update" }));

    expect(screen.getByText("name Field is Mandatory")).toBeInTheDocument();
    expect(screen.getByText("phone Field is Mandatory")).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledTimes(1);

    unmount();
    await act(async () => {
      resolveFetch(successfulProfileResponse());
    });
  });

  test("shows field and image validation errors without updating", async () => {
    global.fetch.mockResolvedValue(successfulProfileResponse());
    renderUpdateProfile();
    await screen.findByDisplayValue("Demo Buyer");

    fireEvent.change(screen.getByPlaceholderText("Full Name"), {
      target: { name: "name", value: "x" },
    });
    fireEvent.change(screen.getByPlaceholderText("Phone Number"), {
      target: { name: "phone", value: "123" },
    });
    const largePicture = new File([new Uint8Array(1048577)], "large.png", {
      type: "image/png",
    });
    fireEvent.change(screen.getByLabelText("Profile picture"), {
      target: { name: "pic", files: [largePicture] },
    });
    fireEvent.click(screen.getByRole("button", { name: "Update" }));

    expect(
      screen.getByText("name Field Length must be within 3-50 characters"),
    ).toBeInTheDocument();
    expect(screen.getByText("Invalid Phone Number")).toBeInTheDocument();
    expect(screen.getByText(/Pic size is more then 1 mb/)).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  test("submits multipart profile data and returns a buyer to profile", async () => {
    global.fetch
      .mockResolvedValueOnce(successfulProfileResponse())
      .mockResolvedValueOnce({ ok: true, status: 200 });
    renderUpdateProfile();
    await screen.findByDisplayValue("Demo Buyer");

    fireEvent.change(screen.getByPlaceholderText("City Name"), {
      target: { name: "city", value: "Dublin" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Update" }));

    expect(await screen.findByText("Buyer profile destination")).toBeInTheDocument();
    const [, updateOptions] = global.fetch.mock.calls[1];
    expect(updateOptions.method).toBe("PUT");
    expect(updateOptions.headers).toEqual({ Authorization: "Bearer buyer-token" });
    expect(updateOptions.body).toBeInstanceOf(FormData);
    expect(updateOptions.body.get("data")).toBeInstanceOf(Blob);
    expect(updateOptions.body.has("pic")).toBe(false);
  });

  test("uploads a selected picture and returns an admin to the dashboard", async () => {
    localStorage.setItem("role", "Admin");
    global.fetch
      .mockResolvedValueOnce(successfulProfileResponse())
      .mockResolvedValueOnce({ ok: true, status: 200 });
    renderUpdateProfile();
    await screen.findByDisplayValue("Demo Buyer");
    const picture = new File(["picture"], "buyer.png", { type: "image/png" });

    fireEvent.change(screen.getByLabelText("Profile picture"), {
      target: { name: "pic", files: [picture] },
    });
    fireEvent.click(screen.getByRole("button", { name: "Update" }));

    expect(await screen.findByText("Admin destination")).toBeInTheDocument();
    const formData = global.fetch.mock.calls[1][1].body;
    expect(formData.get("pic")).toBe(picture);
  });

  test("shows an error when the update API rejects the request", async () => {
    global.fetch
      .mockResolvedValueOnce(successfulProfileResponse())
      .mockResolvedValueOnce({ ok: false, status: 500 });
    renderUpdateProfile();
    await screen.findByDisplayValue("Demo Buyer");

    fireEvent.click(screen.getByRole("button", { name: "Update" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Update failed. Please try again.",
    );
  });

  test("shows an error when the update request cannot reach the server", async () => {
    global.fetch
      .mockResolvedValueOnce(successfulProfileResponse())
      .mockRejectedValueOnce(new Error("Network unavailable"));
    renderUpdateProfile();
    await screen.findByDisplayValue("Demo Buyer");

    fireEvent.click(screen.getByRole("button", { name: "Update" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Server error. Please try again later.",
    );
  });
});
