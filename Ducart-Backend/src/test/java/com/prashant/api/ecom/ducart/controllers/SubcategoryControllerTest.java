package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.SubcategoryDTO;
import com.prashant.api.ecom.ducart.modal.SubcategoryResponseDTO;
import com.prashant.api.ecom.ducart.security.JwtService;
import com.prashant.api.ecom.ducart.services.SubcategoryService;

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

@WebMvcTest(SubcategoryController.class)
@Import({ GlobalExceptionHandler.class, SubcategoryControllerTest.TestSecurityConfig.class })
class SubcategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubcategoryService subcategoryService;

    @MockitoBean
    private JwtService jwtService;

    /*
     * 这里测试 SubcategoryController 层：GET 是否公开、Admin 才能创建/更新/删除、multipart data 是否能转成
     * SubcategoryDTO、
     * DTO 校验是否生效，以及 SubcategoryService 抛出的 404/409 是否能变成正确 HTTP status。
     */
    private SubcategoryDTO subcategoryDTO(String name, Boolean active) {
        SubcategoryDTO dto = new SubcategoryDTO();
        dto.setName(name);
        dto.setActive(active);
        return dto;
    }

    private SubcategoryResponseDTO subcategoryResponse(Long id, String name, boolean active) {
        return SubcategoryResponseDTO.builder()
                .id(id)
                .name(name)
                .pic("/uploads/subcategories/" + name.toLowerCase().replace(" ", "-") + ".jpg")
                .active(active)
                .build();
    }

    private MockMultipartFile dataPart(SubcategoryDTO dto) throws Exception {
        return new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto));
    }

    private MockMultipartFile picPart() {
        return new MockMultipartFile(
                "pic",
                "phones.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        when(subcategoryService.findAll()).thenReturn(List.of(
                subcategoryResponse(1L, "Phones", true),
                subcategoryResponse(2L, "Shoes", false)));

        mockMvc.perform(get("/subcategory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Phones"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Shoes"))
                .andExpect(jsonPath("$[1].active").value(false));

        verify(subcategoryService).findAll();
    }

    @Test
    void getById_whenSubcategoryExists_shouldReturn200() throws Exception {
        when(subcategoryService.findById(1L))
                .thenReturn(subcategoryResponse(1L, "Phones", true));

        mockMvc.perform(get("/subcategory/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Phones"))
                .andExpect(jsonPath("$.pic").value("/uploads/subcategories/phones.jpg"))
                .andExpect(jsonPath("$.active").value(true));

        verify(subcategoryService).findById(1L);
    }

    @Test
    void getById_whenSubcategoryDoesNotExist_shouldReturn404() throws Exception {
        when(subcategoryService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Subcategory not found"));

        mockMvc.perform(get("/subcategory/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Subcategory not found"));

        verify(subcategoryService).findById(99L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_whenAdminAndValidRequest_shouldReturn201() throws Exception {
        SubcategoryDTO request = subcategoryDTO("Phones", true);
        SubcategoryResponseDTO response = subcategoryResponse(1L, "Phones", true);

        when(subcategoryService.create(any(SubcategoryDTO.class), any(MultipartFile.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/subcategory")
                .file(dataPart(request))
                .file(picPart()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Phones"))
                .andExpect(jsonPath("$.pic").value("/uploads/subcategories/phones.jpg"))
                .andExpect(jsonPath("$.active").value(true));

        verify(subcategoryService).create(any(SubcategoryDTO.class), any(MultipartFile.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_whenNameIsBlank_shouldReturn400() throws Exception {
        SubcategoryDTO request = subcategoryDTO("", true);

        mockMvc.perform(multipart("/subcategory")
                .file(dataPart(request))
                .file(picPart()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Subcategory name is required"));

        verifyNoInteractions(subcategoryService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_whenJsonIsInvalid_shouldReturn400() throws Exception {
        MockMultipartFile badData = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{bad-json}".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/subcategory")
                .file(badData)
                .file(picPart()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid subcategory data"));

        verifyNoInteractions(subcategoryService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(multipart("/subcategory")
                .file(dataPart(subcategoryDTO("Phones", true)))
                .file(picPart()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subcategoryService);
    }

    @Test
    void create_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(multipart("/subcategory")
                .file(dataPart(subcategoryDTO("Phones", true)))
                .file(picPart()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(subcategoryService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateMultipart_whenAdminAndValidRequest_shouldReturn200() throws Exception {
        SubcategoryDTO request = subcategoryDTO("Updated Phones", false);
        SubcategoryResponseDTO response = subcategoryResponse(1L, "Updated Phones", false);

        when(subcategoryService.update(eq(1L), any(SubcategoryDTO.class), any(MultipartFile.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/subcategory/{id}", 1L)
                .file(dataPart(request))
                .file(picPart())
                .with(req -> {
                    req.setMethod("PUT");
                    return req;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Phones"))
                .andExpect(jsonPath("$.active").value(false));

        verify(subcategoryService).update(eq(1L), any(SubcategoryDTO.class), any(MultipartFile.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateMultipart_whenSubcategoryDoesNotExist_shouldReturn404() throws Exception {
        SubcategoryDTO request = subcategoryDTO("Updated Phones", false);

        when(subcategoryService.update(eq(99L), any(SubcategoryDTO.class), any(MultipartFile.class)))
                .thenThrow(new ResourceNotFoundException("Subcategory not found"));

        mockMvc.perform(multipart("/subcategory/{id}", 99L)
                .file(dataPart(request))
                .file(picPart())
                .with(req -> {
                    req.setMethod("PUT");
                    return req;
                }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Subcategory not found"));

        verify(subcategoryService).update(eq(99L), any(SubcategoryDTO.class), any(MultipartFile.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePlain_whenAdminAndValidRequest_shouldReturn200() throws Exception {
        SubcategoryDTO request = subcategoryDTO("Shoes", true);
        SubcategoryResponseDTO response = subcategoryResponse(1L, "Shoes", true);

        when(subcategoryService.updatePlain(eq(1L), any(SubcategoryDTO.class)))
                .thenReturn(response);

        mockMvc.perform(put("/subcategory/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Shoes"))
                .andExpect(jsonPath("$.active").value(true));

        verify(subcategoryService).updatePlain(eq(1L), any(SubcategoryDTO.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePlain_whenNameIsBlank_shouldReturn400() throws Exception {
        SubcategoryDTO request = subcategoryDTO("", true);

        mockMvc.perform(put("/subcategory/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Subcategory name is required"));

        verifyNoInteractions(subcategoryService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void updatePlain_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(put("/subcategory/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subcategoryDTO("Phones", true))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subcategoryService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenAdminAndSubcategoryExists_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/subcategory/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Subcategory deleted successfully"));

        verify(subcategoryService).delete(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenSubcategoryDoesNotExist_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Subcategory not found"))
                .when(subcategoryService).delete(99L);

        mockMvc.perform(delete("/subcategory/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Subcategory not found"));

        verify(subcategoryService).delete(99L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenProductsStillUseSubcategory_shouldReturn409() throws Exception {
        doThrow(new ConflictException("Cannot delete subcategory because products still use it"))
                .when(subcategoryService).delete(1L);

        mockMvc.perform(delete("/subcategory/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot delete subcategory because products still use it"));

        verify(subcategoryService).delete(1L);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void delete_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/subcategory/{id}", 1L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subcategoryService);
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
                            .requestMatchers(HttpMethod.GET, "/subcategory", "/subcategory/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/subcategory", "/subcategory/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.PUT, "/subcategory", "/subcategory/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.DELETE, "/subcategory", "/subcategory/**").hasRole("ADMIN")
                            .anyRequest().permitAll())
                    .build();
        }
    }
}