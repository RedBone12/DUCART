package com.prashant.api.ecom.ducart.entities;

import jakarta.persistence.*;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cart")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String user;
    private String product;
    private String name;
    private String brand;
    private String color;
    private String size;
    private Double price;
    private Integer stockQuantity;
    private String pic;
    private Integer qty;
    private Double total;
}
