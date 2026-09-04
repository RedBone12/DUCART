import { configureStore } from "@reduxjs/toolkit";
import { fireEvent, render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import AdminBrand from "./Brand/AdminBrand";
import AdminCreateBrand from "./Brand/AdminCreateBrand";
import AdminUpdateBrand from "./Brand/AdminUpdateBrand";
import AdminSubcategory from "./Subcategory/AdminSubcategory";
import AdminCreateSubcategory from "./Subcategory/AdminCreateSubcategory";
import AdminUpdateSubcategory from "./Subcategory/AdminUpdateSubcategory";
import AdminTestimonial from "./Testimonial/AdminTestimonial";
import AdminCreateTestimonial from "./Testimonial/AdminCreateTestimonial";
import AdminUpdateTestimonial from "./Testimonial/AdminUpdateTestimonial";
import {
  CREATE_BRAND,
  CREATE_SUBCATEGORY,
  CREATE_TESTIMONIAL,
  DELETE_BRAND,
  DELETE_SUBCATEGORY,
  DELETE_TESTIMONIAL,
  UPDATE_BRAND,
  UPDATE_SUBCATEGORY,
  UPDATE_TESTIMONIAL,
} from "../Redux/Constants";

jest.mock("jquery", () => () => ({ DataTable: jest.fn() }));
jest.mock("datatables.net", () => ({}));

const brand = { id: 1, name: "Acme", pic: "/brand.jpg", active: true };
const subcategory = { id: 2, name: "Shoes", pic: "/shoes.jpg", active: false };
const testimonial = {
  id: 3,
  name: "Alex",
  message: "Excellent shop",
  pic: "/alex.jpg",
  active: true,
};

function renderPage(element, route, pattern = route, overrides = {}) {
  const initialState = {
    BrandStateData: [brand],
    SubcategoryStateData: [subcategory],
    TestimonialStateData: [testimonial],
    ...overrides,
  };
  const store = configureStore({
    reducer: (state = initialState) => state,
    preloadedState: initialState,
    middleware: () => [],
  });
  const dispatchSpy = jest.spyOn(store, "dispatch");
  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path={pattern} element={element} />
          <Route path="/admin/brand" element={<div>Brand destination</div>} />
          <Route path="/admin/subcategory" element={<div>Subcategory destination</div>} />
          <Route path="/admin/testimonial" element={<div>Testimonial destination</div>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );
  return dispatchSpy;
}

function uploadPicture() {
  const picture = new File(["image"], "picture.png", { type: "image/png" });
  fireEvent.change(document.getElementsByName("pic")[0], {
    target: { files: [picture] },
  });
  return picture;
}

function dispatched(spy, type) {
  return spy.mock.calls.map(([action]) => action).find((action) => action.type === type);
}

describe("remaining admin catalog CRUD", () => {
  beforeEach(() => {
    URL.createObjectURL = jest.fn(() => "blob:preview");
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test.each([
    [<AdminBrand />, "/admin/brand", "Acme", DELETE_BRAND, 1],
    [<AdminSubcategory />, "/admin/subcategory", "Shoes", DELETE_SUBCATEGORY, 2],
    [<AdminTestimonial />, "/admin/testimonial", "Excellent shop", DELETE_TESTIMONIAL, 3],
  ])("lists and deletes records in %s", (element, route, text, type, id) => {
    jest.spyOn(window, "confirm").mockReturnValue(true);
    const spy = renderPage(element, route);
    expect(screen.getByText(text)).toBeInTheDocument();
    spy.mockClear();
    fireEvent.click(screen.getAllByRole("button").at(-1));
    expect(spy).toHaveBeenCalledWith({ type, payload: { id } });
  });

  test.each([
    [<AdminBrand />, "/admin/brand", "BrandStateData", DELETE_BRAND],
    [<AdminSubcategory />, "/admin/subcategory", "SubcategoryStateData", DELETE_SUBCATEGORY],
    [<AdminTestimonial />, "/admin/testimonial", "TestimonialStateData", DELETE_TESTIMONIAL],
  ])("handles empty lists and cancelled deletion", (element, route, stateKey, type) => {
    const confirmSpy = jest.spyOn(window, "confirm").mockReturnValue(false);
    renderPage(element, route, undefined, { [stateKey]: [] });
    expect(screen.queryAllByRole("row")).toHaveLength(1);

    const spy = renderPage(element, route);
    spy.mockClear();
    fireEvent.click(screen.getAllByRole("button").at(-1));
    expect(confirmSpy).toHaveBeenCalled();
    expect(dispatched(spy, type)).toBeUndefined();
  });

  test("creates a brand with multipart data and supports inactive selection", async () => {
    const spy = renderPage(<AdminCreateBrand />, "/admin/brand/create", undefined, {
      BrandStateData: [],
    });
    spy.mockClear();
    fireEvent.change(screen.getByRole("textbox"), { target: { value: "Nova" } });
    uploadPicture();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "0" } });
    fireEvent.click(screen.getByRole("button", { name: "Create" }));
    expect(await screen.findByText("Brand destination")).toBeInTheDocument();
    expect(dispatched(spy, CREATE_BRAND).payload).toBeInstanceOf(FormData);
  });

  test("creates a subcategory with multipart data", async () => {
    const spy = renderPage(<AdminCreateSubcategory />, "/admin/subcategory/create", undefined, {
      SubcategoryStateData: [],
    });
    spy.mockClear();
    fireEvent.change(screen.getByRole("textbox"), { target: { value: "Garden" } });
    uploadPicture();
    fireEvent.click(screen.getByRole("button", { name: "Create" }));
    expect(await screen.findByText("Subcategory destination")).toBeInTheDocument();
    expect(dispatched(spy, CREATE_SUBCATEGORY).payload).toBeInstanceOf(FormData);
  });

  test("creates a testimonial with its message and picture", async () => {
    const spy = renderPage(<AdminCreateTestimonial />, "/admin/testimonial/create", undefined, {
      TestimonialStateData: [],
    });
    spy.mockClear();
    fireEvent.change(screen.getAllByRole("textbox")[0], { target: { value: "Morgan" } });
    fireEvent.change(screen.getByPlaceholderText("Message..."), {
      target: { value: "A very helpful shopping experience with quick delivery and support." },
    });
    uploadPicture();
    fireEvent.click(screen.getByRole("button", { name: "Create" }));
    expect(await screen.findByText("Testimonial destination")).toBeInTheDocument();
    expect(dispatched(spy, CREATE_TESTIMONIAL).payload).toBeInstanceOf(FormData);
  });

  test.each([
    [<AdminCreateBrand />, "/admin/brand/create", "Acme", "Brand Name is Already Exist"],
    [<AdminCreateSubcategory />, "/admin/subcategory/create", "Shoes", "Subcategory Name is Already Exist"],
    [<AdminCreateTestimonial />, "/admin/testimonial/create", "Alex", "Testimonial Name is Already Exist"],
  ])("rejects duplicate names while creating", (element, route, name, message) => {
    const spy = renderPage(element, route);
    spy.mockClear();
    fireEvent.change(screen.getAllByRole("textbox")[0], { target: { value: name } });
    const messageBox = screen.queryByPlaceholderText("Message...");
    if (messageBox) {
      fireEvent.change(messageBox, {
        target: { value: "This testimonial message is deliberately longer than fifty characters." },
      });
    }
    uploadPicture();
    fireEvent.click(screen.getByRole("button", { name: "Create" }));
    expect(screen.getByText(message)).toBeInTheDocument();
  });

  test.each([
    [<AdminUpdateBrand />, "/admin/brand/update/1", "/admin/brand/update/:id", "Acme", "Brand destination", UPDATE_BRAND],
    [<AdminUpdateSubcategory />, "/admin/subcategory/update/2", "/admin/subcategory/update/:id", "Shoes", "Subcategory destination", UPDATE_SUBCATEGORY],
    [<AdminUpdateTestimonial />, "/admin/testimonial/update/3", "/admin/testimonial/update/:id", "Alex", "Testimonial destination", UPDATE_TESTIMONIAL],
  ])("updates the current record without flagging its own name as duplicate", async (element, route, pattern, value, destination, type) => {
    const spy = renderPage(element, route, pattern);
    expect(await screen.findByDisplayValue(value)).toBeInTheDocument();
    spy.mockClear();
    fireEvent.change(screen.getAllByRole("textbox")[0], { target: { value } });
    uploadPicture();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "0" } });
    fireEvent.click(screen.getByRole("button", { name: "Update" }));
    expect(await screen.findByText(destination)).toBeInTheDocument();
    expect(dispatched(spy, type).payload).toBeInstanceOf(FormData);
  });

  test.each([
    [<AdminUpdateBrand />, "/admin/brand/update/1", "/admin/brand/update/:id", "BrandStateData", brand, "Nova", "Brand Name is Already Exist", UPDATE_BRAND],
    [<AdminUpdateSubcategory />, "/admin/subcategory/update/2", "/admin/subcategory/update/:id", "SubcategoryStateData", subcategory, "Garden", "Subcategory Name is Already Exist", UPDATE_SUBCATEGORY],
    [<AdminUpdateTestimonial />, "/admin/testimonial/update/3", "/admin/testimonial/update/:id", "TestimonialStateData", testimonial, "Morgan", "Testimonial Name is Already Exist", UPDATE_TESTIMONIAL],
  ])("rejects an update that duplicates another record", async (element, route, pattern, stateKey, current, duplicateName, message, type) => {
    const spy = renderPage(element, route, pattern, {
      [stateKey]: [current, { ...current, id: 99, name: duplicateName }],
    });
    expect(await screen.findByDisplayValue(current.name)).toBeInTheDocument();
    spy.mockClear();
    fireEvent.change(screen.getAllByRole("textbox")[0], {
      target: { value: duplicateName },
    });
    fireEvent.click(screen.getByRole("button", { name: "Update" }));
    expect(screen.getByText(message)).toBeInTheDocument();
    expect(dispatched(spy, type)).toBeUndefined();
  });
});
