import * as brandActions from "./BrandActionCreators";
import * as cartActions from "./CartActionCreators";
import * as checkoutActions from "./CheckoutActionCreators";
import * as contactActions from "./ContactUsActionCreators";
import * as maincategoryActions from "./MaincategoryActionCreators";
import * as newsletterActions from "./NewsletterActionCreators";
import * as productActions from "./ProductActionCreators";
import * as subcategoryActions from "./SubcategoryActionCreators";
import * as testimonialActions from "./TestimonialActionCreators";
import * as wishlistActions from "./WishlistActionCreators";
import * as types from "../Constants";

const payload = { id: 7, name: "Demo" };

const actionCases = [
  [brandActions.createMutipartRecord, types.CREATE_BRAND, true],
  [brandActions.getBrand, types.GET_BRAND, false],
  [brandActions.updateMultipartRecord, types.UPDATE_BRAND, true],
  [brandActions.deleteBrand, types.DELETE_BRAND, true],
  [cartActions.createCart, types.CREATE_CART, true],
  [cartActions.getCart, types.GET_CART, false],
  [cartActions.updateCart, types.UPDATE_CART, true],
  [cartActions.deleteCart, types.DELETE_CART, true],
  [checkoutActions.createCheckout, types.CREATE_CHECKOUT, true],
  [checkoutActions.getCheckout, types.GET_CHECKOUT, false],
  [checkoutActions.updateCheckout, types.UPDATE_CHECKOUT, true],
  [checkoutActions.deleteCheckout, types.DELETE_CHECKOUT, true],
  [contactActions.createContactUs, types.CREATE_CONTACT_US, true],
  [contactActions.getContactUs, types.GET_CONTACT_US, false],
  [contactActions.updateContactUs, types.UPDATE_CONTACT_US, true],
  [contactActions.deleteContactUs, types.DELETE_CONTACT_US, true],
  [maincategoryActions.createMultipartRecord, types.CREATE_MAINCATEGORY, true],
  [maincategoryActions.getMaincategory, types.GET_MAINCATEGORY, false],
  [maincategoryActions.updateMultipartRecord, types.UPDATE_MAINCATEGORY, true],
  [maincategoryActions.deleteMaincategory, types.DELETE_MAINCATEGORY, true],
  [newsletterActions.createNewsletter, types.CREATE_NEWSLETTER, true],
  [newsletterActions.getNewsletter, types.GET_NEWSLETTER, false],
  [newsletterActions.updateNewsletter, types.UPDATE_NEWSLETTER, true],
  [newsletterActions.deleteNewsletter, types.DELETE_NEWSLETTER, true],
  [productActions.createMultipartRecord, types.CREATE_PRODUCT, true],
  [productActions.getProduct, types.GET_PRODUCT, false],
  [productActions.updateProduct, types.UPDATE_PRODUCT, true],
  [productActions.deleteProduct, types.DELETE_PRODUCT, true],
  [subcategoryActions.createMultipartRecord, types.CREATE_SUBCATEGORY, true],
  [subcategoryActions.getSubcategory, types.GET_SUBCATEGORY, false],
  [subcategoryActions.updateMultipartRecord, types.UPDATE_SUBCATEGORY, true],
  [subcategoryActions.deleteSubcategory, types.DELETE_SUBCATEGORY, true],
  [testimonialActions.createTestimonial, types.CREATE_TESTIMONIAL, true],
  [testimonialActions.getTestimonial, types.GET_TESTIMONIAL, false],
  [testimonialActions.updateTestimonial, types.UPDATE_TESTIMONIAL, true],
  [testimonialActions.deleteTestimonial, types.DELETE_TESTIMONIAL, true],
  [wishlistActions.createWishlist, types.CREATE_WISHLIST, true],
  [wishlistActions.getWishlist, types.GET_WISHLIST, false],
  [wishlistActions.deleteWishlist, types.DELETE_WISHLIST, true],
];

test.each(actionCases)(
  "%p creates the expected Redux action",
  (creator, type, hasPayload) => {
    expect(creator(payload)).toEqual(
      hasPayload ? { type, payload } : { type },
    );
  },
);
