package com.prashant.api.ecom.ducart.repositories;

import com.prashant.api.ecom.ducart.entities.PasswordResetLimit;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;

public interface PasswordResetLimitRepo extends JpaRepository<PasswordResetLimit, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PasswordResetLimit r where r.bucketKey = :key")
    Optional<PasswordResetLimit> findLocked(@Param("key") String key);

    long deleteByWindowStartBefore(Instant before);
}
