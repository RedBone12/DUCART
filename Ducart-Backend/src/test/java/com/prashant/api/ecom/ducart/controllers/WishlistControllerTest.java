package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.entities.Wishlist;
import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.security.JwtService;
import com.prashant.api.ecom.ducart.services.WishlistService;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishlistController.class)
@Import({GlobalExceptionHandler.class, WishlistControllerTest.TestSecurityConfig.class})
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WishlistService wishlistService;

    @MockitoBean
    private JwtService jwtService;

    /*
     * WishlistController 当前直接调用 WishlistRepo。
     * 这里测试权限、当前用户写入、本人收藏查询和删除权限。
     */
    private Wishlist wishlist(Long id, String user, String product) {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(id);
        wishlist.setUser(user);
        wishlist.setProduct(product);
        wishlist.setName(product);
        wishlist.setBrand("Apple");
        wishlist.setColor("Black");
        wishlist.setSize("128GB");
        wishlist.setPrice(90.0);
        wishlist.setStockQuantity(10);
        wishlist.setPic("phone.jpg");
        return wishlist;
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAll_whenAdmin_shouldReturn200() throws Exception {
        when(wishlistService.getAllWishlistItems()).thenReturn(List.of(
                wishlist(1L, "buyer1", "Phone"),
                wishlist(2L, "buyer2", "Laptop")));

        mockMvc.perform(get("/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].user").value("buyer1"))
                .andExpect(jsonPath("$[0].product").value("Phone"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].user").value("buyer2"))
                .andExpect(jsonPath("$[1].product").value("Laptop"));

        verify(wishlistService).getAllWishlistItems();
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void getAll_whenBuyer_shouldReturn403() throws Exception {
        mockMvc.perform(get("/wishlist")).andExpect(status().isForbidden());
        verifyNoInteractions(wishlistService);
    }

    @Test
    void getAll_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(get("/wishlist")).andExpect(status().isUnauthorized());
        verifyNoInteractions(wishlistService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void getMyWishlist_whenLoggedIn_shouldReturn200() throws Exception {
        when(wishlistService.getWishlistItemsByUser("buyer"))
                .thenReturn(List.of(wishlist(1L, "buyer", "Phone")));

        mockMvc.perform(get("/wishlist/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].user").value("buyer"))
                .andExpect(jsonPath("$[0].product").value("Phone"))
                .andExpect(jsonPath("$[0].price").value(90.0));

        verify(wishlistService).getWishlistItemsByUser("buyer");
    }

    @Test
    void getMyWishlist_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(get("/wishlist/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(wishlistService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void create_whenLoggedIn_shouldPassCurrentUserAndReturn201() throws Exception {
        Wishlist request = wishlist(null, "someoneElse", "Phone");
        Wishlist saved = wishlist(1L, "buyer", "Phone");

        when(wishlistService.createWishlistItem(any(Wishlist.class), eq("buyer")))
                .thenReturn(saved);

        mockMvc.perform(post("/wishlist").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.user").value("buyer"))
                .andExpect(jsonPath("$.product").value("Phone"))
                .andExpect(jsonPath("$.brand").value("Apple"))
                .andExpect(jsonPath("$.price").value(90.0));

        verify(wishlistService).createWishlistItem(any(Wishlist.class), eq("buyer"));
    }

    @Test
    void create_whenNotLoggedIn_shouldReturn401() throws Exception {
        Wishlist request = wishlist(null, null, "Phone");

        mockMvc.perform(post("/wishlist").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(wishlistService);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void delete_whenOwner_shouldReturn200() throws Exception {
        doNothing().when(wishlistService).deleteWishlistItem(1L, "buyer", false);

        mockMvc.perform(delete("/wishlist/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Wishlist item deleted successfully"));

        verify(wishlistService).deleteWishlistItem(1L, "buyer", false);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_whenAdmin_shouldReturn200EvenIfNotOwner() throws Exception {
        doNothing().when(wishlistService).deleteWishlistItem(1L, "admin", true);

        mockMvc.perform(delete("/wishlist/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Wishlist item deleted successfully"));

        verify(wishlistService).deleteWishlistItem(1L, "admin", true);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void delete_whenItemDoesNotExist_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Wishlist item not found"))
                .when(wishlistService).deleteWishlistItem(99L, "buyer", false);

        mockMvc.perform(delete("/wishlist/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Wishlist item not found"));

        verify(wishlistService).deleteWishlistItem(99L, "buyer", false);
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void delete_whenNotOwner_shouldReturn403() throws Exception {
        doThrow(new AccessDeniedException("You do not have permission to delete this wishlist item"))
                .when(wishlistService).deleteWishlistItem(1L, "buyer", false);

        mockMvc.perform(delete("/wishlist/{id}", 1L)).andExpect(status().isForbidden());

        verify(wishlistService).deleteWishlistItem(1L, "buyer", false);
    }

    @Test
    void delete_whenNotLoggedIn_shouldReturn401() throws Exception {
        mockMvc.perform(delete("/wishlist/{id}", 1L)).andExpect(status().isUnauthorized());
        verifyNoInteractions(wishlistService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .exceptionHandling(exception -> exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.GET, "/wishlist").hasRole("ADMIN")
                            .requestMatchers("/wishlist", "/wishlist/**").authenticated()
                            .anyRequest().permitAll())
                    .build();
        }
    }
}
