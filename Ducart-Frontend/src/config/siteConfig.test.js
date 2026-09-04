import siteConfig, { formatCurrency } from "./siteConfig";

describe("siteConfig utilities", () => {
  test("formats values as Irish euro currency", () => {
    expect(formatCurrency(1234.5)).toBe("€1,234.50");
    expect(formatCurrency("24")).toBe("€24.00");
  });

  test("formats invalid or missing values as zero", () => {
    expect(formatCurrency(undefined)).toBe("€0.00");
    expect(formatCurrency("not-a-number")).toBe("€0.00");
  });

  test("provides checkout shipping and payment configuration", () => {
    expect(siteConfig.shippingFee).toBe(4.99);
    expect(siteConfig.freeShippingThreshold).toBe(100);
    expect(siteConfig.paymentModes.map((mode) => mode.value)).toEqual([
      "COD",
      "Card Demo",
      "PayPal Demo",
      "Bank Transfer",
    ]);
  });
});
