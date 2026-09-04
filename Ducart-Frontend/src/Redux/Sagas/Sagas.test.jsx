import { put, takeEvery } from "redux-saga/effects";

import * as services from "./Services";
import * as brand from "./BrandSagas";
import * as cart from "./CartSagas";
import * as checkout from "./CheckoutSagas";
import * as contact from "./ContactUsSagas";
import * as maincategory from "./MaincategorySagas";
import * as newsletter from "./NewsletterSagas";
import * as product from "./ProductSagas";
import RootSaga from "./RootSaga";
import * as subcategory from "./SubcategorySagas";
import * as testimonial from "./TestimonialSagas";
import * as wishlist from "./WishlistSagas";
import * as types from "../Constants";

jest.mock("./Services", () => ({
  createRecord: jest.fn(),
  createMultipartRecord: jest.fn(),
  getRecord: jest.fn(),
  updateRecord: jest.fn(),
  updateMultipartRecord: jest.fn(),
  deleteRecord: jest.fn(),
}));

const payload = { id: 5, name: "Demo" };
const response = { id: 5, name: "Saved" };

function expectResponseWorker(generator, requestEffect, type) {
  expect(generator.next().value).toBe(requestEffect);
  expect(generator.next(response).value).toEqual(put({ type, payload: response }));
  expect(generator.next().done).toBe(true);
}

function expectPayloadWorker(generator, requestEffect, type) {
  expect(generator.next().value).toBe(requestEffect);
  expect(generator.next().value).toEqual(put({ type, payload }));
  expect(generator.next().done).toBe(true);
}

const jsonSagaCases = [
  [
    "cart",
    cart,
    "cart",
    "cart/me",
    types.CREATE_CART_RED,
    types.GET_CART_RED,
    types.UPDATE_CART_RED,
    types.DELETE_CART_RED,
  ],
  [
    "checkout",
    checkout,
    "checkout",
    "checkout/me",
    types.CREATE_CHECKOUT_RED,
    types.GET_CHECKOUT_RED,
    types.UPDATE_CHECKOUT_RED,
    types.DELETE_CHECKOUT_RED,
  ],
  [
    "contact us",
    contact,
    "contactus",
    "contactus",
    types.CREATE_CONTACT_US_RED,
    types.GET_CONTACT_US_RED,
    types.UPDATE_CONTACT_US_RED,
    types.DELETE_CONTACT_US_RED,
  ],
  [
    "newsletter",
    newsletter,
    "newsletter",
    "newsletter",
    types.CREATE_NEWSLETTER_RED,
    types.GET_NEWSLETTER_RED,
    types.UPDATE_NEWSLETTER_RED,
    types.DELETE_NEWSLETTER_RED,
  ],
];

describe.each(jsonSagaCases)(
  "%s saga workers",
  (_name, saga, collection, getCollection, createRed, getRed, updateRed, deleteRed) => {
    beforeEach(() => jest.clearAllMocks());

    test("creates and loads records", () => {
      services.createRecord.mockReturnValue("create request");
      expectResponseWorker(
        saga.createSaga({ payload }),
        "create request",
        createRed,
      );
      expect(services.createRecord).toHaveBeenCalledWith(collection, payload);

      services.getRecord.mockReturnValue("get request");
      expectResponseWorker(saga.getSaga(), "get request", getRed);
      expect(services.getRecord).toHaveBeenCalledWith(getCollection);
    });

    test("updates and deletes records", () => {
      services.updateRecord.mockReturnValue("update request");
      expectPayloadWorker(
        saga.updateSaga({ payload }),
        "update request",
        updateRed,
      );
      expect(services.updateRecord).toHaveBeenCalledWith(collection, payload);

      services.deleteRecord.mockReturnValue("delete request");
      expectPayloadWorker(
        saga.deleteSaga({ payload }),
        "delete request",
        deleteRed,
      );
      expect(services.deleteRecord).toHaveBeenCalledWith(collection, payload);
    });
  },
);

const multipartSagaCases = [
  [
    "brand",
    brand,
    "brand",
    types.CREATE_BRAND_RED,
    types.GET_BRAND_RED,
    types.UPDATE_BRAND_RED,
    types.DELETE_BRAND_RED,
  ],
  [
    "main category",
    maincategory,
    "maincategory",
    types.CREATE_MAINCATEGORY_RED,
    types.GET_MAINCATEGORY_RED,
    types.UPDATE_MAINCATEGORY_RED,
    types.DELETE_MAINCATEGORY_RED,
  ],
  [
    "subcategory",
    subcategory,
    "subcategory",
    types.CREATE_SUBCATEGORY_RED,
    types.GET_SUBCATEGORY_RED,
    types.UPDATE_SUBCATEGORY_RED,
    types.DELETE_SUBCATEGORY_RED,
  ],
];

