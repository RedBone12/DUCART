package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.TestimonialDTO;
import com.prashant.api.ecom.ducart.modal.TestimonialResponseDTO;
import com.prashant.api.ecom.ducart.security.JwtService;
import com.prashant.api.ecom.ducart.services.TestimonialService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestimonialController.class)
@Import({
        GlobalExceptionHandler.class,
        TestimonialControllerTest.TestSecurityConfig.class
})
class TestimonialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TestimonialService testimonialService;

    @MockitoBean
    private JwtService jwtService;

    /*
     * 这里测试 TestimonialController：公开查询、Admin 创建/更新/删除、
     * multipart 和 JSON 两种请求、DTO 校验，以及 Service 抛出的 404。
     */
    private TestimonialDTO testimonialDTO(
            String name,
            String message,
            Boolean active) {

        TestimonialDTO dto = new TestimonialDTO();
        dto.setName(name);
        dto.setMessage(message);
        dto.setActive(active);
        return dto;
    }

    private TestimonialResponseDTO testimonialResponse(
            Long id,
            String name,
            String message,
            boolean active) {

        return TestimonialResponseDTO.builder()
                .id(id)
                .name(name)
                .message(message)
                .pic("/uploads/testimonials/alice.jpg")
                .active(active)
                .build();
    }

    private MockMultipartFile dataPart(TestimonialDTO dto)
            throws Exception {

        return new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto));
    }

    private MockMultipartFile picPart() {
        return new MockMultipartFile(
                "pic",
                "alice.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        when(testimonialService.findAll()).thenReturn(List.of(
                testimonialResponse(
                        1L,
                        "Alice",
                        "Great shopping experience!",
                        true),
                testimonialResponse(
                        2L,
                        "Bob",
                        "Fast delivery.",
                        false)));

        mockMvc.perform(get("/testimonial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].message")
                        .value("Great shopping experience!"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Bob"))
                .andExpect(jsonPath("$[1].active").value(false));

        verify(testimonialService).findAll();
    }

    @Test
    void getById_whenTestimonialExists_shouldReturn200()
            throws Exception {

        when(testimonialService.findById(1L))
                .thenReturn(testimonialResponse(
                        1L,
                        "Alice",
                        "Great shopping experience!",
                        true));

        mockMvc.perform(get("/testimonial/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.message")
                        .value("Great shopping experience!"))
                .andExpect(jsonPath("$.pic")
                        .value("/uploads/testimonials/alice.jpg"))
                .andExpect(jsonPath("$.active").value(true));

        verify(testimonialService).findById(1L);
    }

    @Test
    void getById_whenTestimonialDoesNotExist_shouldReturn404()
            throws Exception {

        when(testimonialService.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Testimonial not found"));

        mockMvc.perform(get("/testimonial/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Testimonial not found"));

        verify(testimonialService).findById(99L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createMultipart_whenAdminAndRequestIsValid_shouldReturn201()
            throws Exception {

        TestimonialDTO request = testimonialDTO(
                "Alice",
                "Great shopping experience!",
                true);

        TestimonialResponseDTO response = testimonialResponse(
                1L,
                "Alice",
                "Great shopping experience!",
                true);

        when(testimonialService.create(
                any(TestimonialDTO.class),
                any(MultipartFile.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/testimonial")
                .file(dataPart(request))
                .file(picPart()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.message")
                        .value("Great shopping experience!"))
                .andExpect(jsonPath("$.pic")
                        .value("/uploads/testimonials/alice.jpg"))
                .andExpect(jsonPath("$.active").value(true));

        verify(testimonialService).create(
                any(TestimonialDTO.class),
                any(MultipartFile.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createMultipart_whenNameIsBlank_shouldReturn400()
            throws Exception {

        TestimonialDTO request = testimonialDTO(
                "",
                "Great shopping experience!",
                true);

        mockMvc.perform(multipart("/testimonial")
                .file(dataPart(request))
                .file(picPart()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Name is required"));

        verifyNoInteractions(testimonialService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createMultipart_whenMessageIsBlank_shouldReturn400()
            throws Exception {

        TestimonialDTO request = testimonialDTO(
                "Alice",
                "",
                true);

        mockMvc.perform(multipart("/testimonial")
                .file(dataPart(request))
                .file(picPart()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Message is required"));

        verifyNoInteractions(testimonialService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createMultipart_whenJsonIsInvalid_shouldReturn400()
            throws Exception {

        MockMultipartFile badData = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{bad-json}".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/testimonial")
                .file(badData)
                .file(picPart()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Invalid testimonial data"));

        verifyNoInteractions(testimonialService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void createMultipart_whenBuyer_shouldReturn403()
            throws Exception {

        mockMvc.perform(multipart("/testimonial")
                .file(dataPart(testimonialDTO(
                        "Alice",
                        "Great shopping experience!",
                        true)))
                .file(picPart()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(testimonialService);
    }

    @Test
    void createMultipart_whenNotLoggedIn_shouldReturn401()
            throws Exception {

        mockMvc.perform(multipart("/testimonial")
                .file(dataPart(testimonialDTO(
                        "Alice",
                        "Great shopping experience!",
                        true)))
                .file(picPart()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(testimonialService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createJson_whenAdminAndRequestIsValid_shouldReturn201()
            throws Exception {

        TestimonialDTO request = testimonialDTO(
                "Alice",
                "Great shopping experience!",
                true);

        request.setPic("/uploads/testimonials/alice.jpg");

        TestimonialResponseDTO response = testimonialResponse(
                1L,
                "Alice",
                "Great shopping experience!",
                true);

        when(testimonialService.create(
                any(TestimonialDTO.class),
                isNull()))
                .thenReturn(response);

        mockMvc.perform(post("/testimonial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.message")
                        .value("Great shopping experience!"))
                .andExpect(jsonPath("$.pic")
                        .value("/uploads/testimonials/alice.jpg"));

        verify(testimonialService).create(
                any(TestimonialDTO.class),
                isNull());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createJson_whenNameIsBlank_shouldReturn400()
            throws Exception {

        TestimonialDTO request = testimonialDTO(
                "",
                "Great shopping experience!",
                true);

        mockMvc.perform(post("/testimonial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Name is required"));

        verifyNoInteractions(testimonialService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createJson_whenMessageIsBlank_shouldReturn400()
            throws Exception {

        TestimonialDTO request = testimonialDTO(
                "Alice",
                "",
                true);

        mockMvc.perform(post("/testimonial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Message is required"));

        verifyNoInteractions(testimonialService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateMultipart_whenRequestIsValid_shouldReturn200()
            throws Exception {

        TestimonialDTO request = testimonialDTO(
                "Alice Updated",
                "Updated message",
                false);

        TestimonialResponseDTO response = testimonialResponse(
                1L,
                "Alice Updated",
                "Updated message",
                false);

        when(testimonialService.update(
                eq(1L),
                any(TestimonialDTO.class),
                any(MultipartFile.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/testimonial/{id}", 1L)
                .file(dataPart(request))
                .file(picPart())
                .with(req -> {
                    req.setMethod("PUT");
                    return req;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Alice Updated"))
                .andExpect(jsonPath("$.message")
                        .value("Updated message"))
                .andExpect(jsonPath("$.active").value(false));

        verify(testimonialService).update(
                eq(1L),
                any(TestimonialDTO.class),
                any(MultipartFile.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateMultipart_whenTestimonialDoesNotExist_shouldReturn404()
            throws Exception {

        TestimonialDTO request = testimonialDTO(
                "Alice Updated",
                "Updated message",
                true);

        when(testimonialService.update(
                eq(99L),
                any(TestimonialDTO.class),
                any(MultipartFile.class)))
                .thenThrow(new ResourceNotFoundException(
                        "Testimonial not found"));

        mockMvc.perform(multipart("/testimonial/{id}", 99L)
                .file(dataPart(request))
                .file(picPart())
                .with(req -> {
                    req.setMethod("PUT");
                    return req;
                }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Testimonial not found"));

        verify(testimonialService).update(
                eq(99L),
                any(TestimonialDTO.class),
                any(MultipartFile.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateJson_whenRequestIsValid_shouldReturn200()
            throws Exception {

        TestimonialDTO request = testimonialDTO(
                "Alice Updated",
                "Updated message",
                false);

        TestimonialResponseDTO response = testimonialResponse(
                1L,
                "Alice Updated",
                "Updated message",
                false);

        when(testimonialService.updatePlain(
                eq(1L),
                any(TestimonialDTO.class)))
                .thenReturn(response);

        mockMvc.perform(put("/testimonial/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Alice Updated"))
                .andExpect(jsonPath("$.message")
                        .value("Updated message"))
                .andExpect(jsonPath("$.active").value(false));

        verify(testimonialService).updatePlain(
                eq(1L),
                any(TestimonialDTO.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateJson_whenTestimonialDoesNotExist_shouldReturn404()
            throws Exception {

        TestimonialDTO request = testimonialDTO(
                "Alice Updated",
                "Updated message",
                true);

        when(testimonialService.updatePlain(
                eq(99L),
                any(TestimonialDTO.class)))
                .thenThrow(new ResourceNotFoundException(
                        "Testimonial not found"));

        mockMvc.perform(put("/testimonial/{id}", 99L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Testimonial not found"));

        verify(testimonialService).updatePlain(
                eq(99L),
                any(TestimonialDTO.class));
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void updateJson_whenBuyer_shouldReturn403()
            throws Exception {

        mockMvc.perform(put("/testimonial/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        testimonialDTO(
                                "Alice",
                                "Updated message",
                                true))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(testimonialService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenTestimonialExists_shouldReturn200()
            throws Exception {

        mockMvc.perform(delete("/testimonial/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Testimonial deleted successfully"));

        verify(testimonialService).delete(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenTestimonialDoesNotExist_shouldReturn404()
            throws Exception {

        doThrow(new ResourceNotFoundException(
                "Testimonial not found"))
                .when(testimonialService)
                .delete(99L);

        mockMvc.perform(delete("/testimonial/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Testimonial not found"));

        verify(testimonialService).delete(99L);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void delete_whenBuyer_shouldReturn403()
            throws Exception {

        mockMvc.perform(delete("/testimonial/{id}", 1L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(testimonialService);
    }

    @Test
    void delete_whenNotLoggedIn_shouldReturn401()
            throws Exception {

        mockMvc.perform(delete("/testimonial/{id}", 1L))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(testimonialService);
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(
                HttpSecurity http) throws Exception {

            return http
                    .csrf(csrf -> csrf.disable())
                    .exceptionHandling(exception -> exception.authenticationEntryPoint(
                            new HttpStatusEntryPoint(
                                    HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(
                                    HttpMethod.GET,
                                    "/testimonial",
                                    "/testimonial/**")
                            .permitAll()
                            .requestMatchers(
                                    HttpMethod.POST,
                                    "/testimonial",
                                    "/testimonial/**")
                            .hasRole("ADMIN")
                            .requestMatchers(
                                    HttpMethod.PUT,
                                    "/testimonial",
                                    "/testimonial/**")
                            .hasRole("ADMIN")
                            .requestMatchers(
                                    HttpMethod.DELETE,
                                    "/testimonial",
                                    "/testimonial/**")
                            .hasRole("ADMIN")
                            .anyRequest()
                            .permitAll())
                    .build();
        }
    }
}