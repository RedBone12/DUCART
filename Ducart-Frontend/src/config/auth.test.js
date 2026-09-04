import {
  authJsonHeaders,
  authOnlyHeaders,
  clearAuth,
  getRole,
  getToken,
  isLoggedIn,
  saveAuth,
} from "./auth";

describe("auth utilities", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  test("saves and reads authentication data", () => {
    saveAuth({
      token: "buyer-token",
      userid: "42",
      username: "buyer",
      name: "Demo Buyer",
      role: "Buyer",
    });

    expect(getToken()).toBe("buyer-token");
    expect(getRole()).toBe("Buyer");
    expect(isLoggedIn()).toBe(true);
    expect(localStorage.getItem("userid")).toBe("42");
    expect(localStorage.getItem("name")).toBe("Demo Buyer");
  });

  test("clears every authentication value", () => {
    saveAuth({ token: "token", userid: "1", role: "Admin" });

    clearAuth();

    expect(getToken()).toBeNull();
    expect(getRole()).toBeNull();
    expect(isLoggedIn()).toBe(false);
    expect(localStorage.getItem("userid")).toBeNull();
  });

  test("builds anonymous request headers", () => {
    expect(authJsonHeaders()).toEqual({ "content-type": "application/json" });
    expect(authOnlyHeaders()).toEqual({});
  });

  test("adds the bearer token to authenticated request headers", () => {
    localStorage.setItem("token", "buyer-token");

    expect(authJsonHeaders()).toEqual({
      "content-type": "application/json",
      Authorization: "Bearer buyer-token",
    });
    expect(authOnlyHeaders()).toEqual({
      Authorization: "Bearer buyer-token",
    });
  });

  test("stores empty strings for omitted authentication fields", () => {
    saveAuth({});
    expect(localStorage.getItem("token")).toBe("");
    expect(localStorage.getItem("userid")).toBe("");
    expect(localStorage.getItem("username")).toBe("");
    expect(localStorage.getItem("name")).toBe("");
    expect(localStorage.getItem("role")).toBe("");
  });
});