describe.each(multipartSagaCases)(
  "%s multipart saga workers",
  (_name, saga, collection, createRed, getRed, updateRed, deleteRed) => {
    beforeEach(() => jest.clearAllMocks());

    test("creates, gets, updates, and deletes records", () => {
      services.createMultipartRecord.mockReturnValue("create multipart request");
      expectResponseWorker(
        saga.createSaga({ payload }),
        "create multipart request",
        createRed,
      );

      services.getRecord.mockReturnValue("get request");
      expectResponseWorker(saga.getSaga(), "get request", getRed);

      services.updateMultipartRecord.mockReturnValue("update multipart request");
      expectResponseWorker(
        saga.updateSaga({ payload }),
        "update multipart request",
        updateRed,
      );

      services.deleteRecord.mockReturnValue("delete request");
      expectPayloadWorker(
        saga.deleteSaga({ payload }),
        "delete request",
        deleteRed,
      );

      expect(services.createMultipartRecord).toHaveBeenCalledWith(collection, payload);
      expect(services.getRecord).toHaveBeenCalledWith(collection);
      expect(services.updateMultipartRecord).toHaveBeenCalledWith(collection, payload);
      expect(services.deleteRecord).toHaveBeenCalledWith(collection, payload);
    });
  },
);

describe("product saga workers", () => {
  beforeEach(() => jest.clearAllMocks());

  test("creates, gets, and deletes products", () => {
    services.createMultipartRecord.mockReturnValue("create request");
    expectResponseWorker(
      product.createSaga({ payload }),
      "create request",
      types.CREATE_PRODUCT_RED,
    );
    services.getRecord.mockReturnValue("get request");
    expectResponseWorker(product.getSaga(), "get request", types.GET_PRODUCT_RED);
    services.deleteRecord.mockReturnValue("delete request");
    expectPayloadWorker(
      product.deleteSaga({ payload }),
      "delete request",
      types.DELETE_PRODUCT_RED,
    );
  });

  test("updates JSON and multipart products", () => {
    services.updateRecord.mockReturnValue("json update");
    expectResponseWorker(
      product.updateSaga({ payload }),
      "json update",
      types.UPDATE_PRODUCT_RED,
    );

    const formData = new FormData();
    services.updateMultipartRecord.mockReturnValue("multipart update");
    expectResponseWorker(
      product.updateSaga({ payload: formData }),
      "multipart update",
      types.UPDATE_PRODUCT_RED,
    );
  });
});

describe("testimonial saga workers", () => {
  beforeEach(() => jest.clearAllMocks());

  test("creates and updates JSON or multipart testimonials", () => {
    services.createRecord.mockReturnValue("json create");
    expectResponseWorker(
      testimonial.createSaga({ payload }),
      "json create",
      types.CREATE_TESTIMONIAL_RED,
    );
    services.updateRecord.mockReturnValue("json update");
    expectResponseWorker(
      testimonial.updateSaga({ payload }),
      "json update",
      types.UPDATE_TESTIMONIAL_RED,
    );

    const formData = new FormData();
    services.createMultipartRecord.mockReturnValue("multipart create");
    expectResponseWorker(
      testimonial.createSaga({ payload: formData }),
      "multipart create",
      types.CREATE_TESTIMONIAL_RED,
    );
    services.updateMultipartRecord.mockReturnValue("multipart update");
    expectResponseWorker(
      testimonial.updateSaga({ payload: formData }),
      "multipart update",
      types.UPDATE_TESTIMONIAL_RED,
    );
  });

  test("gets and deletes testimonials", () => {
    services.getRecord.mockReturnValue("get request");
    expectResponseWorker(
      testimonial.getSaga(),
      "get request",
      types.GET_TESTIMONIAL_RED,
    );
    services.deleteRecord.mockReturnValue("delete request");
    expectPayloadWorker(
      testimonial.deleteSaga({ payload }),
      "delete request",
      types.DELETE_TESTIMONIAL_RED,
    );
  });
});

test("wishlist saga creates, loads, and deletes the current user's items", () => {
  services.createRecord.mockReturnValue("create request");
  expectResponseWorker(
    wishlist.createSaga({ payload }),
    "create request",
    types.CREATE_WISHLIST_RED,
  );
  services.getRecord.mockReturnValue("get request");
  expectResponseWorker(wishlist.getSaga(), "get request", types.GET_WISHLIST_RED);
  expect(services.getRecord).toHaveBeenCalledWith("wishlist/me");
  services.deleteRecord.mockReturnValue("delete request");
  expectPayloadWorker(
    wishlist.deleteSaga({ payload }),
    "delete request",
    types.DELETE_WISHLIST_RED,
  );
});

