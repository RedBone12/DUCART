package com.prashant.api.ecom.ducart.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtService jwtService;
    @Mock FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestWithoutBearerToken_shouldContinueUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JwtAuthenticationFilter(jwtService).doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void requestWithNonBearerAuthorization_shouldContinueUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic credentials");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JwtAuthenticationFilter(jwtService).doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validToken_shouldPopulateUsernameRoleAndUserId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractUsername("token-value")).thenReturn("alice");
        when(jwtService.extractUserId("token-value")).thenReturn(42L);
        when(jwtService.extractRole("token-value")).thenReturn("Admin");

        new JwtAuthenticationFilter(jwtService).doFilter(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("alice", authentication.getPrincipal());
        assertEquals(42L, authentication.getDetails());
        assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidToken_shouldClearContextAndStillContinueChain() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("old-user", null));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer broken");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractUsername("broken")).thenThrow(new IllegalArgumentException("invalid"));

        new JwtAuthenticationFilter(jwtService).doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validToken_shouldNotReplaceExistingAuthentication() throws Exception {
        var existing = new UsernamePasswordAuthenticationToken("existing", null);
        SecurityContextHolder.getContext().setAuthentication(existing);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractUsername("valid")).thenReturn("alice");
        when(jwtService.extractUserId("valid")).thenReturn(1L);
        when(jwtService.extractRole("valid")).thenReturn("Buyer");

        new JwtAuthenticationFilter(jwtService).doFilter(request, response, filterChain);

        assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenWithoutUsername_shouldNotCreateAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer no-subject");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.extractUsername("no-subject")).thenReturn(null);
        when(jwtService.extractUserId("no-subject")).thenReturn(1L);
        when(jwtService.extractRole("no-subject")).thenReturn("Buyer");

        new JwtAuthenticationFilter(jwtService).doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
