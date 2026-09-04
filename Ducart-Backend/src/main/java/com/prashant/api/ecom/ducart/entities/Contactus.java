package com.prashant.api.ecom.ducart.entities;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contactus")
public class Contactus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String subject;

    @Lob
    private String message;

    private LocalDate date;
    private boolean active;
}
