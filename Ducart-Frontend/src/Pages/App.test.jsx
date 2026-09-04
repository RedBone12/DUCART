import { render, screen } from "@testing-library/react";

import App from "./App";

jest.mock("../Components/Navbar", () => () => <nav>Navbar shell</nav>);
jest.mock("../Components/Footer", () => () => <footer>Footer shell</footer>);
jest.mock("../Components/ProtectedRoute", () => {
  const { Outlet } = jest.requireActual("react-router-dom");
  return () => <Outlet />;
});

jest.mock("./Home", () => () => <div>Home page</div>);
jest.mock("./AboutUsPage", () => () => <div>About page</div>);
jest.mock("./ShopPage", () => () => <div>Shop page</div>);
jest.mock("./SingleProductPage", () => () => <div>Product detail page</div>);
jest.mock("./TestimonialPage", () => () => <div>Testimonials page</div>);
jest.mock("./ContactUsPage", () => () => <div>Contact page</div>);
jest.mock("./Signup", () => () => <div>Signup page</div>);
jest.mock("./Login", () => () => <div>Login page</div>);
jest.mock("./ForgotPassword", () => () => <div>Forgot password page</div>);
jest.mock("./ProfilePage", () => () => <div>Profile page</div>);
jest.mock("./UpdateProfile", () => () => <div>Update profile page</div>);
jest.mock("./CartPage", () => () => <div>Cart page</div>);
jest.mock("./CheckoutPage", () => () => <div>Checkout page</div>);
jest.mock("./Confirmation", () => () => <div>Confirmation page</div>);
jest.mock("./ErrorPage", () => () => <div>Error page</div>);

jest.mock("../Admin/Home/AdminHome", () => () => <div>Admin home page</div>);
jest.mock("../Admin/Maincategory/AdminMaincategory", () => () => (
  <div>Main category list</div>
));
jest.mock("../Admin/Maincategory/AdminCreateMaincategory", () => () => (
  <div>Main category create</div>
));
jest.mock("../Admin/Maincategory/AdminUpdateMaincategory", () => () => (
  <div>Main category update</div>
));
jest.mock("../Admin/Subcategory/AdminSubcategory", () => () => (
  <div>Subcategory list</div>
));
jest.mock("../Admin/Subcategory/AdminCreateSubcategory", () => () => (
  <div>Subcategory create</div>
));
jest.mock("../Admin/Subcategory/AdminUpdateSubcategory", () => () => (
  <div>Subcategory update</div>
));
jest.mock("../Admin/Brand/AdminBrand", () => () => <div>Brand list</div>);
jest.mock("../Admin/Brand/AdminCreateBrand", () => () => <div>Brand create</div>);
jest.mock("../Admin/Brand/AdminUpdateBrand", () => () => <div>Brand update</div>);
jest.mock("../Admin/Testimonial/AdminTestimonial", () => () => (
  <div>Testimonial list</div>
));
jest.mock("../Admin/Testimonial/AdminCreateTestimonial", () => () => (
  <div>Testimonial create</div>
));
jest.mock("../Admin/Testimonial/AdminUpdateTestimonial", () => () => (
  <div>Testimonial update</div>
));
jest.mock("../Admin/Product/AdminProduct", () => () => <div>Product list</div>);
jest.mock("../Admin/Product/AdminCreateProduct", () => () => (
  <div>Product create</div>
));
jest.mock("../Admin/Product/AdminUpdateProduct", () => () => (
  <div>Product update</div>
));
jest.mock("../Admin/Newsletter/AdminNewsletter", () => () => (
  <div>Newsletter list</div>
));
jest.mock("../Admin/User/AdminUser", () => () => <div>User list</div>);
jest.mock("../Admin/ContactUs/AdminContactUs", () => () => (
  <div>Admin contact list</div>
));
jest.mock("../Admin/ContactUs/AdminContactUsShow", () => () => (
  <div>Admin contact detail</div>
));
jest.mock("../Admin/Checkout/AdminCheckout", () => () => (
  <div>Admin checkout list</div>
));
jest.mock("../Admin/Checkout/AdminCheckoutShow", () => () => (
  <div>Admin checkout detail</div>
));

function renderPath(path) {
  window.history.pushState({}, "", path);
  return render(<App />);
}

describe("App route table", () => {
  test("renders a public route inside the shared layout", () => {
    renderPath("/signup");

    expect(screen.getByText("Signup page")).toBeInTheDocument();
    expect(screen.getByText("Navbar shell")).toBeInTheDocument();
    expect(screen.getByText("Footer shell")).toBeInTheDocument();
  });

  test("maps buyer profile update to the correct page", () => {
    renderPath("/update-profile");

    expect(screen.getByText("Update profile page")).toBeInTheDocument();
  });

  test("matches a dynamic admin update route", () => {
    renderPath("/admin/product/update/44");

    expect(screen.getByText("Product update")).toBeInTheDocument();
  });

  test("renders the error page for an unknown route", () => {
    renderPath("/not-a-real-route");

    expect(screen.getByText("Error page")).toBeInTheDocument();
  });
});
