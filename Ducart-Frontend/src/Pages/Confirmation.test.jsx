import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import Confirmation from "./Confirmation";

describe("Confirmation", () => {
  test("shows order confirmation and a link back to shopping", () => {
    render(
      <MemoryRouter>
        <Confirmation />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "Thank You" })).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Your Order has Been Placed" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Shop More" })).toHaveAttribute(
      "href",
      "/shop",
    );
  });
});
