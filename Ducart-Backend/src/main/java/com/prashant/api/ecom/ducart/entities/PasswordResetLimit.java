package com.prashant.api.ecom.ducart.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_limits", indexes = @Index(name = "idx_reset_limit_window", columnList = "window_start"))
@Getter @Setter @NoArgsConstructor
public class PasswordResetLimit implements org.springframework.data.domain.Persistable<String> {
    // Assigned IDs must insert new buckets, never merge over another request's counter.
    @Transient @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private boolean newEntity = true;

    @Id @Column(length = 64)
    private String bucketKey;
    @Column(name = "window_start", nullable = false)
    private Instant windowStart;
    @Column(nullable = false)
    private int attempts;

    @Override
    public String getId() { return bucketKey; }

    @Override
    public boolean isNew() { return newEntity; }

    @PostLoad @PostPersist
    private void markPersisted() { newEntity = false; }
}
