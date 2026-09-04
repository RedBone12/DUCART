package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.entities.Cart;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.security.JwtService;
import com.prashant.api.ecom.ducart.services.CartService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import({ GlobalExceptionHandler.class, CartControllerTest.TestSecurityConfig.class })
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtService jwtService;

    private Cart cart(Long id, String user, String product, Integer qty) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setUser(user);
        cart.setProduct(product);
        cart.setName(product);
        cart.setBrand("Apple");
        cart.setColor("Black");
        cart.setSize("128GB");
        cart.setPrice(90.0);
        cart.setStockQuantity(10);
        cart.setPic("phone.jpg");
        cart.setQty(qty);
        cart.setTotal(90.0 * qty);
        return cart;
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAllCartItems_whenAdmin_shouldReturn200() throws Exception {
        when(cartService.getAllCartItems())
                .thenReturn(List.of(
                        cart(1L, "buyer1", "Phone", 2),
                        cart(2L, "buyer2", "Laptop", 1)));

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].user").value("buyer1"))
                .andExpect(jsonPath("$[0].product").value("Phone"))
                .andExpect(jsonPath("$[0].qty").value(2))
                .andExpect(jsonPath("$[1].product").value("Laptop"));

        verify(cartService).getAllCartItems();
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void getAllCartItems_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cartService);
    }

    @Test
    void getAllCartItems_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void getMyCart_whenLoggedIn_shouldReturn200() throws Exception {
        when(cartService.getCartItemsByUser("buyer"))
                .thenReturn(List.of(cart(1L, "buyer", "Phone", 2)));

        mockMvc.perform(get("/cart/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].user").value("buyer"))
                .andExpect(jsonPath("$[0].product").value("Phone"))
                .andExpect(jsonPath("$[0].qty").value(2))
                .andExpect(jsonPath("$[0].total").value(180.0));

        verify(cartService).getCartItemsByUser("buyer");
    }

    @Test
    void getMyCart_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(get("/cart/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void createCartItem_whenLoggedIn_shouldReturn201() throws Exception {
        Cart request = cart(null, null, "Phone", 2);
        Cart saved = cart(1L, "buyer", "Phone", 2);

        when(cartService.createCartItem(any(Cart.class), eq("buyer")))
                .thenReturn(saved);

        mockMvc.perform(post("/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.user").value("buyer"))
                .andExpect(jsonPath("$.product").value("Phone"))
                .andExpect(jsonPath("$.qty").value(2))
                .andExpect(jsonPath("$.total").value(180.0));

        verify(cartService).createCartItem(any(Cart.class), eq("buyer"));
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void createCartItem_whenServiceThrowsBadRequest_shouldReturn400() throws Exception {
        Cart request = cart(null, null, "", 2);

        when(cartService.createCartItem(any(Cart.class), eq("buyer")))
                .thenThrow(new BadRequestException("Product is required"));

        mockMvc.perform(post("/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Product is required"));

        verify(cartService).createCartItem(any(Cart.class), eq("buyer"));
    }

    @Test
    void createCartItem_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(post("/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cart(null, null, "Phone", 2))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void updateCartItem_whenBuyer_shouldPassAdminFalseAndReturn200() throws Exception {
        Cart request = cart(null, null, "Phone", 3);
        Cart updated = cart(1L, "buyer", "Phone", 3);

        when(cartService.updateCartItem(eq(1L), any(Cart.class), eq("buyer"), eq(false)))
                .thenReturn(updated);

        mockMvc.perform(put("/cart/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.user").value("buyer"))
                .andExpect(jsonPath("$.qty").value(3))
                .andExpect(jsonPath("$.total").value(270.0));

        verify(cartService).updateCartItem(eq(1L), any(Cart.class), eq("buyer"), eq(false));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateCartItem_whenAdmin_shouldPassAdminTrueAndReturn200() throws Exception {
        Cart request = cart(null, null, "Phone", 4);
        Cart updated = cart(1L, "buyer", "Phone", 4);

        when(cartService.updateCartItem(eq(1L), any(Cart.class), eq("admin"), eq(true)))
                .thenReturn(updated);

        mockMvc.perform(put("/cart/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.user").value("buyer"))
                .andExpect(jsonPath("$.qty").value(4));

        verify(cartService).updateCartItem(eq(1L), any(Cart.class), eq("admin"), eq(true));
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void updateCartItem_whenProductDoesNotExist_shouldReturn404() throws Exception {
        when(cartService.updateCartItem(eq(99L), any(Cart.class), eq("buyer"), eq(false)))
                .thenThrow(new ResourceNotFoundException("Cart item not found"));

        mockMvc.perform(put("/cart/{id}", 99L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cart(null, null, "Phone", 2))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Cart item not found"));

        verify(cartService).updateCartItem(eq(99L), any(Cart.class), eq("buyer"), eq(false));
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void updateCartItem_whenServiceThrowsAccessDenied_shouldReturn403() throws Exception {
        when(cartService.updateCartItem(eq(1L), any(Cart.class), eq("buyer"), eq(false)))
                .thenThrow(new AccessDeniedException("You do not have permission to access this cart item"));

        mockMvc.perform(put("/cart/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cart(null, null, "Phone", 2))))
                .andExpect(status().isForbidden());

        verify(cartService).updateCartItem(eq(1L), any(Cart.class), eq("buyer"), eq(false));
    }

    @Test
    void updateCartItem_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(put("/cart/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cart(null, null, "Phone", 2))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void deleteCartItem_whenBuyer_shouldPassAdminFalseAndReturn200() throws Exception {
        doNothing().when(cartService).deleteCartItem(1L, "buyer", false);

        mockMvc.perform(delete("/cart/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart item deleted successfully"));

        verify(cartService).deleteCartItem(1L, "buyer", false);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteCartItem_whenAdmin_shouldPassAdminTrueAndReturn200() throws Exception {
        doNothing().when(cartService).deleteCartItem(1L, "admin", true);

        mockMvc.perform(delete("/cart/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart item deleted successfully"));

        verify(cartService).deleteCartItem(1L, "admin", true);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void deleteCartItem_whenProductDoesNotExist_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Cart item not found"))
                .when(cartService).deleteCartItem(99L, "buyer", false);

        mockMvc.perform(delete("/cart/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Cart item not found"));

        verify(cartService).deleteCartItem(99L, "buyer", false);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void deleteCartItem_whenServiceThrowsAccessDenied_shouldReturn403() throws Exception {
        doThrow(new AccessDeniedException("You do not have permission to access this cart item"))
                .when(cartService).deleteCartItem(1L, "buyer", false);

        mockMvc.perform(delete("/cart/{id}", 1L))
                .andExpect(status().isForbidden());

        verify(cartService).deleteCartItem(1L, "buyer", false);
    }

    @Test
    void deleteCartItem_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(delete("/cart/{id}", 1L))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(cartService);
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
                            .requestMatchers(HttpMethod.GET, "/cart").hasRole("ADMIN")
                            .requestMatchers("/cart", "/cart/**").authenticated()
                            .anyRequest().permitAll())
                    .build();
        }
    }
}