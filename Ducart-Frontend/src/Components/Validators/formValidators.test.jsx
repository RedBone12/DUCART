import formValidators from "./formValidators";

function validate(name, value) {
  return formValidators({ target: { name, value } });
}

describe("formValidators", () => {
  test.each([
    ["name", "", "name Field is Mandatory"],
    ["name", "Al", "name Field Length must be within 3-50 characters"],
    ["name", "Alice", ""],
    ["username", "buyer", ""],
  ])("validates %s value %j", (name, value, expected) => {
    expect(validate(name, value)).toBe(expected);
  });

  test.each([
    ["", "email Field is Mandatory"],
    ["not-an-email", "Invalid Email Address"],
    ["buyer@example.com", ""],
  ])("validates email %j", (value, expected) => {
    expect(validate("email", value)).toBe(expected);
  });

  test.each([
    ["weak", true],
    ["Password123", true],
    ["StrongPass1", false],
  ])("validates password strength for %j", (value, hasError) => {
    expect(Boolean(validate("password", value))).toBe(hasError);
  });

  test.each([
    ["+353851714732", ""],
    ["123", "Invalid Phone Number"],
    ["12-345678", "Invalid Phone Number"],
  ])("validates phone %j", (value, expected) => {
    expect(validate("phone", value)).toBe(expected);
  });

  test.each([
    ["basePrice", "19.99", ""],
    ["basePrice", "0", "Price Must be a Value Greater than 0"],
    ["basePrice", "abc", "Price Must be a Value Greater than 0"],
    ["discount", "100", ""],
    ["discount", "101", "Discount Field Must Be 0-100"],
    ["stockQuantity", "0", ""],
    [
      "stockQuantity",
      "1.5",
      "Stock Quantity Must be a Non-negative Integer",
    ],
  ])("validates numeric field %s with %j", (name, value, expected) => {
    expect(validate(name, value)).toBe(expected);
  });

  test("validates message length boundaries", () => {
    expect(validate("message", "Too short")).toBe(
      "message Field Length must be within 50-2000 characters",
    );
    expect(validate("message", "x".repeat(50))).toBe("");
    expect(validate("message", "x".repeat(2001))).toBe(
      "message Field Length must be within 50-2000 characters",
    );
  });

  test("ignores fields without a validation rule", () => {
    expect(validate("description", "anything")).toBe("");
  });

  test.each([
    ["subject", "", "subject Field is Mandatory"],
    ["subject", "short", "subject Field Length must be within 10-200 characters"],
    ["subject", "A valid subject line", ""],
    ["password", "", "password Field is Mandatory"],
    ["phone", "", "phone Field is Mandatory"],
    ["size", "", "size Field is Mandatory"],
    ["size", "x".repeat(51), "size Field Length must be less than 50 characters"],
    ["size", "Medium", ""],
    ["basePrice", "", "basePrice Field is Mandatory"],
    ["discount", "", "discount Field is Mandatory"],
    ["discount", "-1", "Discount Field Must Be 0-100"],
    ["stockQuantity", "", "stockQuantity Field is Mandatory"],
    ["stockQuantity", "-1", "Stock Quantity Must be a Non-negative Integer"],
    ["message", "", "message Field is Mandatory"],
  ])("covers the remaining %s boundary for %j", (name, value, expected) => {
    expect(validate(name, value)).toBe(expected);
  });
});
