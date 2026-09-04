package com.prashant.api.ecom.ducart.entities;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "checkout")
public class Checkout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Lob
    private String productsJson;
}
