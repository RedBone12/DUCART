package com.prashant.api.ecom.ducart.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String maincategory;
    private String subcategory;
    private String brand;
    private String color;
    private String size;
    private Double basePrice;
    private Double discount;
    private Double finalPrice;
    private boolean stock;

    @Lob
    private String description;

    private Integer stockQuantity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_pics", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "pics")
    private List<String> pics = new ArrayList<>();

    private boolean active;
}
