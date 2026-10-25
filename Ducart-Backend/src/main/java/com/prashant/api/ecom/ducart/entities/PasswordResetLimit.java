package com.prashant.api.ecom.ducart.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_limits", indexes = @Index(name = "idx_reset_limit_window", columnList = "window_start"))
@Getter @Setter @NoArgsConstructor
public class PasswordResetLimit {
    @Id @Column(length = 64)
    private String bucketKey;
    @Column(name = "window_start", nullable = false)
    private Instant windowStart;
    @Column(nullable = false)
    private int attempts;
}
