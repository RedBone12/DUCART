package com.prashant.api.ecom.ducart.modal;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DtoValidationTest {

    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void signup_shouldAcceptInclusiveLengthAndPhoneBoundaries() {
        SignupDTO minimum = validSignup();
        minimum.setName("Ann");
        minimum.setUsername("bob");
        minimum.setPassword("123456");
        minimum.setPhone("1234567");
        assertTrue(validator.validate(minimum).isEmpty());

        SignupDTO maximum = validSignup();
        maximum.setName("123456789012345");
        maximum.setUsername("123456789012345");
        maximum.setPhone("+123456789012345");
        assertTrue(validator.validate(maximum).isEmpty());
    }

    @Test
    void signup_shouldRejectValuesImmediatelyOutsideBoundaries() {
        SignupDTO dto = validSignup();
        dto.setName("ab");
        dto.setUsername("1234567890123456");
        dto.setPassword("12345");
        dto.setPhone("123456");

        Set<ConstraintViolation<SignupDTO>> violations = validator.validate(dto);

        assertViolation(violations, "name", "Name must be between 3 and 15 characters");
        assertViolation(violations, "username", "Username must be between 3 and 15 characters");
        assertViolation(violations, "password", "Password must be at least 6 characters");
        assertViolation(violations, "phone", "Phone must contain 7 to 15 digits");
    }

    @Test
    void signupBlankName_shouldExposeBothViolationsForCompleteErrorResponse() {
        SignupDTO dto = validSignup();
        dto.setName("");

        Set<ConstraintViolation<SignupDTO>> violations = validator.validate(dto);

        assertViolation(violations, "name", "Name is required");
        assertViolation(violations, "name", "Name must be between 3 and 15 characters");
    }

    @Test
    void productNumericBoundaries_shouldAcceptZeroAndOneHundred() {
        ProductDTO dto = validProduct();
        dto.setBasePrice(0.0);
        dto.setDiscount(100.0);
        dto.setStockQuantity(0);

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void productNumericBoundaries_shouldRejectNegativeAndOverHundred() {
        ProductDTO dto = validProduct();
        dto.setBasePrice(-0.01);
        dto.setDiscount(100.01);
        dto.setStockQuantity(-1);

        Set<ConstraintViolation<ProductDTO>> violations = validator.validate(dto);

        assertViolation(violations, "basePrice", "Base price must be 0 or greater");
        assertViolation(violations, "discount", "Discount must not exceed 100");
        assertViolation(violations, "stockQuantity", "Stock quantity cannot be negative");
    }

    @Test
    void simpleDtos_shouldEnforceRequiredAndEmailFields() {
        BrandDTO brand = new BrandDTO();
        assertViolation(validator.validate(brand), "name", "Brand name is required");
        assertViolation(validator.validate(brand), "active", "Brand active status is required");

        NewsletterDTO newsletter = new NewsletterDTO("not-an-email", true);
        assertViolation(validator.validate(newsletter), "email", "Email format is invalid");

        ProductStockUpdateDTO stock = new ProductStockUpdateDTO();
        stock.setStockQuantity(-1);
        assertViolation(validator.validate(stock), "stockQuantity", "Stock quantity cannot be negative");
    }

    private SignupDTO validSignup() {
        return SignupDTO.builder()
                .name("Alice")
                .username("alice")
                .email("alice@example.com")
                .phone("1234567890")
                .password("secret1")
                .build();
    }

    private ProductDTO validProduct() {
        ProductDTO dto = new ProductDTO();
        dto.setName("Phone");
        dto.setMaincategory("Electronics");
        dto.setSubcategory("Mobiles");
        dto.setBrand("Apple");
        dto.setColor("Black");
        dto.setSize("128GB");
        dto.setBasePrice(100.0);
        dto.setDiscount(10.0);
        dto.setDescription("A phone");
        dto.setStockQuantity(5);
        return dto;
    }

    private <T> void assertViolation(Set<ConstraintViolation<T>> violations, String field, String message) {
        assertTrue(violations.stream().anyMatch(violation ->
                        violation.getPropertyPath().toString().equals(field)
                                && violation.getMessage().equals(message)),
                () -> "Expected validation error for " + field + ": " + message + ", actual: " + violations);
    }
}
