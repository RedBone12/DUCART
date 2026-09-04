package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.entities.User;
import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.modal.LoginDTO;
import com.prashant.api.ecom.ducart.repositories.UserRepo;
import com.prashant.api.ecom.ducart.security.JwtService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoginController.class)
@Import({ GlobalExceptionHandler.class, LoginControllerTest.TestSecurityConfig.class })
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserRepo userRepo;

    @MockitoBean
    private JwtService jwtService;

    /*
     * 这里测试 LoginController：空字段校验、AuthenticationManager 是否被调用、username/email
     * 两种登录查找路径、
     * JwtService 是否生成 token，以及登录失败时是否返回正确状态码。真正的密码比对由 Spring Security 负责，不在
     * ControllerTest 里测。
     */
    private LoginDTO loginDTO(String username, String password) {
        return new LoginDTO(username, password);
    }

    private User user(Long id, String username, String email, String role) {
        User user = new User();
        user.setUserid(id);
        user.setName("Test User");
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone("1234567890");
        user.setPassword("$2a$10$encoded-password");
        user.setRole(role);
        return user;
    }

    @Test
    void login_whenUsernameAndPasswordAreValid_shouldReturn200AndToken() throws Exception {
        LoginDTO request = loginDTO("buyer", "password");
        User existing = user(1L, "buyer", "buyer@test.com", "Buyer");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("buyer", null));
        when(userRepo.findByUsername("buyer")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing)).thenReturn("jwt-token");

        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userid").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.username").value("buyer"))
                .andExpect(jsonPath("$.email").value("buyer@test.com"))
                .andExpect(jsonPath("$.role").value("Buyer"))
                .andExpect(jsonPath("$.token").value("jwt-token"));

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(userRepo).findByUsername("buyer");
        verify(userRepo, never()).findByEmail("buyer");
        verify(jwtService).generateToken(existing);
    }

    @Test
    void login_whenEmailAndPasswordAreValid_shouldReturn200AndToken() throws Exception {
        LoginDTO request = loginDTO("buyer@test.com", "password");
        User existing = user(1L, "buyer", "buyer@test.com", "Buyer");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("buyer@test.com", null));
        when(userRepo.findByUsername("buyer@test.com")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("buyer@test.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing)).thenReturn("jwt-token");

        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userid").value(1))
                .andExpect(jsonPath("$.username").value("buyer"))
                .andExpect(jsonPath("$.email").value("buyer@test.com"))
                .andExpect(jsonPath("$.token").value("jwt-token"));

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(userRepo).findByUsername("buyer@test.com");
        verify(userRepo).findByEmail("buyer@test.com");
        verify(jwtService).generateToken(existing);
    }

    @Test
    void login_whenRoleIsNull_shouldReturnBuyerAsDefaultRole() throws Exception {
        LoginDTO request = loginDTO("buyer", "password");
        User existing = user(1L, "buyer", "buyer@test.com", null);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("buyer", null));
        when(userRepo.findByUsername("buyer")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing)).thenReturn("jwt-token");

        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("Buyer"))
                .andExpect(jsonPath("$.token").value("jwt-token"));

        verify(jwtService).generateToken(existing);
    }

    @Test
    void login_shouldPassUsernameAndPasswordToAuthenticationManager() throws Exception {
        LoginDTO request = loginDTO("buyer", "password");
        User existing = user(1L, "buyer", "buyer@test.com", "Buyer");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("buyer", null));
        when(userRepo.findByUsername("buyer")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing)).thenReturn("jwt-token");

        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("buyer", captor.getValue().getPrincipal());
        assertEquals("password", captor.getValue().getCredentials());
    }

    @Test
    void login_whenUsernameIsBlank_shouldReturn400() throws Exception {
        LoginDTO request = loginDTO("", "password");

        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Username or email is required"));

        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(userRepo);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_whenPasswordIsBlank_shouldReturn400() throws Exception {
        LoginDTO request = loginDTO("buyer", "");

        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Password is required"));

        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(userRepo);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_whenAuthenticationFails_shouldReturn401() throws Exception {
        LoginDTO request = loginDTO("buyer", "wrong-password");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Invalid username/email or password"));

        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username/email or password"));

        verify(authenticationManager).authenticate(any(Authentication.class));
        verifyNoInteractions(userRepo);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_whenAuthenticatedButUserNotFound_shouldReturn401() throws Exception {
        LoginDTO request = loginDTO("ghost", "password");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("ghost", null));
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username/email or password"));

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(userRepo).findByUsername("ghost");
        verify(userRepo).findByEmail("ghost");
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_whenUsernameOrPasswordIsNull_shouldReturn400() throws Exception {
        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO(null, "password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username or email is required"));

        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO("buyer", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password is required"));

        verifyNoInteractions(authenticationManager, userRepo, jwtService);
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.POST, "/user/login").permitAll()
                            .anyRequest().permitAll())
                    .build();
        }
    }
}
