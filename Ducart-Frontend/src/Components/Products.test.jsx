import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import Products from "./Products";

const products = [
  {
    id: 1,
    name: "Premium Dog Food",
    basePrice: 30,
    finalPrice: 24,
    discount: 20,
    pics: ["/uploads/products/dog-food.jpg"],
  },
  {
    id: 2,
    name: "Cat Toy",
    basePrice: 15,
    finalPrice: 12,
    discount: 20,
    pics: [],
  },
];

function renderProducts(props = {}) {
  return render(
    <MemoryRouter>
      <Products data={products} title="Shop" {...props} />
    </MemoryRouter>,
  );
}

describe("Products", () => {
  test("renders product cards, images, fallback content, and detail links", () => {
    renderProducts();

    expect(screen.getByText("Premium Dog Food")).toBeInTheDocument();
    expect(screen.getByText("Cat Toy")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Premium Dog Food" })).toHaveAttribute(
      "src",
      `${process.env.REACT_APP_SERVER}/uploads/products/dog-food.jpg`,
    );
    expect(screen.getByText("No Image")).toBeInTheDocument();

    const detailLinks = screen.getAllByRole("link", { name: "Add To Cart" });
    expect(detailLinks[0]).toHaveAttribute("href", "/product/1");
    expect(detailLinks[1]).toHaveAttribute("href", "/product/2");
  });

  test("renders a category heading and encoded explore link outside the shop page", () => {
    renderProducts({ title: "Dog Food" });

    expect(
      screen.getByRole("heading", {
        name: /Checkout Our Ducart Dog Food Products/i,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Explore More Products" }),
    ).toHaveAttribute("href", "/shop?mc=Dog%20Food&sc=All&br=All");
  });

  test("keeps an already absolute product image URL unchanged", () => {
    renderProducts({
      data: [{ ...products[0], pics: ["https://cdn.example.com/product.jpg"] }],
    });
    expect(screen.getByRole("img", { name: "Premium Dog Food" })).toHaveAttribute(
      "src",
      "https://cdn.example.com/product.jpg",
    );
  });
});
