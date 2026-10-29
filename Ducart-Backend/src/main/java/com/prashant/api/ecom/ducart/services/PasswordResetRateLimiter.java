package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.PasswordResetLimit;
import com.prashant.api.ecom.ducart.repositories.PasswordResetLimitRepo;
import com.prashant.api.ecom.ducart.security.ResetTokenSupport;
import java.time.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PasswordResetRateLimiter {
    private final PasswordResetLimitRepo limits;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public PasswordResetRateLimiter(PasswordResetLimitRepo limits, Clock clock, PlatformTransactionManager manager) {
        this.limits = limits;
        this.clock = clock;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public boolean consume(String scope, String subject, int maximum) {
        String key = ResetTokenSupport.hash(scope + ":" + subject);
        // Retry the first-insert race in a fresh transaction; subsequent requests lock the row.
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return Boolean.TRUE.equals(transaction.execute(status -> {
                    Instant now = clock.instant();
                    limits.deleteByWindowStartBefore(now.minus(Duration.ofDays(1)));
                    PasswordResetLimit limit = limits.findLocked(key).orElseGet(() -> {
                        PasswordResetLimit fresh = new PasswordResetLimit();
                        fresh.setBucketKey(key);
                        fresh.setWindowStart(now);
                        return limits.saveAndFlush(fresh);
                    });
                    if (!limit.getWindowStart().plus(Duration.ofMinutes(15)).isAfter(now)) {
                        limit.setWindowStart(now);
                        limit.setAttempts(0);
                    }
                    if (limit.getAttempts() >= maximum) return false;
                    limit.setAttempts(limit.getAttempts() + 1);
                    limits.saveAndFlush(limit);
                    return true;
                }));
            } catch (DataIntegrityViolationException | TransientDataAccessException ex) {
                if (attempt == 2) throw ex;
            }
        }
        return false;
    }
}
