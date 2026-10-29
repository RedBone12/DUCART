package com.prashant.api.ecom.ducart.modal;

import lombok.*;
import java.math.BigDecimal;
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
    private BigDecimal subtotal;
    private BigDecimal shipping;
    private BigDecimal total;
    private String date;
    private String rppid;
    private List<CheckoutLineDTO> products = new ArrayList<>();
}
