package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.MainResponseDTO;
import com.prashant.api.ecom.ducart.modal.MaincategoryDTO;
import com.prashant.api.ecom.ducart.security.JwtService;
import com.prashant.api.ecom.ducart.services.MaincategoryService;

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

@WebMvcTest(MaincategoryController.class)
@Import({ GlobalExceptionHandler.class, MaincategoryControllerTest.TestSecurityConfig.class })
class MaincategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MaincategoryService maincategoryService;

    @MockitoBean
    private JwtService jwtService;

    /*
     * 这里测试 MaincategoryController 层：GET 是否公开、Admin 才能创建/更新/删除、multipart data 是否能转成
     * MaincategoryDTO、
     * DTO 校验是否生效，以及 MaincategoryService 抛出的 404/409 是否能变成正确 HTTP status。
     */
    private MaincategoryDTO maincategoryDTO(String name, Boolean active) {
        MaincategoryDTO dto = new MaincategoryDTO();
        dto.setName(name);
        dto.setActive(active);
        return dto;
    }

    private MainResponseDTO mainResponse(Long id, String name, boolean active) {
        return MainResponseDTO.builder()
                .id(id)
                .name(name)
                .pic("/uploads/maincategories/" + name.toLowerCase().replace(" ", "-") + ".jpg")
                .active(active)
                .build();
    }

    private MockMultipartFile dataPart(MaincategoryDTO dto) throws Exception {
        return new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto));
    }

    private MockMultipartFile picPart() {
        return new MockMultipartFile(
                "pic",
                "electronics.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        when(maincategoryService.findAll()).thenReturn(List.of(
                mainResponse(1L, "Electronics", true),
                mainResponse(2L, "Fashion", false)));

        mockMvc.perform(get("/maincategory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Fashion"))
                .andExpect(jsonPath("$[1].active").value(false));

        verify(maincategoryService).findAll();
    }

    @Test
    void getById_whenMaincategoryExists_shouldReturn200() throws Exception {
        when(maincategoryService.findById(1L))
                .thenReturn(mainResponse(1L, "Electronics", true));

        mockMvc.perform(get("/maincategory/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.pic").value("/uploads/maincategories/electronics.jpg"))
                .andExpect(jsonPath("$.active").value(true));

        verify(maincategoryService).findById(1L);
    }

    @Test
    void getById_whenMaincategoryDoesNotExist_shouldReturn404() throws Exception {
        when(maincategoryService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Main category not found"));

        mockMvc.perform(get("/maincategory/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Main category not found"));

        verify(maincategoryService).findById(99L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_whenAdminAndValidRequest_shouldReturn201() throws Exception {
        MaincategoryDTO request = maincategoryDTO("Electronics", true);
        MainResponseDTO response = mainResponse(1L, "Electronics", true);

        when(maincategoryService.create(any(MaincategoryDTO.class), any(MultipartFile.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/maincategory")
                .file(dataPart(request))
                .file(picPart()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.pic").value("/uploads/maincategories/electronics.jpg"))
                .andExpect(jsonPath("$.active").value(true));

        verify(maincategoryService).create(any(MaincategoryDTO.class), any(MultipartFile.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_whenNameIsBlank_shouldReturn400() throws Exception {
        MaincategoryDTO request = maincategoryDTO("", true);

        mockMvc.perform(multipart("/maincategory")
                .file(dataPart(request))
                .file(picPart()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Main category name is required"));

        verifyNoInteractions(maincategoryService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_whenJsonIsInvalid_shouldReturn400() throws Exception {
        MockMultipartFile badData = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{bad-json}".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/maincategory")
                .file(badData)
                .file(picPart()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid main category data"));

        verifyNoInteractions(maincategoryService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(multipart("/maincategory")
                .file(dataPart(maincategoryDTO("Electronics", true)))
                .file(picPart()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(maincategoryService);
    }

    @Test
    void create_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(multipart("/maincategory")
                .file(dataPart(maincategoryDTO("Electronics", true)))
                .file(picPart()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(maincategoryService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateMultipart_whenAdminAndValidRequest_shouldReturn200() throws Exception {
        MaincategoryDTO request = maincategoryDTO("Updated Electronics", false);
        MainResponseDTO response = mainResponse(1L, "Updated Electronics", false);

        when(maincategoryService.update(eq(1L), any(MaincategoryDTO.class), any(MultipartFile.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/maincategory/{id}", 1L)
                .file(dataPart(request))
                .file(picPart())
                .with(req -> {
                    req.setMethod("PUT");
                    return req;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Electronics"))
                .andExpect(jsonPath("$.active").value(false));

        verify(maincategoryService).update(eq(1L), any(MaincategoryDTO.class), any(MultipartFile.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateMultipart_whenMaincategoryDoesNotExist_shouldReturn404() throws Exception {
        MaincategoryDTO request = maincategoryDTO("Updated Electronics", false);

        when(maincategoryService.update(eq(99L), any(MaincategoryDTO.class), any(MultipartFile.class)))
                .thenThrow(new ResourceNotFoundException("Main category not found"));

        mockMvc.perform(multipart("/maincategory/{id}", 99L)
                .file(dataPart(request))
                .file(picPart())
                .with(req -> {
                    req.setMethod("PUT");
                    return req;
                }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Main category not found"));

        verify(maincategoryService).update(eq(99L), any(MaincategoryDTO.class), any(MultipartFile.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePlain_whenAdminAndValidRequest_shouldReturn200() throws Exception {
        MaincategoryDTO request = maincategoryDTO("Fashion", true);
        MainResponseDTO response = mainResponse(1L, "Fashion", true);

        when(maincategoryService.updatePlain(eq(1L), any(MaincategoryDTO.class)))
                .thenReturn(response);

        mockMvc.perform(put("/maincategory/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Fashion"))
                .andExpect(jsonPath("$.active").value(true));

        verify(maincategoryService).updatePlain(eq(1L), any(MaincategoryDTO.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePlain_whenNameIsBlank_shouldReturn400() throws Exception {
        MaincategoryDTO request = maincategoryDTO("", true);

        mockMvc.perform(put("/maincategory/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Main category name is required"));

        verifyNoInteractions(maincategoryService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void updatePlain_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(put("/maincategory/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(maincategoryDTO("Electronics", true))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(maincategoryService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenAdminAndMaincategoryExists_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/maincategory/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Main category deleted successfully"));

        verify(maincategoryService).delete(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenMaincategoryDoesNotExist_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Main category not found"))
                .when(maincategoryService).delete(99L);

        mockMvc.perform(delete("/maincategory/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Main category not found"));

        verify(maincategoryService).delete(99L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenProductsStillUseMaincategory_shouldReturn409() throws Exception {
        doThrow(new ConflictException("Cannot delete main category because products still use it"))
                .when(maincategoryService).delete(1L);

        mockMvc.perform(delete("/maincategory/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot delete main category because products still use it"));

        verify(maincategoryService).delete(1L);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void delete_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/maincategory/{id}", 1L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(maincategoryService);
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
                            .requestMatchers(HttpMethod.GET, "/maincategory", "/maincategory/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/maincategory", "/maincategory/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.PUT, "/maincategory", "/maincategory/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.DELETE, "/maincategory", "/maincategory/**").hasRole("ADMIN")
                            .anyRequest().permitAll())
                    .build();
        }
    }
}