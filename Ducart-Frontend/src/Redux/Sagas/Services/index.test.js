import {
  createMultipartRecord,
  createRecord,
  deleteRecord,
  getRecord,
  updateMultipartRecord,
  updateRecord,
} from "./index";

function responseWith(text) {
  return { text: jest.fn().mockResolvedValue(text) };
}

describe("Redux API services", () => {
  beforeEach(() => {
    localStorage.clear();
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("creates a JSON record without an authorization token", async () => {
    global.fetch.mockResolvedValue(responseWith('{"id":1,"name":"Demo"}'));

    await expect(createRecord("newsletter", { email: "a@b.com" })).resolves.toEqual({
      id: 1,
      name: "Demo",
    });
    expect(global.fetch).toHaveBeenCalledWith(
      `${process.env.REACT_APP_SERVER}/newsletter`,
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ email: "a@b.com" }),
      },
    );
  });

  test("creates multipart data with authentication and accepts an empty body", async () => {
    localStorage.setItem("token", "admin-token");
    global.fetch.mockResolvedValue(responseWith(""));
    const formData = new FormData();
    formData.append("name", "Demo");

    await expect(createMultipartRecord("brand", formData)).resolves.toEqual({});
    expect(global.fetch).toHaveBeenCalledWith(
      `${process.env.REACT_APP_SERVER}/brand`,
      {
        method: "POST",
        headers: { Authorization: "Bearer admin-token" },
        body: formData,
      },
    );
  });

  test("omits authorization from multipart requests for a guest", async () => {
    global.fetch.mockResolvedValue(responseWith("{}"));
    const formData = new FormData();

    await createMultipartRecord("brand", formData);

    expect(global.fetch).toHaveBeenCalledWith(
      `${process.env.REACT_APP_SERVER}/brand`,
      expect.objectContaining({ headers: {} }),
    );
  });

  test("gets records with authentication and preserves a plain-text response", async () => {
    localStorage.setItem("token", "buyer-token");
    global.fetch.mockResolvedValue(responseWith("Service unavailable"));

    await expect(getRecord("cart/me")).resolves.toEqual({
      message: "Service unavailable",
    });
    expect(global.fetch).toHaveBeenCalledWith(
      `${process.env.REACT_APP_SERVER}/cart/me`,
      {
        method: "GET",
        headers: {
          "content-type": "application/json",
          Authorization: "Bearer buyer-token",
        },
      },
    );
  });

  test("updates a JSON record at its id endpoint", async () => {
    localStorage.setItem("token", "admin-token");
    global.fetch.mockResolvedValue(responseWith('{"id":4,"active":false}'));
    const record = { id: 4, active: false };

    await expect(updateRecord("product", record)).resolves.toEqual(record);
    expect(global.fetch).toHaveBeenCalledWith(
      `${process.env.REACT_APP_SERVER}/product/4`,
      {
        method: "PUT",
        headers: {
          "content-type": "application/json",
          Authorization: "Bearer admin-token",
        },
        body: JSON.stringify(record),
      },
    );
  });

  test("updates multipart data using the id stored in FormData", async () => {
    localStorage.setItem("token", "admin-token");
    global.fetch.mockResolvedValue(responseWith('{"id":"9"}'));
    const formData = new FormData();
    formData.append("id", "9");

    await expect(updateMultipartRecord("brand", formData)).resolves.toEqual({
      id: "9",
    });
    expect(global.fetch).toHaveBeenCalledWith(
      `${process.env.REACT_APP_SERVER}/brand/9`,
      {
        method: "PUT",
        headers: { Authorization: "Bearer admin-token" },
        body: formData,
      },
    );
  });

  test("deletes a record and parses the server response", async () => {
    localStorage.setItem("token", "admin-token");
    global.fetch.mockResolvedValue(responseWith('{"deleted":true}'));

    await expect(deleteRecord("testimonial", { id: 3 })).resolves.toEqual({
      deleted: true,
    });
    expect(global.fetch).toHaveBeenCalledWith(
      `${process.env.REACT_APP_SERVER}/testimonial/3`,
      {
        method: "DELETE",
        headers: {
          "content-type": "application/json",
          Authorization: "Bearer admin-token",
        },
      },
    );
  });
});