const watcherCases = [
  [brand, [[types.CREATE_BRAND, brand.createSaga], [types.GET_BRAND, brand.getSaga], [types.UPDATE_BRAND, brand.updateSaga], [types.DELETE_BRAND, brand.deleteSaga]]],
  [cart, [[types.CREATE_CART, cart.createSaga], [types.GET_CART, cart.getSaga], [types.UPDATE_CART, cart.updateSaga], [types.DELETE_CART, cart.deleteSaga]]],
  [checkout, [[types.CREATE_CHECKOUT, checkout.createSaga], [types.GET_CHECKOUT, checkout.getSaga], [types.UPDATE_CHECKOUT, checkout.updateSaga], [types.DELETE_CHECKOUT, checkout.deleteSaga]]],
  [contact, [[types.CREATE_CONTACT_US, contact.createSaga], [types.GET_CONTACT_US, contact.getSaga], [types.UPDATE_CONTACT_US, contact.updateSaga], [types.DELETE_CONTACT_US, contact.deleteSaga]]],
  [maincategory, [[types.CREATE_MAINCATEGORY, maincategory.createSaga], [types.GET_MAINCATEGORY, maincategory.getSaga], [types.UPDATE_MAINCATEGORY, maincategory.updateSaga], [types.DELETE_MAINCATEGORY, maincategory.deleteSaga]]],
  [newsletter, [[types.CREATE_NEWSLETTER, newsletter.createSaga], [types.GET_NEWSLETTER, newsletter.getSaga], [types.UPDATE_NEWSLETTER, newsletter.updateSaga], [types.DELETE_NEWSLETTER, newsletter.deleteSaga]]],
  [product, [[types.CREATE_PRODUCT, product.createSaga], [types.GET_PRODUCT, product.getSaga], [types.UPDATE_PRODUCT, product.updateSaga], [types.DELETE_PRODUCT, product.deleteSaga]]],
  [subcategory, [[types.CREATE_SUBCATEGORY, subcategory.createSaga], [types.GET_SUBCATEGORY, subcategory.getSaga], [types.UPDATE_SUBCATEGORY, subcategory.updateSaga], [types.DELETE_SUBCATEGORY, subcategory.deleteSaga]]],
  [testimonial, [[types.CREATE_TESTIMONIAL, testimonial.createSaga], [types.GET_TESTIMONIAL, testimonial.getSaga], [types.UPDATE_TESTIMONIAL, testimonial.updateSaga], [types.DELETE_TESTIMONIAL, testimonial.deleteSaga]]],
  [wishlist, [[types.CREATE_WISHLIST, wishlist.createSaga], [types.GET_WISHLIST, wishlist.getSaga], [types.DELETE_WISHLIST, wishlist.deleteSaga]]],
];

test.each(watcherCases)("registers every watcher in a saga", (saga, watchers) => {
  const generator = saga.default();
  watchers.forEach(([type, worker]) => {
    expect(generator.next().value).toEqual(takeEvery(type, worker));
  });
  expect(generator.next().done).toBe(true);
});

test("root saga combines all domain sagas", () => {
  const generator = RootSaga();
  const effect = generator.next().value;

  expect(effect.type).toBe("ALL");
  expect(effect.payload).toHaveLength(10);
  expect(generator.next().done).toBe(true);
});

test.each([
  [brand.createSaga, services.createMultipartRecord, "Error creating brand:"],
  [brand.updateSaga, services.updateMultipartRecord, "Error updating brand:"],
  [
    maincategory.createSaga,
    services.createMultipartRecord,
    "Error creating maincategory:",
  ],
  [
    maincategory.updateSaga,
    services.updateMultipartRecord,
    "Error updating maincategory:",
  ],
  [
    subcategory.updateSaga,
    services.updateMultipartRecord,
    "Error updating subcategory:",
  ],
])("handles a multipart saga service failure", (worker, service, message) => {
  const error = new Error("Service unavailable");
  service.mockImplementation(() => {
    throw error;
  });
  const consoleSpy = jest.spyOn(console, "error").mockImplementation(() => {});

  expect(worker({ payload }).next().done).toBe(true);
  expect(consoleSpy).toHaveBeenCalledWith(message, error);

  consoleSpy.mockRestore();
});
