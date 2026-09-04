package com.prashant.api.ecom.ducart.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.prashant.api.ecom.ducart.entities.User;
import com.prashant.api.ecom.ducart.security.JwtService;

class JwtServiceTest {
    private JwtService jwtService;

    private static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @BeforeEach
    void setup() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);
    }

    private User user() {
        User user = new User();
        user.setUserid(1L);
        user.setName("Alice");
        user.setUsername("alice");
        user.setEmail("alice@test.com");
        user.setPhone("1234567890");
        user.setPassword("encodedPassword");
        user.setRole("Admin");
        return user;
    }

    @Test
    void generateToken_shouldReturnNonBlankToken() {
        String token = jwtService.generateToken(user());

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractUsername_shouldReturnUsernameFromToken() {
        String token = jwtService.generateToken(user());

        String username = jwtService.extractUsername(token);

        assertEquals("alice", username);
    }

    @Test
    void extractUserId_shouldReturnUserIdFromToken() {
        String token = jwtService.generateToken(user());

        Long userId = jwtService.extractUserId(token);

        assertEquals(1L, userId);
    }

    @Test
    void extractRole_shouldReturnRoleFromToken() {
        String token = jwtService.generateToken(user());

        String role = jwtService.extractRole(token);

        assertEquals("Admin", role);
    }

    @Test
    void generateToken_whenRoleIsNull_shouldUseBuyerRole() {
        User user = user();
        user.setRole(null);

        String token = jwtService.generateToken(user);

        assertEquals("Buyer", jwtService.extractRole(token));
    }

    @Test
    void extractUserId_whenTokenHasNoUserId_shouldReturnNull() {
        User user = user();
        user.setUserid(null);

        String token = jwtService.generateToken(user);

        assertNull(jwtService.extractUserId(token));
    }
}
