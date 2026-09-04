package com.prashant.api.ecom.ducart.controllers;

import com.prashant.api.ecom.ducart.entities.Cart;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.modal.CheckoutDTO;
import com.prashant.api.ecom.ducart.services.CheckoutService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    private void validateCheckout(CheckoutDTO dto) {
        if (dto.getPaymentMode() == null || dto.getPaymentMode().isBlank()) {
            throw new BadRequestException("Payment mode is required");
        }

        if (dto.getProducts() == null || dto.getProducts().isEmpty()) {
            throw new BadRequestException("At least one product is required");
        }

        if (dto.getSubtotal() != null && dto.getSubtotal() < 0) {
            throw new BadRequestException("Subtotal cannot be negative");
        }

        if (dto.getShipping() != null && dto.getShipping() < 0) {
            throw new BadRequestException("Shipping cannot be negative");
        }

        if (dto.getTotal() == null || dto.getTotal() < 0) {
            throw new BadRequestException("Valid total is required");
        }

        for (Cart item : dto.getProducts()) {
            if (item.getQty() == null || item.getQty() <= 0) {
                throw new BadRequestException("Quantity must be greater than 0");
            }

            if (item.getStockQuantity() != null && item.getQty() > item.getStockQuantity()) {
                throw new BadRequestException("Requested quantity exceeds available stock");
            }
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CheckoutDTO>> getAll() {
        return ResponseEntity.ok(checkoutService.findAll());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CheckoutDTO>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(checkoutService.findByUser(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CheckoutDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(checkoutService.findById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CheckoutDTO> create(@RequestBody CheckoutDTO dto,
            Authentication authentication) throws Exception {
        dto.setUser(authentication.getName());
        validateCheckout(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CheckoutDTO> update(@PathVariable Long id,
            @RequestBody CheckoutDTO dto) throws Exception {
        validateCheckout(dto);
        return ResponseEntity.ok(checkoutService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        checkoutService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Order deleted successfully"));
    }
}