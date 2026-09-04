package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Cart;
import com.prashant.api.ecom.ducart.entities.Product;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.repositories.CartRepo;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {
    private final CartRepo cartRepo;
    private final ProductRepo productRepo;

    public CartService(CartRepo cartRepo, ProductRepo productRepo) {
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
    }

    public List<Cart> getAllCartItems() {
        return cartRepo.findAll();
    }

    public List<Cart> getCartItemsByUser(String user) {
        return cartRepo.findByUser(user);
    }

    public Cart createCartItem(Cart cart, String username) {
        Product product = requireProduct(cart);
        validateQuantity(cart.getQty());

        Integer stockQuantity = Optional.ofNullable(product.getStockQuantity()).orElse(0);

        Cart itemToSave = cartRepo.findByUserAndProduct(username, product.getName())
                .map(existing -> {
                    int newQty = existing.getQty() + cart.getQty();
                    validateStock(newQty, stockQuantity);
                    existing.setQty(newQty);
                    applyProductDetails(existing, product);
                    return existing;
                })
                .orElseGet(() -> {
                    validateStock(cart.getQty(), stockQuantity);
                    cart.setUser(username);
                    cart.setProduct(product.getName());
                    applyProductDetails(cart, product);
                    return cart;
                });

        return cartRepo.save(itemToSave);
    }

    public Cart updateCartItem(Long id, Cart cart, String username, boolean admin) {
        Cart existing = cartRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        checkPermission(existing, username, admin);
        validateQuantity(cart.getQty());

        Product product = requireProduct(existing);
        Integer stockQuantity = Optional.ofNullable(product.getStockQuantity()).orElse(0);

        validateStock(cart.getQty(), stockQuantity);

        existing.setQty(cart.getQty());
        applyProductDetails(existing, product);

        return cartRepo.save(existing);
    }

    public void deleteCartItem(Long id, String username, boolean admin) {
        Cart existing = cartRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        checkPermission(existing, username, admin);
        cartRepo.deleteById(id);
    }

    private Product requireProduct(Cart cart) {
        if (cart.getProduct() == null || cart.getProduct().isBlank()) {
            throw new BadRequestException("Product is required");
        }

        return productRepo.findByName(cart.getProduct())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private void validateQuantity(Integer qty) {
        if (qty == null || qty <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }
    }

    private void validateStock(Integer qty, Integer stockQuantity) {
        if (qty > stockQuantity) {
            throw new BadRequestException("Requested quantity exceeds available stock");
        }
    }

    private void checkPermission(Cart cart, String username, boolean admin) {
        if (!admin && !cart.getUser().equals(username)) {
            throw new AccessDeniedException("You do not have permission to access this cart item");
        }
    }

    private void applyProductDetails(Cart cart, Product product) {
        Double price = Optional.ofNullable(product.getFinalPrice())
                .orElse(Optional.ofNullable(product.getBasePrice()).orElse(0.0));

        cart.setProduct(product.getName());
        cart.setName(product.getName());
        cart.setBrand(product.getBrand());
        cart.setColor(product.getColor());
        cart.setSize(product.getSize());
        cart.setPrice(price);
        cart.setStockQuantity(product.getStockQuantity());
        cart.setTotal(price * cart.getQty());

        if (product.getPics() != null && !product.getPics().isEmpty()) {
            cart.setPic(product.getPics().get(0));
        }
    }
}