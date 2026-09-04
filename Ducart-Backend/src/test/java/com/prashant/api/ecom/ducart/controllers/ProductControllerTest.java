package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.ProductDTO;
import com.prashant.api.ecom.ducart.modal.ProductResponseDTO;
import com.prashant.api.ecom.ducart.modal.ProductStockUpdateDTO;
import com.prashant.api.ecom.ducart.services.ProductService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.prashant.api.ecom.ducart.security.JwtService;

@WebMvcTest(ProductController.class)
@Import({ GlobalExceptionHandler.class, ProductControllerTest.TestSecurityConfig.class })
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    private ProductDTO productDTO(String name) {
        ProductDTO dto = new ProductDTO();
        dto.setName(name);
        dto.setMaincategory("Electronics");
        dto.setSubcategory("Mobile");
        dto.setBrand("Apple");
        dto.setColor("Black");
        dto.setSize("128GB");
        dto.setBasePrice(100.0);
        dto.setDiscount(10.0);
        dto.setStock(true);
        dto.setDescription("Test product");
        dto.setStockQuantity(5);
        dto.setActive(true);
        return dto;
    }

    private ProductResponseDTO productResponse(Long id, String name) {
        return ProductResponseDTO.builder()
                .id(id)
                .name(name)
                .maincategory("Electronics")
                .subcategory("Mobile")
                .brand("Apple")
                .color("Black")
                .size("128GB")
                .basePrice(100.0)
                .discount(10.0)
                .finalPrice(90.0)
                .stock(true)
                .description("Test product")
                .stockQuantity(5)
                .pics(List.of("phone.jpg"))
                .active(true)
                .build();
    }

    private MockMultipartFile dataPart(ProductDTO dto) throws Exception {
        return new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto));
    }

    private MockMultipartFile imagePart() {
        return new MockMultipartFile(
                "pic",
                "phone.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void getAllProducts_shouldReturn200() throws Exception {
        when(productService.findAll()).thenReturn(List.of(
                productResponse(1L, "Phone"),
                productResponse(2L, "Laptop")));

        mockMvc.perform(get("/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Phone"))
                .andExpect(jsonPath("$[0].finalPrice").value(90.0))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Laptop"));

        verify(productService).findAll();
    }

    @Test
    void getProductById_whenProductExists_shouldReturn200() throws Exception {
        when(productService.findById(1L)).thenReturn(productResponse(1L, "Phone"));

        mockMvc.perform(get("/product/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Phone"))
                .andExpect(jsonPath("$.brand").value("Apple"))
                .andExpect(jsonPath("$.stock").value(true))
                .andExpect(jsonPath("$.stockQuantity").value(5));

        verify(productService).findById(1L);
    }

    @Test
    void getProductById_whenProductDoesNotExist_shouldReturn404() throws Exception {
        when(productService.findById(99L)).thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/product/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found"));

        verify(productService).findById(99L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_whenAdminAndValidRequest_shouldReturn201() throws Exception {
        ProductDTO dto = productDTO("Phone");
        ProductResponseDTO response = productResponse(1L, "Phone");

        when(productService.create(any(ProductDTO.class), any(MultipartFile[].class))).thenReturn(response);

        mockMvc.perform(multipart("/product")
                .file(dataPart(dto))
                .file(imagePart()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Phone"))
                .andExpect(jsonPath("$.finalPrice").value(90.0))
                .andExpect(jsonPath("$.pics[0]").value("phone.jpg"));

        verify(productService).create(any(ProductDTO.class), any(MultipartFile[].class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_whenNameIsBlank_shouldReturn400() throws Exception {
        ProductDTO dto = productDTO("");

        mockMvc.perform(multipart("/product")
                .file(dataPart(dto))
                .file(imagePart()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Product name is required"));

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_whenJsonIsInvalid_shouldReturn400() throws Exception {
        MockMultipartFile badData = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{bad-json}".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/product")
                .file(badData)
                .file(imagePart()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid product data"));

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void createProduct_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(multipart("/product")
                .file(dataPart(productDTO("Phone")))
                .file(imagePart()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(multipart("/product")
                .file(dataPart(productDTO("Phone")))
                .file(imagePart()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProductById_whenAdminAndValidRequest_shouldReturn200() throws Exception {
        ProductDTO dto = productDTO("Updated Phone");
        ProductResponseDTO response = productResponse(1L, "Updated Phone");

        when(productService.update(eq(1L), any(ProductDTO.class), any(MultipartFile[].class))).thenReturn(response);

        mockMvc.perform(multipart("/product/{id}", 1L)
                .file(dataPart(dto))
                .file(imagePart())
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Phone"))
                .andExpect(jsonPath("$.brand").value("Apple"));

        verify(productService).update(eq(1L), any(ProductDTO.class), any(MultipartFile[].class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProductById_whenProductDoesNotExist_shouldReturn404() throws Exception {
        ProductDTO dto = productDTO("Updated Phone");

        when(productService.update(eq(99L), any(ProductDTO.class), any(MultipartFile[].class)))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(multipart("/product/{id}", 99L)
                .file(dataPart(dto))
                .file(imagePart())
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found"));

        verify(productService).update(eq(99L), any(ProductDTO.class), any(MultipartFile[].class));
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void updateProductById_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(multipart("/product/{id}", 1L)
                .file(dataPart(productDTO("Updated Phone")))
                .file(imagePart())
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProductStock_whenAdminAndValidRequest_shouldReturn200() throws Exception {
        ProductStockUpdateDTO stockDTO = new ProductStockUpdateDTO(0, true);
        ProductResponseDTO response = productResponse(1L, "Phone");
        response.setStockQuantity(0);
        response.setStock(false);

        when(productService.updateStock(eq(1L), any(ProductStockUpdateDTO.class))).thenReturn(response);

        mockMvc.perform(put("/product/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stockQuantity").value(0))
                .andExpect(jsonPath("$.stock").value(false));

        verify(productService).updateStock(eq(1L), any(ProductStockUpdateDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProductStock_whenStockQuantityIsNegative_shouldReturn400() throws Exception {
        ProductStockUpdateDTO stockDTO = new ProductStockUpdateDTO(-1, true);

        mockMvc.perform(put("/product/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockDTO)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).updateStock(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProductStock_whenProductDoesNotExist_shouldReturn404() throws Exception {
        ProductStockUpdateDTO stockDTO = new ProductStockUpdateDTO(5, true);

        when(productService.updateStock(eq(99L), any(ProductStockUpdateDTO.class)))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(put("/product/{id}", 99L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found"));

        verify(productService).updateStock(eq(99L), any(ProductStockUpdateDTO.class));
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void updateProductStock_whenBuyer_shouldReturn403() throws Exception {
        ProductStockUpdateDTO stockDTO = new ProductStockUpdateDTO(5, true);

        mockMvc.perform(put("/product/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockDTO)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteById_whenAdminAndProductExists_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/product/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product deleted successfully"));

        verify(productService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteById_whenProductDoesNotExist_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found")).when(productService).delete(99L);

        mockMvc.perform(delete("/product/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found"));

        verify(productService).delete(99L);
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void deleteById_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/product/{id}", 1L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
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
                            .requestMatchers(HttpMethod.GET, "/product", "/product/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/product", "/product/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.PUT, "/product", "/product/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.DELETE, "/product", "/product/**").hasRole("ADMIN")
                            .anyRequest().permitAll())
                    .build();
        }
    }
}