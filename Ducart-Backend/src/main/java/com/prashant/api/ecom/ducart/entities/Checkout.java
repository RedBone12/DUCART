package com.prashant.api.ecom.ducart.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "checkout", uniqueConstraints = @UniqueConstraint(
        name = "uk_checkout_user_request", columnNames = {"user", "idempotency_key"}))
public class Checkout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String user;
    private String orderStatus;
    private String paymentMode;
    private String paymentStatus;
    @Column(precision = 19, scale = 2)
    private BigDecimal subtotal;
    @Column(precision = 19, scale = 2)
    private BigDecimal shipping;
    @Column(precision = 19, scale = 2)
    private BigDecimal total;
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;
    @Column(length = 64)
    private String requestFingerprint;
    private String date;
    private String rppid;

    @Lob
    private String productsJson;
}
