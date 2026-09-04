package com.prashant.api.ecom.ducart.controllers;

import com.prashant.api.ecom.ducart.entities.Wishlist;
import com.prashant.api.ecom.ducart.services.WishlistService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Wishlist>> getAll() {
        return ResponseEntity.ok(
                wishlistService.getAllWishlistItems());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Wishlist>> getMyWishlist(Authentication authentication) {
        return ResponseEntity.ok(wishlistService.getWishlistItemsByUser(authentication.getName()));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Wishlist> create(
            @RequestBody Wishlist item,
            Authentication authentication) {

        Wishlist saved = wishlistService.createWishlistItem(
                item,
                authentication.getName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Long id,
            Authentication authentication) {

        wishlistService.deleteWishlistItem(
                id,
                authentication.getName(),
                isAdmin(authentication));

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Wishlist item deleted successfully"));
    }
}