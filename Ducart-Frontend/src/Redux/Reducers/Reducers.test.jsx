import BrandReducer from "./BrandReducer";
import CartReducer from "./CartReducer";
import CheckoutReducer from "./CheckoutReducer";
import ContactUsReducer from "./ContactUsReducer";
import MaincategoryReducer from "./MaincategoryReducer";
import NewsletterReducer from "./NewsletterReducer";
import ProductReducer from "./ProductReducer";
import RootReducer from "./RootReducer";
import SubcategoryReducer from "./SubcategoryReducer";
import TestimonialReducer from "./TestimonialReducer";
import WishlistReducer from "./WishlistReducer";
import {
  CREATE_BRAND_RED,
  CREATE_CART_RED,
  CREATE_CHECKOUT_RED,
  CREATE_CONTACT_US_RED,
  CREATE_MAINCATEGORY_RED,
  CREATE_NEWSLETTER_RED,
  CREATE_PRODUCT_RED,
  CREATE_SUBCATEGORY_RED,
  CREATE_TESTIMONIAL_RED,
  CREATE_WISHLIST_RED,
  DELETE_BRAND_RED,
  DELETE_CART_RED,
  DELETE_CHECKOUT_RED,
  DELETE_CONTACT_US_RED,
  DELETE_MAINCATEGORY_RED,
  DELETE_NEWSLETTER_RED,
  DELETE_PRODUCT_RED,
  DELETE_SUBCATEGORY_RED,
  DELETE_TESTIMONIAL_RED,
  DELETE_WISHLIST_RED,
  GET_BRAND_RED,
  GET_CART_RED,
  GET_CHECKOUT_RED,
  GET_CONTACT_US_RED,
  GET_MAINCATEGORY_RED,
  GET_NEWSLETTER_RED,
  GET_PRODUCT_RED,
  GET_SUBCATEGORY_RED,
  GET_TESTIMONIAL_RED,
  GET_WISHLIST_RED,
  UPDATE_BRAND_RED,
  UPDATE_CART_RED,
  UPDATE_CHECKOUT_RED,
  UPDATE_CONTACT_US_RED,
  UPDATE_MAINCATEGORY_RED,
  UPDATE_NEWSLETTER_RED,
  UPDATE_PRODUCT_RED,
  UPDATE_SUBCATEGORY_RED,
  UPDATE_TESTIMONIAL_RED,
} from "../Constants";

const reducerCases = [
  ["brand", BrandReducer, CREATE_BRAND_RED, GET_BRAND_RED, UPDATE_BRAND_RED, DELETE_BRAND_RED],
  ["cart", CartReducer, CREATE_CART_RED, GET_CART_RED, UPDATE_CART_RED, DELETE_CART_RED],
  [
    "checkout",
    CheckoutReducer,
    CREATE_CHECKOUT_RED,
    GET_CHECKOUT_RED,
    UPDATE_CHECKOUT_RED,
    DELETE_CHECKOUT_RED,
  ],
  [
    "contact us",
    ContactUsReducer,
    CREATE_CONTACT_US_RED,
    GET_CONTACT_US_RED,
    UPDATE_CONTACT_US_RED,
    DELETE_CONTACT_US_RED,
  ],
  [
    "main category",
    MaincategoryReducer,
    CREATE_MAINCATEGORY_RED,
    GET_MAINCATEGORY_RED,
    UPDATE_MAINCATEGORY_RED,
    DELETE_MAINCATEGORY_RED,
  ],
  [
    "newsletter",
    NewsletterReducer,
    CREATE_NEWSLETTER_RED,
    GET_NEWSLETTER_RED,
    UPDATE_NEWSLETTER_RED,
    DELETE_NEWSLETTER_RED,
  ],
  [
    "product",
    ProductReducer,
    CREATE_PRODUCT_RED,
    GET_PRODUCT_RED,
    UPDATE_PRODUCT_RED,
    DELETE_PRODUCT_RED,
  ],
  [
    "subcategory",
    SubcategoryReducer,
    CREATE_SUBCATEGORY_RED,
    GET_SUBCATEGORY_RED,
    UPDATE_SUBCATEGORY_RED,
    DELETE_SUBCATEGORY_RED,
  ],
  [
    "testimonial",
    TestimonialReducer,
    CREATE_TESTIMONIAL_RED,
    GET_TESTIMONIAL_RED,
    UPDATE_TESTIMONIAL_RED,
    DELETE_TESTIMONIAL_RED,
  ],
];

describe.each(reducerCases)(
  "%s reducer",
  (_name, reducer, createType, getType, updateType, deleteType) => {
    const first = { id: 1, name: "First", active: true };
    const second = { id: 2, name: "Second", active: true };

    test("returns its initial state and preserves unknown actions", () => {
      expect(reducer(undefined, { type: "@@INIT" })).toEqual([]);
      const current = [first];
      expect(reducer(current, { type: "UNKNOWN" })).toBe(current);
    });

    test("adds a newly created item", () => {
      expect(reducer([first], { type: createType, payload: second })).toEqual([
        first,
        second,
      ]);
    });

    test("replaces state on a valid get and rejects a malformed payload", () => {
      expect(reducer([], { type: getType, payload: [first, second] })).toEqual([
        first,
        second,
      ]);
      expect(reducer([first], { type: getType, payload: null })).toEqual([]);
    });

    test("updates only the item with the matching id", () => {
      expect(
        reducer([first, second], {
          type: updateType,
          payload: { id: 1, name: "Updated" },
        }),
      ).toEqual([{ id: 1, name: "Updated", active: true }, second]);
    });

    test("deletes only the item with the matching id", () => {
      expect(
        reducer([first, second], { type: deleteType, payload: { id: 1 } }),
      ).toEqual([second]);
    });
  },
);

describe("wishlist reducer", () => {
  const first = { id: 1, name: "First" };
  const second = { id: 2, name: "Second" };

  test("initializes and preserves state for an unknown action", () => {
    expect(WishlistReducer(undefined, { type: "@@INIT" })).toEqual([]);
    const state = [first];
    expect(WishlistReducer(state, { type: "UNKNOWN" })).toBe(state);
  });

  test("creates and loads wishlist items", () => {
    expect(
      WishlistReducer([first], { type: CREATE_WISHLIST_RED, payload: second }),
    ).toEqual([first, second]);
    expect(
      WishlistReducer([], { type: GET_WISHLIST_RED, payload: [first, second] }),
    ).toEqual([first, second]);
  });

  test("deletes the selected wishlist item", () => {
    expect(
      WishlistReducer([first, second], {
        type: DELETE_WISHLIST_RED,
        payload: { id: 1 },
      }),
    ).toEqual([second]);
  });
});

test("root reducer exposes every application state slice", () => {
  expect(Object.keys(RootReducer(undefined, { type: "@@INIT" }))).toEqual([
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
});
