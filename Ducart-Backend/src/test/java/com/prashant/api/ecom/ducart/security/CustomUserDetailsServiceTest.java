package com.prashant.api.ecom.ducart.security;

import com.prashant.api.ecom.ducart.entities.User;
import com.prashant.api.ecom.ducart.repositories.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock UserRepo userRepo;
    @InjectMocks CustomUserDetailsService service;

    @Test
    void loadByUsername_shouldCreateSpringUserWithNormalizedRole() {
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user("alice", "Admin")));

        UserDetails details = service.loadUserByUsername("alice");

        assertEquals("alice", details.getUsername());
        assertEquals("encoded", details.getPassword());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(userRepo, never()).findByEmail(anyString());
    }

    @Test
    void loadByEmail_shouldFallBackWhenUsernameDoesNotMatch() {
        when(userRepo.findByUsername("alice@example.com")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(user("alice", "Buyer")));

        UserDetails details = service.loadUserByUsername("alice@example.com");

        assertEquals("alice", details.getUsername());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_BUYER")));
    }

    @Test
    void loadWhenRoleIsNull_shouldDefaultToBuyer() {
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user("alice", null)));

        UserDetails details = service.loadUserByUsername("alice");

        assertEquals("ROLE_BUYER", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadWhenNoUserMatches_shouldThrowUsernameNotFound() {
        when(userRepo.findByUsername("missing")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
    }

    private User user(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encoded");
        user.setRole(role);
        return user;
    }
}
