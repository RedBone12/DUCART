package com.prashant.api.ecom.ducart.modal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDTO { // For input data
  @NotBlank(message = "Product name is required")
  private String name;

  @NotBlank(message = "maincategory name is required")
  private String maincategory;

  @NotBlank(message = "subcategory name is required")
  private String subcategory;

  @NotBlank(message = "brand name is required")
  private String brand;

  @NotBlank(message = "color name is required")
  private String color;

  @NotBlank(message = "size is required")
  private String size;

  @NotNull(message = "Base price is required")
  @PositiveOrZero(message = "Base price must be 0 or greater")
  private Double basePrice;

  @NotNull(message = "Discount is required")
  @DecimalMin(value = "0.0", inclusive = true, message = "Discount must be 0 or greater")
  @DecimalMax(value = "100.0", inclusive = true, message = "Discount must not exceed 100")
  private Double discount;

  // @NotBlank(message = "final price is required")
  // private Double finalPrice;

  private Boolean stock;

  @NotBlank(message = "description is required")
  private String description;

  @NotNull(message = "Stock quantity is required")
  @PositiveOrZero(message = "Stock quantity cannot be negative")
  private Integer stockQuantity;

  // @NotNull(message = "Product images are required")
  // private List<String> pics;

  private Boolean active;
}
