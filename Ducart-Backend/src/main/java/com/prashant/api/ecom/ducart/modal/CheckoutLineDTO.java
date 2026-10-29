package com.prashant.api.ecom.ducart.modal;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutLineDTO {
    private Long id;
    private String user;
    private String product;
    private String name;
    private String brand;
    private String color;
    private String size;
    private BigDecimal price;
    private Integer stockQuantity;
    private String pic;
    private Integer qty;
    private BigDecimal total;
}
