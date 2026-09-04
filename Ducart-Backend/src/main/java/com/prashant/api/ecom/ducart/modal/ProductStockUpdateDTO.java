package com.prashant.api.ecom.ducart.modal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockUpdateDTO {

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    /*
     * Optional:
     *
     * null  = automatically decide from stock quantity
     * true  = available when stock quantity is greater than 0
     * false = manually mark the product as unavailable
     */
    private Boolean stock;
}