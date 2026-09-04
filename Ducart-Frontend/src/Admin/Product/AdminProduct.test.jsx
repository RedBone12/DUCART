import { configureStore } from "@reduxjs/toolkit";
import { fireEvent, render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import AdminProduct from "./AdminProduct";
import AdminCreateProduct from "./AdminCreateProduct";
import AdminUpdateProduct from "./AdminUpdateProduct";
import { CREATE_PRODUCT, DELETE_PRODUCT, UPDATE_PRODUCT } from "../../Redux/Constants";
import { formatCurrency } from "../../config/siteConfig";

jest.mock("jquery", () => () => ({ DataTable: jest.fn() }));
jest.mock("datatables.net", () => ({}));

const product = {
  id: 1,
  name: "Premium Dog Food",
  maincategory: "Pets",
  subcategory: "Food",
  brand: "Acme",
  color: "Brown",
  size: "2 kg",
  basePrice: 100,
  discount: 20,
  finalPrice: 80,
  stock: true,
  stockQuantity: 5,
  active: true,
  pic: ["/uploads/products/dog-food.jpg"],
  description: "<p>Healthy food</p>",
};

function createStore(products = [product]) {
  const initialState = {
    ProductStateData: products,
    MaincategoryStateData: [{ id: 1, name: "Pets", active: true }],
    SubcategoryStateData: [{ id: 1, name: "Food", active: true }],
    BrandStateData: [{ id: 1, name: "Acme", active: true }],
  };
  return configureStore({
    reducer: (state = initialState) => state,
    preloadedState: initialState,
    middleware: () => [],
  });
}

function renderProductAdmin(element, route, products = [product], routePattern = route) {
  const store = createStore(products);
  const dispatchSpy = jest.spyOn(store, "dispatch");

  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path={routePattern} element={element} />
          <Route path="/admin/product" element={<div>Product list page</div>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );

  return { dispatchSpy };
}

describe("Admin product CRUD", () => {
  beforeEach(() => {
    URL.createObjectURL = jest.fn(() => "blob:preview");
    window.RichTextEditor = jest.fn(() => ({
      setHTMLCode: jest.fn(),
      getHTMLCode: jest.fn(() => "<p>Healthy food</p>"),
    }));
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("lists product data with create and update destinations", () => {
    renderProductAdmin(<AdminProduct />, "/admin/product");

    expect(screen.getByText("Premium Dog Food")).toBeInTheDocument();
    expect(screen.getByText(formatCurrency(100))).toBeInTheDocument();
    expect(screen.getByText(formatCurrency(80))).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Premium Dog Food 1" })).toHaveAttribute(
      "src",
      `${process.env.REACT_APP_SERVER}/uploads/products/dog-food.jpg`,
    );
    expect(screen.getByRole("link", { name: "Create product" })).toHaveAttribute(
      "href",
      "/admin/product/create",
    );
    expect(
      screen.getByRole("link", { name: "Edit Premium Dog Food" }),
    ).toHaveAttribute("href", "/admin/product/update/1");
  });

  test("deletes a product after confirmation", () => {
    jest.spyOn(window, "confirm").mockReturnValue(true);
    const { dispatchSpy } = renderProductAdmin(
      <AdminProduct />,
      "/admin/product",
    );
    dispatchSpy.mockClear();

    fireEvent.click(
      screen.getByRole("button", { name: "Delete Premium Dog Food" }),
    );

    expect(dispatchSpy).toHaveBeenCalledWith({
      type: DELETE_PRODUCT,
      payload: { id: 1 },
    });
  });

  test("shows product validation messages before dispatching create", () => {
    const { dispatchSpy } = renderProductAdmin(
      <AdminCreateProduct />,
      "/admin/product/create",
      [],
    );
    dispatchSpy.mockClear();

    fireEvent.click(screen.getByRole("button", { name: "Create" }));

    expect(screen.getByText("Name Field is Mandatory")).toBeInTheDocument();
    expect(screen.getByText("Pic Field is Mandatory")).toBeInTheDocument();
    expect(dispatchSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: CREATE_PRODUCT }),
    );
  });

  test("creates a valid product with multiple multipart fields", async () => {
    const { dispatchSpy } = renderProductAdmin(
      <AdminCreateProduct />,
      "/admin/product/create",
      [],
    );
    dispatchSpy.mockClear();
    const picture = new File(["image"], "dog-food.png", {
      type: "image/png",
    });

    fireEvent.change(screen.getByLabelText("Product name"), {
      target: { value: "Premium Dog Food" },
    });
    fireEvent.change(screen.getByPlaceholderText("Product Color"), {
      target: { value: "Brown" },
    });
    fireEvent.change(screen.getByPlaceholderText("Product Size"), {
      target: { value: "2 kg" },
    });
    fireEvent.change(screen.getByPlaceholderText("Product Base Price"), {
      target: { value: "100" },
    });
    fireEvent.change(screen.getByPlaceholderText("Product Discount"), {
      target: { value: "20" },
    });
    fireEvent.change(screen.getByPlaceholderText("Product Stock Quantity"), {
      target: { value: "5" },
    });
    fireEvent.change(screen.getByLabelText("Product pictures"), {
      target: { files: [picture] },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create" }));

    expect(await screen.findByText("Product list page")).toBeInTheDocument();
    const createAction = dispatchSpy.mock.calls
      .map(([action]) => action)
      .find((action) => action.type === CREATE_PRODUCT);
    expect(createAction.payload).toBeInstanceOf(FormData);
    expect(createAction.payload.getAll("pic")).toEqual([picture]);
    expect(createAction.payload.get("data")).toBeInstanceOf(Blob);
  });

  test("loads and updates an existing product as multipart data", async () => {
    const { dispatchSpy } = renderProductAdmin(
      <AdminUpdateProduct />,
      "/admin/product/update/1",
      [product],
      "/admin/product/update/:id",
    );
    expect(await screen.findByDisplayValue("Premium Dog Food")).toBeInTheDocument();
    dispatchSpy.mockClear();

    fireEvent.change(screen.getByPlaceholderText("Product Color"), {
      target: { value: "Golden Brown" },
    });
    fireEvent.change(screen.getByPlaceholderText("Product Base Price"), {
      target: { value: "120" },
    });
    fireEvent.change(screen.getByPlaceholderText("Product Discount"), {
      target: { value: "25" },
    });
    const replacement = new File(["new image"], "replacement.png", {
      type: "image/png",
    });
    fireEvent.change(document.getElementsByName("pic")[0], {
      target: { files: [replacement] },
    });
    fireEvent.change(screen.getAllByRole("combobox")[3], {
      target: { value: "0" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Update" }));

    expect(await screen.findByText("Product list page")).toBeInTheDocument();
    const updateAction = dispatchSpy.mock.calls
      .map(([action]) => action)
      .find((action) => action.type === UPDATE_PRODUCT);
    expect(updateAction.payload).toBeInstanceOf(FormData);
    expect(updateAction.payload.get("id")).toBe("1");
    expect(updateAction.payload.getAll("pic")).toEqual([replacement]);
  });

  test("shows every edited-field validation error before updating a product", async () => {
    const { dispatchSpy } = renderProductAdmin(
      <AdminUpdateProduct />,
      "/admin/product/update/1",
      [product],
      "/admin/product/update/:id",
    );
    await screen.findByDisplayValue("Premium Dog Food");
    dispatchSpy.mockClear();
    fireEvent.change(screen.getAllByPlaceholderText("Product Name")[0], {
      target: { value: "" },
    });
    fireEvent.change(screen.getByPlaceholderText("Product Color"), {
      target: { value: "" },
    });
    fireEvent.change(screen.getByPlaceholderText("Product Size"), {
      target: { value: "" },
    });
    fireEvent.change(screen.getByPlaceholderText("Product Base Price"), {
      target: { value: "0" },
    });
    fireEvent.change(screen.getByPlaceholderText("Product Discount"), {
      target: { value: "101" },
    });
    fireEvent.change(screen.getByPlaceholderText("Product Stock Quantity"), {
      target: { value: "-1" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Update" }));
    expect(screen.getByText("name Field is Mandatory")).toBeInTheDocument();
    expect(screen.getByText("Price Must be a Value Greater than 0")).toBeInTheDocument();
    expect(screen.getByText("Discount Field Must Be 0-100")).toBeInTheDocument();
    expect(dispatchSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: UPDATE_PRODUCT }),
    );
  });

  test("removes an existing product picture without mutating Redux data", async () => {
    renderProductAdmin(
      <AdminUpdateProduct />,
      "/admin/product/update/1",
      [product],
      "/admin/product/update/:id",
    );
    const picture = await screen.findByRole("img", {
      name: "Remove Premium Dog Food picture 1",
    });
    fireEvent.click(picture);
    expect(
      screen.queryByRole("img", { name: "Remove Premium Dog Food picture 1" }),
    ).not.toBeInTheDocument();
    expect(product.pic).toEqual(["/uploads/products/dog-food.jpg"]);
  });
});
