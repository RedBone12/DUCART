import imageValidators from "./imageValidators";

function validate(files) {
  return imageValidators({ target: { files } });
}

function image(name, type = "image/png", size = 10) {
  return new File([new Uint8Array(size)], name, { type });
}

describe("imageValidators", () => {
  test("requires at least one image", () => {
    expect(validate([])).toBe("Pic Field is Mandatory");
    expect(imageValidators({ target: {} })).toBe("Pic Field is Mandatory");
  });

  test.each(["image/jpeg", "image/jpg", "image/png", "image/gif"])(
    "accepts %s images",
    (type) => {
      expect(validate([image("valid-image", type)])).toBe("");
    },
  );

  test("rejects an image larger than one megabyte", () => {
    expect(validate([image("large.png", "image/png", 1048577)])).toContain(
      "more then 1 mb",
    );
  });

  test("rejects unsupported file types", () => {
    expect(validate([image("notes.txt", "text/plain")])).toContain(
      "Invalid Pic",
    );
  });

  test("returns every error from a multiple-image selection", () => {
    const result = validate([
      image("valid.png"),
      image("large.png", "image/png", 1048577),
      image("notes.txt", "text/plain"),
    ]);

    expect(result).toHaveLength(2);
    expect(result[0]).toContain("Pic 2 size");
    expect(result[1]).toContain("Invalid Pic3");
  });
});
