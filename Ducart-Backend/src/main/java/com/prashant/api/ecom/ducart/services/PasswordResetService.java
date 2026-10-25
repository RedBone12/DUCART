package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.*;
import com.prashant.api.ecom.ducart.exception.*;
import com.prashant.api.ecom.ducart.repositories.*;
import com.prashant.api.ecom.ducart.security.ResetTokenSupport;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PasswordResetService {
    private final UserRepo users;
    private final PasswordResetTokenRepo tokens;
    private final PasswordResetRateLimiter limiter;
    private final JavaMailSender mail;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final TransactionTemplate transaction;
    private final SecureRandom random = new SecureRandom();
    private final String frontendUrl;
    private final String from;

    public PasswordResetService(UserRepo users, PasswordResetTokenRepo tokens, PasswordResetRateLimiter limiter,
            JavaMailSender mail, PasswordEncoder encoder, Clock clock, PlatformTransactionManager manager,
            @Value("${app.password-reset.frontend-url:http://localhost:3000/forgot-password}") String frontendUrl,
            @Value("${app.password-reset.mail-from:no-reply@ducart.local}") String from) {
        this.users = users; this.tokens = tokens; this.limiter = limiter; this.mail = mail;
        this.encoder = encoder; this.clock = clock; this.transaction = new TransactionTemplate(manager);
        this.frontendUrl = frontendUrl; this.from = from;
    }

    public void request(String identity, String clientAddress) {
        if (!limiter.consume("request-ip", clientAddress, 10)) throw new TooManyRequestsException();
        String normalized = identity.trim();
        Optional<User> found = users.findByUsername(normalized).or(() -> users.findByEmail(normalized));
        String account = found.map(user -> "user:" + user.getUserid())
                .orElse("unknown:" + normalized.toLowerCase(Locale.ROOT));
        // Silently limit account mail volume so responses cannot enumerate registered accounts.
        if (!limiter.consume("request-account", account, 3) || found.isEmpty()) return;
        try {
            transaction.executeWithoutResult(status -> {
                User user = users.findForPasswordReset(found.get().getUserid())
                        .orElseThrow(() -> new BadRequestException("Account unavailable"));
                byte[] secret = new byte[32];
                random.nextBytes(secret);
                String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
                PasswordResetToken token = tokens.findForUpdate(user.getUserid()).orElseGet(PasswordResetToken::new);
                token.setUserId(user.getUserid());
                token.setTokenHash(ResetTokenSupport.hash(rawToken));
                token.setExpiresAt(clock.instant().plus(Duration.ofMinutes(15)));
                token.setUsed(false);
                tokens.saveAndFlush(token);
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(user.getEmail());
                message.setSubject("Reset your DUCART password");
                String separator = frontendUrl.contains("?") ? "&" : "?";
                message.setText("Use this link to reset your password within 15 minutes:\n"
                        + frontendUrl + separator + "token=" + rawToken
                        + "\n\nThis link can be used once. If you did not request it, ignore this email.");
                mail.send(message);
            });
        } catch (MailException ex) {
            // Roll back token replacement and never log reset links or SMTP credentials.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Password reset email delivery failed");
        }
    }

    public void confirm(String rawToken, String password, String clientAddress) {
        if (!limiter.consume("confirm-ip", clientAddress, 30)) throw new TooManyRequestsException();
        // BCrypt limits passwords to 72 bytes, rather than 72 Java characters.
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new BadRequestException("Password must not exceed 72 UTF-8 bytes");
        String hash = ResetTokenSupport.hash(rawToken);
        transaction.executeWithoutResult(status -> {
            Long userId = tokens.findUserIdByHash(hash)
                    .orElseThrow(PasswordResetService::invalidToken);
            // Lock the user before reloading the token: requests and confirmations share lock order.
            User user = users.findForPasswordReset(userId)
                    .orElseThrow(PasswordResetService::invalidToken);
            // Refresh under the lock to observe consumption by another transaction.
            PasswordResetToken current = tokens.findForUpdate(userId).orElseThrow(PasswordResetService::invalidToken);
            if (current.isUsed() || !hash.equals(current.getTokenHash())
                    || !current.getExpiresAt().isAfter(clock.instant())) throw invalidToken();
            user.setPassword(encoder.encode(password));
            current.setUsed(true);
            users.saveAndFlush(user);
            tokens.saveAndFlush(current);
        });
    }

    private static BadRequestException invalidToken() {
        return new BadRequestException("Reset link is invalid or expired. Please request a new link.");
    }
}
