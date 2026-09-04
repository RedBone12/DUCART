package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.ContactusDTO;
import com.prashant.api.ecom.ducart.modal.ContactusResponseDTO;
import com.prashant.api.ecom.ducart.security.JwtAuthenticationFilter;
import com.prashant.api.ecom.ducart.security.SecurityConfig;
import com.prashant.api.ecom.ducart.services.ContactusService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ContactusController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                JwtAuthenticationFilter.class,
                SecurityConfig.class
        })
})
@Import({
        GlobalExceptionHandler.class,
        ContactusControllerTest.TestSecurityConfig.class
})
class ContactusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContactusService contactusService;

    /*
     * Creates a valid ContactusDTO used as the request body in tests.
     */
    private ContactusDTO validContactusDTO() {
        ContactusDTO dto = new ContactusDTO();
        dto.setName("Alice");
        dto.setEmail("alice@test.com");
        dto.setPhone("1234567890");
        dto.setSubject("Order question");
        dto.setMessage("I want to ask about my order.");
        dto.setDate(LocalDate.of(2026, 6, 30));
        dto.setActive(true);
        return dto;
    }

    /*
     * Creates a ContactusResponseDTO representing data returned by the service.
     */
    private ContactusResponseDTO contactusResponseDTO() {
        ContactusResponseDTO responseDTO = new ContactusResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("Alice");
        responseDTO.setEmail("alice@test.com");
        responseDTO.setPhone("1234567890");
        responseDTO.setSubject("Order question");
        responseDTO.setMessage("I want to ask about my order.");
        responseDTO.setDate(LocalDate.of(2026, 6, 30));
        responseDTO.setActive(true);
        return responseDTO;
    }

    @Test
    void saveContactus_whenAnonymousAndRequestIsValid_shouldReturn201() throws Exception {
        ContactusDTO dto = validContactusDTO();
        ContactusResponseDTO responseDTO = contactusResponseDTO();

        // Mock the service response.
        when(contactusService.saveContactus(any(ContactusDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/contactus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.phone").value("1234567890"))
                .andExpect(jsonPath("$.subject").value("Order question"))
                .andExpect(jsonPath("$.message")
                        .value("I want to ask about my order."))
                .andExpect(jsonPath("$.date").value("2026-06-30"))
                .andExpect(jsonPath("$.active").value(true));

        verify(contactusService)
                .saveContactus(any(ContactusDTO.class));
    }

    @Test
    void saveContactus_whenNameIsBlank_shouldReturn400() throws Exception {
        ContactusDTO dto = validContactusDTO();
        dto.setName("");

        mockMvc.perform(post("/contactus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Name is required"));

        verifyNoInteractions(contactusService);
    }

    @Test
    void saveContactus_whenEmailIsBlank_shouldReturn400() throws Exception {
        ContactusDTO dto = validContactusDTO();
        dto.setEmail("");

        mockMvc.perform(post("/contactus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email is required"));

        verifyNoInteractions(contactusService);
    }

    @Test
    void saveContactus_whenPhoneIsBlank_shouldReturn400() throws Exception {
        ContactusDTO dto = validContactusDTO();
        dto.setPhone("");

        mockMvc.perform(post("/contactus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Phone Number is required"));

        verifyNoInteractions(contactusService);
    }

    @Test
    void saveContactus_whenSubjectIsBlank_shouldReturn400() throws Exception {
        ContactusDTO dto = validContactusDTO();
        dto.setSubject("");

        mockMvc.perform(post("/contactus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Subject is required"));

        verifyNoInteractions(contactusService);
    }

    @Test
    void saveContactus_whenMessageIsBlank_shouldReturn400() throws Exception {
        ContactusDTO dto = validContactusDTO();
        dto.setMessage("");

        mockMvc.perform(post("/contactus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Message is required"));

        verifyNoInteractions(contactusService);
    }

    @Test
    void getAllContactus_whenAnonymous_shouldReturn200() throws Exception {
        ContactusResponseDTO responseDTO = contactusResponseDTO();

        when(contactusService.getAllContactus())
                .thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/contactus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].email").value("alice@test.com"))
                .andExpect(jsonPath("$[0].phone").value("1234567890"))
                .andExpect(jsonPath("$[0].subject").value("Order question"))
                .andExpect(jsonPath("$[0].message")
                        .value("I want to ask about my order."))
                .andExpect(jsonPath("$[0].date").value("2026-06-30"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(contactusService).getAllContactus();
    }

    @Test
    void getById_whenAnonymousAndContactusExists_shouldReturn200()
            throws Exception {

        ContactusResponseDTO responseDTO = contactusResponseDTO();

        when(contactusService.findById(1L))
                .thenReturn(responseDTO);

        mockMvc.perform(get("/contactus/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.phone").value("1234567890"))
                .andExpect(jsonPath("$.subject").value("Order question"))
                .andExpect(jsonPath("$.message")
                        .value("I want to ask about my order."))
                .andExpect(jsonPath("$.date").value("2026-06-30"))
                .andExpect(jsonPath("$.active").value(true));

        verify(contactusService).findById(1L);
    }

    @Test
    void getById_whenContactusDoesNotExist_shouldReturn404()
            throws Exception {

        when(contactusService.findById(99L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Contact query not found"));

        mockMvc.perform(get("/contactus/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Contact query not found"));

        verify(contactusService).findById(99L);
    }

    @Test
    void update_whenAnonymousAndRequestIsValid_shouldReturn200()
            throws Exception {

        ContactusDTO dto = validContactusDTO();
        dto.setName("Alice Updated");
        dto.setSubject("Updated question");

        ContactusResponseDTO responseDTO = contactusResponseDTO();
        responseDTO.setName("Alice Updated");
        responseDTO.setSubject("Updated question");

        when(contactusService.update(
                eq(1L),
                any(ContactusDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/contactus/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice Updated"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.subject")
                        .value("Updated question"))
                .andExpect(jsonPath("$.date").value("2026-06-30"))
                .andExpect(jsonPath("$.active").value(true));

        verify(contactusService).update(
                eq(1L),
                any(ContactusDTO.class));
    }

    @Test
    void update_whenRequestIsInvalid_shouldReturn400() throws Exception {
        ContactusDTO dto = validContactusDTO();
        dto.setName("");

        mockMvc.perform(put("/contactus/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Name is required"));

        verifyNoInteractions(contactusService);
    }

    @Test
    void update_whenContactusDoesNotExist_shouldReturn404()
            throws Exception {

        ContactusDTO dto = validContactusDTO();

        when(contactusService.update(
                eq(99L),
                any(ContactusDTO.class))).thenThrow(
                        new ResourceNotFoundException(
                                "Contact query not found"));

        mockMvc.perform(put("/contactus/{id}", 99L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Contact query not found"));

        verify(contactusService).update(
                eq(99L),
                any(ContactusDTO.class));
    }

    @Test
    void delete_whenAnonymousAndContactusExists_shouldReturn200()
            throws Exception {

        doNothing().when(contactusService).delete(1L);

        mockMvc.perform(delete("/contactus/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Contact query deleted successfully"));

        verify(contactusService).delete(1L);
    }

    @Test
    void delete_whenContactusDoesNotExist_shouldReturn404()
            throws Exception {

        doThrow(
                new ResourceNotFoundException(
                        "Contact query not found"))
                .when(contactusService).delete(99L);

        mockMvc.perform(delete("/contactus/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Contact query not found"));

        verify(contactusService).delete(99L);
    }

    /*
     * Test-only Security configuration.
     *
     * The real JwtAuthenticationFilter is excluded because this is a
     * Controller slice test, not a complete JWT integration test.
     *
     * Security filters are still enabled, and /contactus/** is configured
     * as permitAll, matching the real SecurityConfig.
     */
    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http)
                throws Exception {

            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/contactus/**").permitAll()
                            .anyRequest().authenticated())
                    .build();
        }
    }
}