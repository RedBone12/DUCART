package com.prashant.api.ecom.ducart.entities;

import jakarta.persistence.*;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "newsletter")
public class Newsletter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // unique = true means the database also prevents duplicate emails.
    // nullable = false means email cannot be null in database.
    @Column(unique = true, nullable = false)
    private String email;

    private boolean active = true;
}
