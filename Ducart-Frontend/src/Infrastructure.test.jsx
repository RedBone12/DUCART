import Store from "./Redux/Store";
import RootReducer from "./Redux/Reducers/RootReducer";

describe("application bootstrap and Redux store", () => {
  test("creates every reducer slice in the production store", () => {
    expect(Object.keys(Store.getState())).toEqual([
      "MaincategoryStateData",
      "SubcategoryStateData",
      "BrandStateData",
      "ProductStateData",
      "TestimonialStateData",
      "CartStateData",
      "WishlistStateData",
      "CheckoutStateData",
      "NewsletterStateData",
      "ContactUsStateData",
    ]);
    expect(RootReducer(undefined, { type: "@@test/INIT" })).toEqual(Store.getState());
  });

  test("mounts the application into the root element", () => {
    const render = jest.fn();
    const createRoot = jest.fn(() => ({ render }));
    jest.isolateModules(() => {
      jest.doMock("react-dom/client", () => ({ createRoot }));
      jest.doMock("./Pages/App", () => () => <div>Application</div>);
      require("./index");
    });
    expect(createRoot).toHaveBeenCalledWith(document.getElementById("root"));
    expect(render).toHaveBeenCalledTimes(1);
  });
});
