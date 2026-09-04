package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Product;
import com.prashant.api.ecom.ducart.entities.Wishlist;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import com.prashant.api.ecom.ducart.repositories.WishlistRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock WishlistRepo wishlistRepo;
    @Mock ProductRepo productRepo;
    @InjectMocks WishlistService wishlistService;

    @Test
    void getMethods_shouldDelegateToRepository() {
        Wishlist item = wishlist(1L, "alice", "Phone");
        when(wishlistRepo.findAll()).thenReturn(List.of(item));
        when(wishlistRepo.findByUser("alice")).thenReturn(List.of(item));

        assertEquals(List.of(item), wishlistService.getAllWishlistItems());
        assertEquals(List.of(item), wishlistService.getWishlistItemsByUser("alice"));
    }

    @Test
    void create_shouldIgnoreClientIdentityAndCopyAuthoritativeProductData() {
        Product product = product("Phone", 100.0, 80.0, List.of("front.jpg", "back.jpg"));
        Wishlist request = wishlist(99L, "mallory", "Phone");
        request.setPrice(1.0);
        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product));
        when(wishlistRepo.existsByUserAndProduct("alice", "Phone")).thenReturn(false);
        when(wishlistRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Wishlist saved = wishlistService.createWishlistItem(request, "alice");

        assertNull(saved.getId());
        assertEquals("alice", saved.getUser());
        assertEquals("Phone", saved.getName());
        assertEquals("Apple", saved.getBrand());
        assertEquals(80.0, saved.getPrice());
        assertEquals("front.jpg", saved.getPic());
        assertEquals(7, saved.getStockQuantity());
    }

    @Test
    void create_whenFinalPriceAndPicturesAreMissing_shouldUseBasePriceAndNullPicture() {
        Product product = product("Phone", 100.0, null, List.of());
        Wishlist request = wishlist(null, null, "Phone");
        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product));
        when(wishlistRepo.existsByUserAndProduct("alice", "Phone")).thenReturn(false);
        when(wishlistRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Wishlist saved = wishlistService.createWishlistItem(request, "alice");

        assertEquals(100.0, saved.getPrice());
        assertNull(saved.getPic());
    }

    @Test
    void create_whenProductNameMissing_shouldRejectRequest() {
        Wishlist request = wishlist(null, null, "  ");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> wishlistService.createWishlistItem(request, "alice"));

        assertEquals("Product is required", exception.getMessage());
        verifyNoInteractions(productRepo, wishlistRepo);
    }

    @Test
    void create_whenProductNameIsNull_shouldRejectRequest() {
        Wishlist request = wishlist(null, null, null);

        assertThrows(BadRequestException.class,
                () -> wishlistService.createWishlistItem(request, "alice"));

        verifyNoInteractions(productRepo, wishlistRepo);
    }

    @Test
    void create_whenPicturesAndBothPricesAreNull_shouldUseZeroAndNullPicture() {
        Product product = product("Phone", null, null, null);
        Wishlist request = wishlist(null, null, "Phone");
        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product));
        when(wishlistRepo.existsByUserAndProduct("alice", "Phone")).thenReturn(false);
        when(wishlistRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Wishlist saved = wishlistService.createWishlistItem(request, "alice");

        assertEquals(0.0, saved.getPrice());
        assertNull(saved.getPic());
    }

    @Test
    void create_whenProductDoesNotExist_shouldReturnNotFound() {
        Wishlist request = wishlist(null, null, "Missing");
        when(productRepo.findByName("Missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> wishlistService.createWishlistItem(request, "alice"));
        verify(wishlistRepo, never()).save(any());
    }

    @Test
    void create_whenDuplicate_shouldReturnConflict() {
        Wishlist request = wishlist(null, null, "Phone");
        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product("Phone", 1.0, 1.0, null)));
        when(wishlistRepo.existsByUserAndProduct("alice", "Phone")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> wishlistService.createWishlistItem(request, "alice"));
        verify(wishlistRepo, never()).save(any());
    }

    @Test
    void delete_shouldAllowOwnerAndAdminButRejectOtherBuyer() {
        Wishlist item = wishlist(1L, "alice", "Phone");
        when(wishlistRepo.findById(1L)).thenReturn(Optional.of(item));

        wishlistService.deleteWishlistItem(1L, "alice", false);
        verify(wishlistRepo).deleteById(1L);

        reset(wishlistRepo);
        when(wishlistRepo.findById(1L)).thenReturn(Optional.of(item));
        wishlistService.deleteWishlistItem(1L, "admin", true);
        verify(wishlistRepo).deleteById(1L);

        reset(wishlistRepo);
        when(wishlistRepo.findById(1L)).thenReturn(Optional.of(item));
        assertThrows(AccessDeniedException.class,
                () -> wishlistService.deleteWishlistItem(1L, "bob", false));
        verify(wishlistRepo, never()).deleteById(anyLong());
    }

    @Test
    void delete_whenMissing_shouldReturnNotFound() {
        when(wishlistRepo.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> wishlistService.deleteWishlistItem(9L, "alice", false));
    }

    private Wishlist wishlist(Long id, String user, String product) {
        Wishlist item = new Wishlist();
        item.setId(id);
        item.setUser(user);
        item.setProduct(product);
        return item;
    }

    private Product product(String name, Double basePrice, Double finalPrice, List<String> pics) {
        Product product = new Product();
        product.setName(name);
        product.setBrand("Apple");
        product.setColor("Black");
        product.setSize("128GB");
        product.setBasePrice(basePrice);
        product.setFinalPrice(finalPrice);
        product.setStockQuantity(7);
        product.setPics(pics);
        return product;
    }
}
