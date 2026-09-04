import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import OrderProducts from "./OrderProducts";
import { formatCurrency } from "../config/siteConfig";

const orderProduct = {
  id: 10,
  name: "Premium Dog Food",
  brand: "Acme",
  color: "Brown",
  size: "2 kg",
  price: 24,
  qty: 2,
  total: 48,
  pic: "/dog-food.jpg",
};

describe("OrderProducts", () => {
  test("renders ordered product details and totals", () => {
    render(
      <MemoryRouter>
        <OrderProducts data={[orderProduct]} />
      </MemoryRouter>,
    );

    expect(screen.getByText("Premium Dog Food")).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "Acme" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: formatCurrency(48) })).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Premium Dog Food" })).toHaveAttribute(
      "src",
      `${process.env.REACT_APP_SERVER}/dog-food.jpg`,
    );
  });

  test("renders no product rows for missing order data", () => {
    render(
      <MemoryRouter>
        <OrderProducts />
      </MemoryRouter>,
    );

    expect(screen.queryByRole("cell", { name: "Premium Dog Food" })).not.toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Quantity" })).toBeInTheDocument();
  });
});
