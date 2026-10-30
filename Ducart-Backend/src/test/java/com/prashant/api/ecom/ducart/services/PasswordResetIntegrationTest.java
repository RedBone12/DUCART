package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.*;
import com.prashant.api.ecom.ducart.repositories.*;
import com.prashant.api.ecom.ducart.security.ResetTokenSupport;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordResetService service;
    @Autowired UserRepo users;
    @Autowired PasswordResetTokenRepo tokens;
    @Autowired PasswordResetLimitRepo limits;
    @Autowired PasswordEncoder encoder;
    @Autowired PasswordResetRateLimiter limiter;
    @MockitoBean JavaMailSender mail;
    @MockitoBean Clock clock;
    final Instant now = Instant.parse("2026-10-25T12:00:00Z");
    User buyer;

    @BeforeEach
    void setup() {
        when(clock.instant()).thenReturn(now);
        tokens.deleteAll();
        limits.deleteAll();
        users.deleteAll();
        buyer = new User();
        buyer.setName("Buyer"); buyer.setUsername("reset-buyer"); buyer.setEmail("reset@example.test");
        buyer.setPhone("1234567890"); buyer.setPassword(encoder.encode("OriginalPassword123"));
        buyer = users.saveAndFlush(buyer);
    }

    ResultActions request(String identity) throws Exception {
        return mvc.perform(post("/user/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"" + identity + "\"}"));
    }

    String issue() throws Exception {
        request("reset-buyer").andExpect(status().isAccepted());
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, atLeastOnce()).send(message.capture());
        SimpleMailMessage latest = message.getAllValues().get(message.getAllValues().size() - 1);
        assertArrayEquals(new String[]{"reset@example.test"}, latest.getTo());
        Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]{43})").matcher(latest.getText());
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    ResultActions confirm(String token, String password) throws Exception {
        return mvc.perform(post("/user/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + password + "\"}"));
    }

    @Test
    void emailContainsLinkButDatabaseOnlyContainsHash() throws Exception {
        String token = issue();
        PasswordResetToken stored = tokens.findById(buyer.getUserid()).orElseThrow();
        assertNotEquals(token, stored.getTokenHash());
        assertEquals(ResetTokenSupport.hash(token), stored.getTokenHash());
        assertEquals(now.plusSeconds(900), stored.getExpiresAt());
        assertFalse(stored.isUsed());
    }

    @Test
    void validTokenChangesPasswordAndCannotBeUsedAgain() throws Exception {
        String token = issue();
        confirm(token, "ReplacementPassword123").andExpect(status().isOk());
        assertTrue(encoder.matches("ReplacementPassword123", users.findById(buyer.getUserid()).orElseThrow().getPassword()));
        confirm(token, "AnotherPassword123").andExpect(status().isBadRequest());
        assertTrue(tokens.findById(buyer.getUserid()).orElseThrow().isUsed());
    }

    @Test
    void expiredTokenDoesNotChangePassword() throws Exception {
        String token = issue();
        when(clock.instant()).thenReturn(now.plusSeconds(900));
        confirm(token, "ReplacementPassword123").andExpect(status().isBadRequest());
        assertTrue(encoder.matches("OriginalPassword123", users.findById(buyer.getUserid()).orElseThrow().getPassword()));
    }

    @Test
    void newRequestInvalidatesPreviousLink() throws Exception {
        String first = issue();
        String second = issue();
        assertNotEquals(first, second);
        confirm(first, "ReplacementPassword123").andExpect(status().isBadRequest());
        confirm(second, "ReplacementPassword123").andExpect(status().isOk());
    }

    @Test
    void unknownAccountGetsIdenticalResponseAndNoEmail() throws Exception {
        String unknown = request("unknown-user").andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        verifyNoInteractions(mail);
        String known = request("reset-buyer").andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertEquals(known, unknown);
    }

    @Test
    void accountMailLimitCombinesUsernameAndEmail() throws Exception {
        for (String identity : List.of("reset-buyer", "reset@example.test", "reset-buyer", "reset@example.test"))
            request(identity).andExpect(status().isAccepted());
        verify(mail, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    void ipLimitReturns429AndRecoversAfterWindow() throws Exception {
        for (int i = 0; i < 10; i++) request("missing-" + i).andExpect(status().isAccepted());
        request("reset-buyer").andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "900"));
        when(clock.instant()).thenReturn(now.plusSeconds(901));
        request("reset-buyer").andExpect(status().isAccepted());
        verify(mail).send(any(SimpleMailMessage.class));
    }

    @Test
    void confirmationAttemptsAreRateLimitedEvenForUnknownTokens() throws Exception {
        String unknown = "a".repeat(43);
        for (int i = 0; i < 30; i++) confirm(unknown, "ReplacementPassword123").andExpect(status().isBadRequest());
        confirm(unknown, "ReplacementPassword123").andExpect(status().isTooManyRequests());
    }

    @Test
    void smtpFailureDoesNotInvalidatePreviouslyDeliveredLink() throws Exception {
        String first = issue();
        doThrow(new MailSendException("Simulated mail outage")).when(mail).send(any(SimpleMailMessage.class));
        request("reset-buyer").andExpect(status().isAccepted());
        assertEquals(ResetTokenSupport.hash(first), tokens.findById(buyer.getUserid()).orElseThrow().getTokenHash());
        confirm(first, "ReplacementPassword123").andExpect(status().isOk());
    }

    @Test
    void phoneBasedLegacyResetNoLongerWorks() throws Exception {
        mvc.perform(put("/user/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"reset-buyer\",\"phone\":\"1234567890\",\"newPassword\":\"AttackerPassword123\"}"))
                .andExpect(status().isUnauthorized());
        assertTrue(encoder.matches("OriginalPassword123", users.findById(buyer.getUserid()).orElseThrow().getPassword()));
    }

    @Test
    void malformedTokenAndWeakPasswordsAreRejected() throws Exception {
        String token = issue();
        mvc.perform(post("/user/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Invalid request body"));
        confirm("invalid", "ReplacementPassword123").andExpect(status().isBadRequest());
        confirm(token, "short").andExpect(status().isBadRequest());
        confirm(token, "\u20ac".repeat(25)).andExpect(status().isBadRequest());
        assertFalse(tokens.findById(buyer.getUserid()).orElseThrow().isUsed());
    }

    @Test
    void concurrentTokenConsumptionSucceedsExactlyOnce() throws Exception {
        String token = issue();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (String address : List.of("client-one", "client-two")) results.add(executor.submit(() -> {
                if (!start.await(10, TimeUnit.SECONDS)) return false;
                try { service.confirm(token, "ReplacementPassword123", address); return true; }
                catch (com.prashant.api.ecom.ducart.exception.BadRequestException ex) { return false; }
            }));
            start.countDown();
            int successes = 0;
            for (Future<Boolean> result : results) if (result.get(20, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void concurrentRateLimitCreationDoesNotAllowExtraRequests() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 20; round++) {
                String subject = "shared-client-" + round;
                CountDownLatch start = new CountDownLatch(1);
                Callable<Boolean> task = () -> {
                    if (!start.await(10, TimeUnit.SECONDS)) return false;
                    return limiter.consume("concurrent-limit", subject, 1);
                };
                Future<Boolean> first = executor.submit(task);
                Future<Boolean> second = executor.submit(task);
                start.countDown();
                int successes = first.get(20, TimeUnit.SECONDS) ? 1 : 0;
                if (second.get(20, TimeUnit.SECONDS)) successes++;
                assertEquals(1, successes);
            }
        } finally { executor.shutdownNow(); }
    }

    @Test
    void aFreshAssignedKeyCannotOverwriteAnExistingCounter() {
        assertTrue(limiter.consume("insert-race", "same-client", 1));
        PasswordResetLimit competing = new PasswordResetLimit();
        competing.setBucketKey(ResetTokenSupport.hash("insert-race:same-client"));
        competing.setWindowStart(now);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> limits.saveAndFlush(competing));
        assertFalse(limiter.consume("insert-race", "same-client", 1));
        assertEquals(1, limits.findById(competing.getBucketKey()).orElseThrow().getAttempts());
    }

    @Test
    void userResponsesAndEntitySerializationNeverContainPassword() throws Exception {
        mvc.perform(get("/user").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].password").doesNotExist());
        mvc.perform(get("/user/" + buyer.getUserid()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.username").value("reset-buyer"));
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(buyer).contains("password"));
    }
}
