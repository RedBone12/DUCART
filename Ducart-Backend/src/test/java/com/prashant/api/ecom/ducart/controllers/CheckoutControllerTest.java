package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.entities.Cart;
import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.CheckoutDTO;
import com.prashant.api.ecom.ducart.security.JwtService;
import com.prashant.api.ecom.ducart.services.CheckoutService;

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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutController.class)
@Import({ GlobalExceptionHandler.class, CheckoutControllerTest.TestSecurityConfig.class })
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CheckoutService checkoutService;

    @MockitoBean
    private JwtService jwtService;

    /*
     * 这里测试 CheckoutController 层：权限、路径、当前登录用户是否写入 dto.user、Controller 自己的下单校验，以及
     * Service 抛出的 404 是否能正确返回。
     * 真正的实体转换、productsJson 存取和数据库操作属于 CheckoutServiceTest，因为 CheckoutService 负责把
     * CheckoutDTO 和 Checkout 实体互相转换。
     */
    private Cart cartItem(String product, Integer qty, Integer stockQuantity) {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUser("buyer");
        cart.setProduct(product);
        cart.setName(product);
        cart.setBrand("Apple");
        cart.setColor("Black");
        cart.setSize("128GB");
        cart.setPrice(90.0);
        cart.setQty(qty);
        cart.setStockQuantity(stockQuantity);
        cart.setTotal(qty == null ? null : 90.0 * qty);
        cart.setPic("phone.jpg");
        return cart;
    }

    private CheckoutDTO checkoutDTO(Long id, String user) {
        CheckoutDTO dto = new CheckoutDTO();
        dto.setId(id);
        dto.setUser(user);
        dto.setOrderStatus("Pending");
        dto.setPaymentMode("COD");
        dto.setPaymentStatus("Pending");
        dto.setSubtotal(180.0);
        dto.setShipping(10.0);
        dto.setTotal(190.0);
        dto.setDate("2026-07-14T10:00:00");
        dto.setRppid("rpp_test_123");
        dto.setProducts(List.of(cartItem("Phone", 2, 10)));
        return dto;
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAll_whenAdmin_shouldReturn200() throws Exception {
        when(checkoutService.findAll()).thenReturn(List.of(
                checkoutDTO(1L, "buyer1"),
                checkoutDTO(2L, "buyer2")));

        mockMvc.perform(get("/checkout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].user").value("buyer1"))
                .andExpect(jsonPath("$[0].paymentMode").value("COD"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].user").value("buyer2"));

        verify(checkoutService).findAll();
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void getAll_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(get("/checkout"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(checkoutService);
    }

    @Test
    void getAll_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(get("/checkout"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void getMyOrders_whenLoggedIn_shouldReturn200() throws Exception {
        when(checkoutService.findByUser("buyer"))
                .thenReturn(List.of(checkoutDTO(1L, "buyer")));

        mockMvc.perform(get("/checkout/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].user").value("buyer"))
                .andExpect(jsonPath("$[0].products[0].product").value("Phone"))
                .andExpect(jsonPath("$[0].total").value(190.0));

        verify(checkoutService).findByUser("buyer");
    }

    @Test
    void getMyOrders_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(get("/checkout/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getById_whenAdminAndOrderExists_shouldReturn200() throws Exception {
        when(checkoutService.findById(1L)).thenReturn(checkoutDTO(1L, "buyer"));

        mockMvc.perform(get("/checkout/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.user").value("buyer"))
                .andExpect(jsonPath("$.products[0].product").value("Phone"));

        verify(checkoutService).findById(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getById_whenOrderDoesNotExist_shouldReturn404() throws Exception {
        when(checkoutService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/checkout/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order not found"));

        verify(checkoutService).findById(99L);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void getById_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(get("/checkout/{id}", 1L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenLoggedInAndValidRequest_shouldSetCurrentUserAndReturn201() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "someoneElse");
        CheckoutDTO saved = checkoutDTO(1L, "buyer");

        when(checkoutService.create(any(CheckoutDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.user").value("buyer"))
                .andExpect(jsonPath("$.paymentMode").value("COD"))
                .andExpect(jsonPath("$.products[0].product").value("Phone"));

        ArgumentCaptor<CheckoutDTO> captor = ArgumentCaptor.forClass(CheckoutDTO.class);
        verify(checkoutService).create(captor.capture());
        assertEquals("buyer", captor.getValue().getUser());
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenPaymentModeIsBlank_shouldReturn400() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setPaymentMode("");

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Payment mode is required"));

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenPaymentModeIsNull_shouldReturn400() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setPaymentMode(null);

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payment mode is required"));

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenProductsAreEmpty_shouldReturn400() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setProducts(List.of());

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("At least one product is required"));

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenProductsAreNull_shouldReturn400() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setProducts(null);

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("At least one product is required"));

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenSubtotalIsNegative_shouldReturn400() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setSubtotal(-1.0);

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Subtotal cannot be negative"));

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenShippingIsNegative_shouldReturn400() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setShipping(-1.0);

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Shipping cannot be negative"));

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenOptionalAmountsAreNull_shouldStillCreate() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setSubtotal(null);
        request.setShipping(null);
        CheckoutDTO saved = checkoutDTO(1L, "buyer");
        when(checkoutService.create(any(CheckoutDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(checkoutService).create(any(CheckoutDTO.class));
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenTotalIsNegative_shouldReturn400() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setTotal(-1.0);

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Valid total is required"));

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenTotalIsNull_shouldReturn400() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setTotal(null);

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valid total is required"));

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenQtyIsGreaterThanStock_shouldReturn400() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setProducts(List.of(cartItem("Phone", 20, 10)));

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Requested quantity exceeds available stock"));

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenQtyIsNull_shouldReturn400() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setProducts(List.of(cartItem("Phone", null, 10)));

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Quantity must be greater than 0"));

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenStockQuantityIsUnknown_shouldAllowValidQuantity() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setProducts(List.of(cartItem("Phone", 2, null)));
        CheckoutDTO saved = checkoutDTO(1L, "buyer");
        when(checkoutService.create(any(CheckoutDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(checkoutService).create(any(CheckoutDTO.class));
    }

    @Test
    void create_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(post("/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(checkoutDTO(null, "buyer"))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_whenAdminAndValidRequest_shouldReturn200() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        CheckoutDTO updated = checkoutDTO(1L, "buyer");
        updated.setOrderStatus("Shipped");

        when(checkoutService.update(eq(1L), any(CheckoutDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/checkout/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderStatus").value("Shipped"))
                .andExpect(jsonPath("$.user").value("buyer"));

        verify(checkoutService).update(eq(1L), any(CheckoutDTO.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_whenOrderDoesNotExist_shouldReturn404() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");

        when(checkoutService.update(eq(99L), any(CheckoutDTO.class)))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(put("/checkout/{id}", 99L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order not found"));

        verify(checkoutService).update(eq(99L), any(CheckoutDTO.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_whenQuantityIsInvalid_shouldReturn400() throws Exception {
        CheckoutDTO request = checkoutDTO(null, "buyer");
        request.setProducts(List.of(cartItem("Phone", 0, 10)));

        mockMvc.perform(put("/checkout/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Quantity must be greater than 0"));

        verify(checkoutService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void update_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(put("/checkout/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(checkoutDTO(null, "buyer"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(checkoutService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenAdminAndOrderExists_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/checkout/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order deleted successfully"));

        verify(checkoutService).delete(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenOrderDoesNotExist_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Order not found"))
                .when(checkoutService).delete(99L);

        mockMvc.perform(delete("/checkout/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order not found"));

        verify(checkoutService).delete(99L);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void delete_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/checkout/{id}", 1L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(checkoutService);
    }

    @Test
    void delete_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(delete("/checkout/{id}", 1L))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(checkoutService);
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
                            .requestMatchers(HttpMethod.GET, "/checkout").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.GET, "/checkout/me").authenticated()
                            .requestMatchers(HttpMethod.GET, "/checkout/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.POST, "/checkout").authenticated()
                            .requestMatchers(HttpMethod.PUT, "/checkout/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.DELETE, "/checkout/**").hasRole("ADMIN")
                            .anyRequest().permitAll())
                    .build();
        }
    }
}
