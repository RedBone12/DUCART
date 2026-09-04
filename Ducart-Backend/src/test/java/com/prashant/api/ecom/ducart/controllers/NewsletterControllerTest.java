package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.NewsletterDTO;
import com.prashant.api.ecom.ducart.modal.NewsletterResponseDTO;
import com.prashant.api.ecom.ducart.services.NewsletterService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.prashant.api.ecom.ducart.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
@WebMvcTest(controllers = NewsletterController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
})
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        NewsletterControllerTest.TestMethodSecurityConfig.class
})
class NewsletterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NewsletterService newsletterService;

    private NewsletterDTO newsletterDTO(String email, Boolean active) {
        NewsletterDTO dto = new NewsletterDTO();
        dto.setEmail(email);
        dto.setActive(active);
        return dto;
    }

    private NewsletterResponseDTO newsletterResponseDTO(Long id, String email, boolean active) {
        NewsletterResponseDTO responseDTO = new NewsletterResponseDTO();
        responseDTO.setId(id);
        responseDTO.setEmail(email);
        responseDTO.setActive(active);
        return responseDTO;
    }

    @Test
    void createNewsletter_whenValidRequest_shouldReturn201() throws Exception {
        NewsletterDTO dto = newsletterDTO("alice@test.com", null);

        NewsletterResponseDTO responseDTO = newsletterResponseDTO(
                1L,
                "alice@test.com",
                true);

        when(newsletterService.createNewsletter(any(NewsletterDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.active").value(true));

        verify(newsletterService).createNewsletter(any(NewsletterDTO.class));
    }

    @Test
    void createNewsletter_whenEmailIsBlank_shouldReturn400() throws Exception {
        NewsletterDTO dto = newsletterDTO("", null);

        mockMvc.perform(post("/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email is required"));

        verifyNoInteractions(newsletterService);
    }

    @Test
    void createNewsletter_whenEmailFormatIsInvalid_shouldReturn400() throws Exception {
        NewsletterDTO dto = newsletterDTO("not-an-email", null);

        mockMvc.perform(post("/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email format is invalid"));

        verifyNoInteractions(newsletterService);
    }

    @Test
    void createNewsletter_whenEmailAlreadySubscribed_shouldReturn409() throws Exception {
        NewsletterDTO dto = newsletterDTO("alice@test.com", null);

        when(newsletterService.createNewsletter(any(NewsletterDTO.class)))
                .thenThrow(new ConflictException("Email already subscribed"));

        mockMvc.perform(post("/newsletter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already subscribed"));

        verify(newsletterService).createNewsletter(any(NewsletterDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllNewsletter_whenAdmin_shouldReturn200() throws Exception {
        NewsletterResponseDTO responseDTO = newsletterResponseDTO(
                1L,
                "alice@test.com",
                true);

        when(newsletterService.getAllNewsLetter())
                .thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/newsletter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("alice@test.com"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(newsletterService).getAllNewsLetter();
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void getAllNewsletter_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(get("/newsletter"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(newsletterService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getNewsletter_whenAdminAndNewsletterExists_shouldReturn200() throws Exception {
        NewsletterResponseDTO responseDTO = newsletterResponseDTO(
                1L,
                "alice@test.com",
                true);

        when(newsletterService.getNewsletterById(1L))
                .thenReturn(responseDTO);

        mockMvc.perform(get("/newsletter/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.active").value(true));

        verify(newsletterService).getNewsletterById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getNewsletter_whenNewsletterDoesNotExist_shouldReturn404() throws Exception {
        when(newsletterService.getNewsletterById(99L))
                .thenThrow(new ResourceNotFoundException("Newsletter not found"));

        mockMvc.perform(get("/newsletter/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Newsletter not found"));

        verify(newsletterService).getNewsletterById(99L);
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void getNewsletter_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(get("/newsletter/{id}", 1L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(newsletterService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateNewsletter_whenAdminAndValidRequest_shouldReturn200() throws Exception {
        NewsletterDTO dto = newsletterDTO("bob@test.com", false);

        NewsletterResponseDTO responseDTO = newsletterResponseDTO(
                1L,
                "bob@test.com",
                false);

        when(newsletterService.updateNewsletterById(eq(1L), any(NewsletterDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/newsletter/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("bob@test.com"))
                .andExpect(jsonPath("$.active").value(false));

        verify(newsletterService).updateNewsletterById(eq(1L), any(NewsletterDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateNewsletter_whenNewsletterDoesNotExist_shouldReturn404() throws Exception {
        NewsletterDTO dto = newsletterDTO("bob@test.com", false);

        when(newsletterService.updateNewsletterById(eq(99L), any(NewsletterDTO.class)))
                .thenThrow(new ResourceNotFoundException("Newsletter not found"));

        mockMvc.perform(put("/newsletter/{id}", 99L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Newsletter not found"));

        verify(newsletterService).updateNewsletterById(eq(99L), any(NewsletterDTO.class));
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void updateNewsletter_whenBuyer_shouldReturn403() throws Exception {
        NewsletterDTO dto = newsletterDTO("bob@test.com", false);

        mockMvc.perform(put("/newsletter/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(newsletterService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteNewsletter_whenAdminAndNewsletterExists_shouldReturn200() throws Exception {
        doNothing().when(newsletterService).deleteNewsletterById(1L);

        mockMvc.perform(delete("/newsletter/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Newsletter deleted successfully"));

        verify(newsletterService).deleteNewsletterById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteNewsletter_whenNewsletterDoesNotExist_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Newsletter not found"))
                .when(newsletterService)
                .deleteNewsletterById(99L);

        mockMvc.perform(delete("/newsletter/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Newsletter not found"));

        verify(newsletterService).deleteNewsletterById(99L);
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void deleteNewsletter_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/newsletter/{id}", 1L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(newsletterService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestMethodSecurityConfig {
        // This test config enables @PreAuthorize in controller tests.
        // It does not load the real JWT filter.
    }
}