package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.BrandDTO;
import com.prashant.api.ecom.ducart.modal.BrandResponseDTO;
import com.prashant.api.ecom.ducart.security.JwtService;
import com.prashant.api.ecom.ducart.services.BrandService;

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrandController.class)
@Import({GlobalExceptionHandler.class, BrandControllerTest.TestSecurityConfig.class})
class BrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private JwtService jwtService;

    /*
     * 这里测试 BrandController 层：GET 是否公开、Admin 才能创建/更新/删除、multipart data 是否能转成 BrandDTO、
     * DTO 校验是否生效，以及 BrandService 抛出的 404/409 是否能变成正确 HTTP status。
     */
    private BrandDTO brandDTO(String name, Boolean active) {
        BrandDTO dto = new BrandDTO();
        dto.setName(name);
        dto.setActive(active);
        return dto;
    }

    private BrandResponseDTO brandResponse(Long id, String name, boolean active) {
        return BrandResponseDTO.builder()
                .id(id)
                .name(name)
                .pic("/uploads/brands/" + name.toLowerCase() + ".jpg")
                .active(active)
                .build();
    }

    private MockMultipartFile dataPart(BrandDTO dto) throws Exception {
        return new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto));
    }

    private MockMultipartFile picPart() {
        return new MockMultipartFile(
                "pic",
                "nike.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        when(brandService.findAll()).thenReturn(List.of(
                brandResponse(1L, "Nike", true),
                brandResponse(2L, "Adidas", false)));

        mockMvc.perform(get("/brand"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Nike"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].name").value("Adidas"))
                .andExpect(jsonPath("$[1].active").value(false));

        verify(brandService).findAll();
    }

    @Test
    void getById_whenBrandExists_shouldReturn200() throws Exception {
        when(brandService.findById(1L)).thenReturn(brandResponse(1L, "Nike", true));

        mockMvc.perform(get("/brand/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Nike"))
                .andExpect(jsonPath("$.pic").value("/uploads/brands/nike.jpg"))
                .andExpect(jsonPath("$.active").value(true));

        verify(brandService).findById(1L);
    }

    @Test
    void getById_whenBrandDoesNotExist_shouldReturn404() throws Exception {
        when(brandService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Brand not found"));

        mockMvc.perform(get("/brand/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Brand not found"));

        verify(brandService).findById(99L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_whenAdminAndValidRequest_shouldReturn201() throws Exception {
        BrandDTO request = brandDTO("Nike", true);
        BrandResponseDTO response = brandResponse(1L, "Nike", true);

        when(brandService.create(any(BrandDTO.class), any(MultipartFile.class))).thenReturn(response);

        mockMvc.perform(multipart("/brand")
                        .file(dataPart(request))
                        .file(picPart()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Nike"))
                .andExpect(jsonPath("$.pic").value("/uploads/brands/nike.jpg"))
                .andExpect(jsonPath("$.active").value(true));

        verify(brandService).create(any(BrandDTO.class), any(MultipartFile.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_whenNameIsBlank_shouldReturn400() throws Exception {
        BrandDTO request = brandDTO("", true);

        mockMvc.perform(multipart("/brand")
                        .file(dataPart(request))
                        .file(picPart()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Brand name is required"));

        verifyNoInteractions(brandService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_whenJsonIsInvalid_shouldReturn400() throws Exception {
        MockMultipartFile badData = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{bad-json}".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/brand")
                        .file(badData)
                        .file(picPart()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid brand data"));

        verifyNoInteractions(brandService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(multipart("/brand")
                        .file(dataPart(brandDTO("Nike", true)))
                        .file(picPart()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(brandService);
    }

    @Test
    void create_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(multipart("/brand")
                        .file(dataPart(brandDTO("Nike", true)))
                        .file(picPart()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(brandService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateMultipart_whenAdminAndValidRequest_shouldReturn200() throws Exception {
        BrandDTO request = brandDTO("Adidas", false);
        BrandResponseDTO response = brandResponse(1L, "Adidas", false);

        when(brandService.update(eq(1L), any(BrandDTO.class), any(MultipartFile.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/brand/{id}", 1L)
                        .file(dataPart(request))
                        .file(picPart())
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Adidas"))
                .andExpect(jsonPath("$.active").value(false));

        verify(brandService).update(eq(1L), any(BrandDTO.class), any(MultipartFile.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateMultipart_whenBrandDoesNotExist_shouldReturn404() throws Exception {
        BrandDTO request = brandDTO("Adidas", false);

        when(brandService.update(eq(99L), any(BrandDTO.class), any(MultipartFile.class)))
                .thenThrow(new ResourceNotFoundException("Brand not found"));

        mockMvc.perform(multipart("/brand/{id}", 99L)
                        .file(dataPart(request))
                        .file(picPart())
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Brand not found"));

        verify(brandService).update(eq(99L), any(BrandDTO.class), any(MultipartFile.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePlain_whenAdminAndValidRequest_shouldReturn200() throws Exception {
        BrandDTO request = brandDTO("Puma", true);
        BrandResponseDTO response = brandResponse(1L, "Puma", true);

        when(brandService.updatePlain(eq(1L), any(BrandDTO.class))).thenReturn(response);

        mockMvc.perform(put("/brand/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Puma"))
                .andExpect(jsonPath("$.active").value(true));

        verify(brandService).updatePlain(eq(1L), any(BrandDTO.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePlain_whenNameIsBlank_shouldReturn400() throws Exception {
        BrandDTO request = brandDTO("", true);

        mockMvc.perform(put("/brand/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Brand name is required"));

        verifyNoInteractions(brandService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void updatePlain_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(put("/brand/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(brandDTO("Nike", true))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(brandService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenAdminAndBrandExists_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/brand/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Brand deleted successfully"));

        verify(brandService).delete(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenBrandDoesNotExist_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Brand not found"))
                .when(brandService).delete(99L);

        mockMvc.perform(delete("/brand/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Brand not found"));

        verify(brandService).delete(99L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenProductsStillUseBrand_shouldReturn409() throws Exception {
        doThrow(new ConflictException("Cannot delete brand because products still use it"))
                .when(brandService).delete(1L);

        mockMvc.perform(delete("/brand/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot delete brand because products still use it"));

        verify(brandService).delete(1L);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void delete_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/brand/{id}", 1L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(brandService);
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .exceptionHandling(exception -> exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.GET, "/brand", "/brand/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/brand", "/brand/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.PUT, "/brand", "/brand/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.DELETE, "/brand", "/brand/**").hasRole("ADMIN")
                            .anyRequest().permitAll())
                    .build();
        }
    }
}