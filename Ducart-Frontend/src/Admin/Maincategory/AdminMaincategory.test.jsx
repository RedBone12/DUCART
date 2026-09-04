import { configureStore } from "@reduxjs/toolkit";
import { fireEvent, render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import AdminMaincategory from "./AdminMaincategory";
import AdminCreateMaincategory from "./AdminCreateMaincategory";
import AdminUpdateMaincategory from "./AdminUpdateMaincategory";
import {
  CREATE_MAINCATEGORY,
  DELETE_MAINCATEGORY,
  UPDATE_MAINCATEGORY,
} from "../../Redux/Constants";

jest.mock("jquery", () => () => ({ DataTable: jest.fn() }));
jest.mock("datatables.net", () => ({}));

const category = {
  id: 1,
  name: "Pets",
  pic: "/uploads/maincategories/pets.jpg",
  active: true,
};

function createStore(categories = [category]) {
  const initialState = { MaincategoryStateData: categories };
  return configureStore({
    reducer: (state = initialState) => state,
    preloadedState: initialState,
    middleware: () => [],
  });
}

function renderAdminPage(
  element,
  route,
  categories = [category],
  routePattern = route,
) {
  const store = createStore(categories);
  const dispatchSpy = jest.spyOn(store, "dispatch");

  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path={routePattern} element={element} />
          <Route
            path="/admin/maincategory"
            element={<div>Maincategory list page</div>}
          />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );

  return { dispatchSpy };
}

describe("Admin maincategory CRUD", () => {
  beforeEach(() => {
    URL.createObjectURL = jest.fn(() => "blob:preview");
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("lists categories with create and update destinations", () => {
    renderAdminPage(
      <AdminMaincategory />,
      "/admin/maincategory",
    );

    expect(screen.getByText("Pets")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Pets" })).toHaveAttribute(
      "src",
      `${process.env.REACT_APP_SERVER}/uploads/maincategories/pets.jpg`,
    );
    expect(
      screen.getByRole("link", { name: "Create maincategory" }),
    ).toHaveAttribute("href", "/admin/maincategory/create");
    expect(screen.getByRole("link", { name: "Edit Pets" })).toHaveAttribute(
      "href",
      "/admin/maincategory/update/1",
    );
  });

  test("deletes a category after confirmation", () => {
    jest.spyOn(window, "confirm").mockReturnValue(true);
    const { dispatchSpy } = renderAdminPage(
      <AdminMaincategory />,
      "/admin/maincategory",
    );
    dispatchSpy.mockClear();

    fireEvent.click(screen.getByRole("button", { name: "Delete Pets" }));

    expect(dispatchSpy).toHaveBeenCalledWith({
      type: DELETE_MAINCATEGORY,
      payload: { id: 1 },
    });
  });

  test("shows validation messages when required create fields are empty", () => {
    const { dispatchSpy } = renderAdminPage(
      <AdminCreateMaincategory />,
      "/admin/maincategory/create",
      [],
    );
    dispatchSpy.mockClear();

    fireEvent.click(screen.getByRole("button", { name: "Create" }));

    expect(screen.getByText("Name Field is Mandatory")).toBeInTheDocument();
    expect(screen.getByText("Pic Field is Mandatory")).toBeInTheDocument();
    expect(dispatchSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: CREATE_MAINCATEGORY }),
    );
  });

  test("blocks creation when a category name already exists", () => {
    const { dispatchSpy } = renderAdminPage(
      <AdminCreateMaincategory />,
      "/admin/maincategory/create",
    );
    dispatchSpy.mockClear();
    const picture = new File(["image"], "pets.png", { type: "image/png" });

    fireEvent.change(screen.getByLabelText("Maincategory name"), {
      target: { value: "pets" },
    });
    fireEvent.change(screen.getByLabelText("Maincategory picture"), {
      target: { files: [picture] },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create" }));

    expect(
      screen.getByText("Maincategory Name is Already Exist"),
    ).toBeInTheDocument();
    expect(dispatchSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: CREATE_MAINCATEGORY }),
    );
  });

  test("creates a valid category using multipart form data", async () => {
    const { dispatchSpy } = renderAdminPage(
      <AdminCreateMaincategory />,
      "/admin/maincategory/create",
      [],
    );
    dispatchSpy.mockClear();
    const picture = new File(["image"], "garden.png", { type: "image/png" });

    fireEvent.change(screen.getByLabelText("Maincategory name"), {
      target: { value: "Garden" },
    });
    fireEvent.change(screen.getByLabelText("Maincategory picture"), {
      target: { files: [picture] },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create" }));

    expect(await screen.findByText("Maincategory list page")).toBeInTheDocument();
    const createAction = dispatchSpy.mock.calls
      .map(([action]) => action)
      .find((action) => action.type === CREATE_MAINCATEGORY);
    expect(createAction.payload).toBeInstanceOf(FormData);
    expect(createAction.payload.get("pic")).toBe(picture);
  });

  test("updates the current category without treating its own name as duplicate", async () => {
    const { dispatchSpy } = renderAdminPage(
      <AdminUpdateMaincategory />,
      "/admin/maincategory/update/1",
      [category],
      "/admin/maincategory/update/:id",
    );
    await screen.findByDisplayValue("Pets");
    dispatchSpy.mockClear();

    fireEvent.change(screen.getByLabelText("Maincategory name"), {
      target: { value: "Pet Supplies" },
    });
    const replacement = new File(["image"], "pet-supplies.png", {
      type: "image/png",
    });
    fireEvent.change(screen.getByLabelText("Maincategory picture"), {
      target: { files: [replacement] },
    });
    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "0" },
    });

    fireEvent.click(screen.getByRole("button", { name: "Update" }));

    expect(await screen.findByText("Maincategory list page")).toBeInTheDocument();
    const updateAction = dispatchSpy.mock.calls
      .map(([action]) => action)
      .find((action) => action.type === UPDATE_MAINCATEGORY);
    expect(updateAction.payload).toBeInstanceOf(FormData);
    expect(updateAction.payload.get("id")).toBe("1");
    expect(updateAction.payload.get("pic")).toBe(replacement);
  });

  test("rejects an update that duplicates another category name", async () => {
    const categories = [category, { ...category, id: 2, name: "Garden" }];
    const { dispatchSpy } = renderAdminPage(
      <AdminUpdateMaincategory />,
      "/admin/maincategory/update/1",
      categories,
      "/admin/maincategory/update/:id",
    );
    await screen.findByDisplayValue("Pets");
    dispatchSpy.mockClear();
    fireEvent.change(screen.getByLabelText("Maincategory name"), {
      target: { value: "Garden" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Update" }));
    expect(
      screen.getByText("Maincategory Name already exists"),
    ).toBeInTheDocument();
    expect(dispatchSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: UPDATE_MAINCATEGORY }),
    );
  });
});
