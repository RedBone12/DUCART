package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.entities.User;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.ForgotPasswordDTO;
import com.prashant.api.ecom.ducart.modal.ProfileDTO;
import com.prashant.api.ecom.ducart.modal.SignupDTO;
import com.prashant.api.ecom.ducart.security.JwtService;
import com.prashant.api.ecom.ducart.services.UserService;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SignupController.class)
@Import({ GlobalExceptionHandler.class, SignupControllerTest.TestSecurityConfig.class })
class SignupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    /*
     * 这里测试 SignupController 层：注册时是否使用 SignupDTO/SignupResponseDTO、不返回
     * password、权限控制、authentication.details 是否能控制“只能访问自己”，以及 forgot-password、profile
     * multipart update、admin update/delete 的状态码。a
     */
    private SignupDTO signupDTO() {
        SignupDTO dto = new SignupDTO();
        dto.setName("Test User");
        dto.setUsername("buyer");
        dto.setEmail("buyer@test.com");
        dto.setPhone("1234567890");
        dto.setPassword("password123");
        return dto;
    }

    private User user(Long id, String username, String role) {
        User user = new User();
        user.setUserid(id);
        user.setName("Test User");
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPhone("1234567890");
        user.setPassword("$2a$10$encoded-password");
        user.setRole(role);
        user.setAddress("Main Street");
        user.setCity("Galway");
        user.setState("Connacht");
        user.setPin("H91");
        user.setPic("user.jpg");
        return user;
    }

    private UsernamePasswordAuthenticationToken authWithDetails(String username, String role, Long userId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(userId);
        return auth;
    }

    private MockMultipartFile profileDataPart(ProfileDTO dto) throws Exception {
        return new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto));
    }

    private MockMultipartFile profilePicPart() {
        return new MockMultipartFile(
                "pic",
                "user.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void signup_whenRequestIsValid_shouldReturn201AndNotReturnPassword() throws Exception {
        SignupDTO request = signupDTO();
        User saved = user(1L, "buyer", "Buyer");

        when(userService.signup(any(User.class))).thenReturn(saved);

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userid").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.username").value("buyer"))
                .andExpect(jsonPath("$.email").value("buyer@test.com"))
                .andExpect(jsonPath("$.phone").value("1234567890"))
                .andExpect(jsonPath("$.role").value("Buyer"))
                .andExpect(jsonPath("$.password").doesNotExist());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).signup(captor.capture());
        assertEquals("buyer", captor.getValue().getUsername());
        assertEquals("password123", captor.getValue().getPassword());
    }

    @Test
    void signup_whenNameIsBlank_shouldReturn400() throws Exception {
        SignupDTO request = signupDTO();
        request.setName("");

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Name is required"));

        verifyNoInteractions(userService);
    }

    @Test
    void signup_whenEmailAlreadyExists_shouldReturn409() throws Exception {
        SignupDTO request = signupDTO();

        when(userService.signup(any(User.class)))
                .thenThrow(new ConflictException("Email already exists"));

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"));

        verify(userService).signup(any(User.class));
    }

    @Test
    void signup_whenServiceThrowsBadRequest_shouldReturn400() throws Exception {
        SignupDTO request = signupDTO();
        request.setPhone("1234567");

        when(userService.signup(any(User.class)))
                .thenThrow(new BadRequestException("Invalid phone number format"));

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid phone number format"));

        verify(userService).signup(any(User.class));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void getAllUsers_whenAdmin_shouldReturn200() throws Exception {
        when(userService.findAll()).thenReturn(List.of(
                user(1L, "buyer1", "Buyer"),
                user(2L, "buyer2", "Buyer")));

        mockMvc.perform(get("/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userid").value(1))
                .andExpect(jsonPath("$[0].username").value("buyer1"))
                .andExpect(jsonPath("$[1].userid").value(2))
                .andExpect(jsonPath("$[1].username").value("buyer2"));

        verify(userService).findAll();
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "buyer", roles = "BUYER")
    void getAllUsers_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void getAllUsers_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void getUserById_whenOwner_shouldReturn200() throws Exception {
        when(userService.findById(1L)).thenReturn(user(1L, "buyer", "Buyer"));

        mockMvc.perform(get("/user/{userid}", 1L)
                .with(authentication(authWithDetails("buyer", "BUYER", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userid").value(1))
                .andExpect(jsonPath("$.username").value("buyer"))
                .andExpect(jsonPath("$.city").value("Galway"));

        verify(userService).findById(1L);
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void getUserById_whenAdmin_shouldReturn200() throws Exception {
        when(userService.findById(1L)).thenReturn(user(1L, "buyer", "Buyer"));

        mockMvc.perform(get("/user/{userid}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userid").value(1))
                .andExpect(jsonPath("$.username").value("buyer"));

        verify(userService).findById(1L);
    }

    @Test
    void getUserById_whenNotOwner_shouldReturn403() throws Exception {
        mockMvc.perform(get("/user/{userid}", 2L)
                .with(authentication(authWithDetails("buyer", "BUYER", 1L))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void getUserById_whenUserDoesNotExist_shouldReturn404() throws Exception {
        when(userService.findById(99L)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/user/{userid}", 99L)
                .with(authentication(authWithDetails("buyer", "BUYER", 99L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(userService).findById(99L);
    }

    @Test
    void forgotPassword_whenRequestIsValid_shouldReturn200() throws Exception {
        ForgotPasswordDTO request = new ForgotPasswordDTO("buyer", "1234567890", "newPassword123");

        when(userService.resetPassword(any(ForgotPasswordDTO.class)))
                .thenReturn(user(1L, "buyer", "Buyer"));

        mockMvc.perform(put("/user/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        verify(userService).resetPassword(any(ForgotPasswordDTO.class));
    }

    @Test
    void forgotPassword_whenUserDoesNotExist_shouldReturn404() throws Exception {
        ForgotPasswordDTO request = new ForgotPasswordDTO("ghost", "1234567890", "newPassword123");

        when(userService.resetPassword(any(ForgotPasswordDTO.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/user/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(userService).resetPassword(any(ForgotPasswordDTO.class));
    }

    @Test
    void updateProfile_whenOwnerAndMultipartRequestIsValid_shouldReturn200() throws Exception {
        ProfileDTO dto = new ProfileDTO("New Name", "1234567890", "New Address", "Galway", "Connacht", "H91", null);
        User updated = user(1L, "buyer", "Buyer");
        updated.setName("New Name");
        updated.setAddress("New Address");

        when(userService.updateProfile(eq(1L), any(ProfileDTO.class), any(MultipartFile.class)))
                .thenReturn(updated);

        mockMvc.perform(multipart("/user/{userid}", 1L)
                .file(profileDataPart(dto))
                .file(profilePicPart())
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .with(authentication(authWithDetails("buyer", "BUYER", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userid").value(1))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.address").value("New Address"))
                .andExpect(jsonPath("$.city").value("Galway"));

        verify(userService).updateProfile(eq(1L), any(ProfileDTO.class), any(MultipartFile.class));
    }

    @Test
    void updateProfile_whenNotOwner_shouldReturn403() throws Exception {
        ProfileDTO dto = new ProfileDTO("New Name", "1234567890", "New Address", "Galway", "Connacht", "H91", null);

        mockMvc.perform(multipart("/user/{userid}", 2L)
                .file(profileDataPart(dto))
                .file(profilePicPart())
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .with(authentication(authWithDetails("buyer", "BUYER", 1L))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void updateUser_whenAdmin_shouldReturn200() throws Exception {
        User incoming = user(null, "buyer", "Buyer");
        incoming.setName("Admin Updated");
        User updated = user(1L, "buyer", "Buyer");
        updated.setName("Admin Updated");

        when(userService.update(eq(1L), any(User.class))).thenReturn(updated);

        mockMvc.perform(put("/user/{userid}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(incoming)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userid").value(1))
                .andExpect(jsonPath("$.name").value("Admin Updated"));

        verify(userService).update(eq(1L), any(User.class));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "buyer", roles = "BUYER")
    void updateUser_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(put("/user/{userid}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user(null, "buyer", "Buyer"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void deleteUser_whenAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/user/{userid}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(userService).delete(1L);
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void deleteUser_whenUserDoesNotExist_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("User not found"))
                .when(userService).delete(99L);

        mockMvc.perform(delete("/user/{userid}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(userService).delete(99L);
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "buyer", roles = "BUYER")
    void deleteUser_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/user/{userid}", 1L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.POST, "/user").permitAll()
                            .requestMatchers(HttpMethod.PUT, "/user/forgot-password").permitAll()
                            .requestMatchers("/user", "/user/**").authenticated()
                            .anyRequest().permitAll())
                    .build();
        }
    }
}