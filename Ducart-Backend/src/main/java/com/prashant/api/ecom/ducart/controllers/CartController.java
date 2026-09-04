package com.prashant.api.ecom.ducart.controllers;

import com.prashant.api.ecom.ducart.entities.Cart;
import com.prashant.api.ecom.ducart.services.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Cart>> getAllCartItems() {
        return ResponseEntity.ok(cartService.getAllCartItems());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Cart>> getMyCart(Authentication authentication) {
        return ResponseEntity.ok(cartService.getCartItemsByUser(authentication.getName()));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Cart> createCartItem(@RequestBody Cart cart, Authentication authentication) {
        Cart saved = cartService.createCartItem(cart, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Cart> updateCartItem(@PathVariable Long id,
            @RequestBody Cart cart,
            Authentication authentication) {
        Cart updated = cartService.updateCartItem(id, cart, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> deleteCartItem(@PathVariable Long id,
            Authentication authentication) {
        cartService.deleteCartItem(id, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.ok(Map.of("message", "Cart item deleted successfully"));
    }
}