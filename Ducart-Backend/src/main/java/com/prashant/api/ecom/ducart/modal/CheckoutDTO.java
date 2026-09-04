package com.prashant.api.ecom.ducart.modal;

import com.prashant.api.ecom.ducart.entities.Cart;
import lombok.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutDTO {
    private Long id;
    private String user;
    private String orderStatus;
    private String paymentMode;
    private String paymentStatus;
    private Double subtotal;
    private Double shipping;
    private Double total;
    private String date;
    private String rppid;
    private List<Cart> products = new ArrayList<>();
}
