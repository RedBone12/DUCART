package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Cart;
import com.prashant.api.ecom.ducart.entities.Product;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.repositories.CartRepo;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepo cartRepo;

    @Mock
    private ProductRepo productRepo;

    @InjectMocks
    private CartService cartService;

    private Product product() {
        Product product = new Product();
        product.setName("Phone");
        product.setBrand("Apple");
        product.setColor("Black");
        product.setSize("128GB");
        product.setFinalPrice(900.0);
        product.setStockQuantity(5);
        product.setPics(List.of("/uploads/products/phone.jpg"));
        return product;
    }

    private Cart cartItem() {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUser("buyer1");
        cart.setProduct("Phone");
        cart.setName("Phone");
        cart.setBrand("Apple");
        cart.setColor("Black");
        cart.setSize("128GB");
        cart.setPrice(900.0);
        cart.setStockQuantity(5);
        cart.setPic("/uploads/products/phone.jpg");
        cart.setQty(1);
        cart.setTotal(900.0);
        return cart;
    }

    @Test
    void getAllCartItems_shouldReturnAllCartItems() {
        Cart cart = cartItem();

        when(cartRepo.findAll()).thenReturn(List.of(cart));

        List<Cart> result = cartService.getAllCartItems();

        assertEquals(1, result.size());
        assertEquals("Phone", result.get(0).getName());
        verify(cartRepo).findAll();
    }

    @Test
    void getCartItemsByUser_shouldReturnUserCartItems() {
        Cart cart = cartItem();

        when(cartRepo.findByUser("buyer1")).thenReturn(List.of(cart));

        List<Cart> result = cartService.getCartItemsByUser("buyer1");

        assertEquals(1, result.size());
        assertEquals("buyer1", result.get(0).getUser());
        verify(cartRepo).findByUser("buyer1");
    }

    @Test
    void createCartItem_whenNewProduct_shouldSaveCartItem() {
        Cart cart = new Cart();
        cart.setProduct("Phone");
        cart.setQty(2);

        Product product = product();

        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product));
        when(cartRepo.findByUserAndProduct("buyer1", "Phone")).thenReturn(Optional.empty());
        when(cartRepo.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.createCartItem(cart, "buyer1");

        assertEquals("buyer1", result.getUser());
        assertEquals("Phone", result.getProduct());
        assertEquals("Apple", result.getBrand());
        assertEquals("Black", result.getColor());
        assertEquals("128GB", result.getSize());
        assertEquals(900.0, result.getPrice(), 0.001);
        assertEquals(2, result.getQty());
        assertEquals(1800.0, result.getTotal(), 0.001);
        assertEquals("/uploads/products/phone.jpg", result.getPic());

        verify(cartRepo).save(cart);
    }

    @Test
    void createCartItem_whenProductAlreadyInCart_shouldIncreaseQuantity() {
        Cart incoming = new Cart();
        incoming.setProduct("Phone");
        incoming.setQty(2);

        Cart existing = cartItem();
        existing.setQty(1);

        Product product = product();

        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product));
        when(cartRepo.findByUserAndProduct("buyer1", "Phone")).thenReturn(Optional.of(existing));
        when(cartRepo.save(existing)).thenReturn(existing);

        Cart result = cartService.createCartItem(incoming, "buyer1");

        assertEquals(3, result.getQty());
        assertEquals(2700.0, result.getTotal(), 0.001);

        verify(cartRepo).save(existing);
    }

    @Test
    void createCartItem_whenQuantityIsZero_shouldThrowBadRequestException() {
        Cart cart = new Cart();
        cart.setProduct("Phone");
        cart.setQty(0);

        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product()));

        assertThrows(BadRequestException.class, () -> cartService.createCartItem(cart, "buyer1"));

        verify(cartRepo, never()).save(any(Cart.class));
    }

    @Test
    void createCartItem_whenProductIsNullOrBlank_shouldThrowBadRequestException() {
        Cart nullProduct = new Cart();
        nullProduct.setQty(1);
        Cart blankProduct = new Cart();
        blankProduct.setProduct("   ");
        blankProduct.setQty(1);

        assertThrows(BadRequestException.class,
                () -> cartService.createCartItem(nullProduct, "buyer1"));
        assertThrows(BadRequestException.class,
                () -> cartService.createCartItem(blankProduct, "buyer1"));

        verify(productRepo, never()).findByName(any());
    }

    @Test
    void createCartItem_whenQuantityIsNull_shouldThrowBadRequestException() {
        Cart cart = new Cart();
        cart.setProduct("Phone");
        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product()));

        assertThrows(BadRequestException.class,
                () -> cartService.createCartItem(cart, "buyer1"));

        verify(cartRepo, never()).save(any(Cart.class));
    }

    @Test
    void createCartItem_whenProductDoesNotExist_shouldThrowResourceNotFoundException() {
        Cart cart = new Cart();
        cart.setProduct("Phone");
        cart.setQty(1);

        when(productRepo.findByName("Phone")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.createCartItem(cart, "buyer1"));

        verify(cartRepo, never()).save(any(Cart.class));
    }

    @Test
    void createCartItem_whenQuantityExceedsStock_shouldThrowBadRequestException() {
        Cart cart = new Cart();
        cart.setProduct("Phone");
        cart.setQty(10);

        Product product = product();

        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product));
        when(cartRepo.findByUserAndProduct("buyer1", "Phone")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> cartService.createCartItem(cart, "buyer1"));

        verify(cartRepo, never()).save(any(Cart.class));
    }

    @Test
    void createCartItem_whenStockAndPricesAndPicsAreMissing_shouldUseSafeDefaults() {
        Cart cart = new Cart();
        cart.setProduct("Phone");
        cart.setQty(1);
        Product product = product();
        product.setStockQuantity(null);
        product.setFinalPrice(null);
        product.setBasePrice(null);
        product.setPics(null);
        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product));
        when(cartRepo.findByUserAndProduct("buyer1", "Phone")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> cartService.createCartItem(cart, "buyer1"));

        verify(cartRepo, never()).save(any(Cart.class));
    }

    @Test
    void createCartItem_whenFinalPriceIsMissing_shouldUseBasePriceAndIgnoreEmptyPics() {
        Cart cart = new Cart();
        cart.setProduct("Phone");
        cart.setQty(1);
        Product product = product();
        product.setFinalPrice(null);
        product.setBasePrice(750.0);
        product.setPics(List.of());
        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product));
        when(cartRepo.findByUserAndProduct("buyer1", "Phone")).thenReturn(Optional.empty());
        when(cartRepo.save(cart)).thenReturn(cart);

        Cart result = cartService.createCartItem(cart, "buyer1");

        assertEquals(750.0, result.getPrice(), 0.001);
        assertEquals(750.0, result.getTotal(), 0.001);
        assertNull(result.getPic());
    }

    @Test
    void createCartItem_whenBothPricesAndPicsAreNull_shouldUseZeroPriceAndNoPic() {
        Cart cart = new Cart();
        cart.setProduct("Phone");
        cart.setQty(1);
        Product product = product();
        product.setFinalPrice(null);
        product.setBasePrice(null);
        product.setPics(null);
        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product));
        when(cartRepo.findByUserAndProduct("buyer1", "Phone")).thenReturn(Optional.empty());
        when(cartRepo.save(cart)).thenReturn(cart);

        Cart result = cartService.createCartItem(cart, "buyer1");

        assertEquals(0.0, result.getPrice(), 0.001);
        assertEquals(0.0, result.getTotal(), 0.001);
        assertNull(result.getPic());
    }

    @Test
    void createCartItem_whenExistingQuantityWouldExceedStock_shouldRejectMerge() {
        Cart incoming = new Cart();
        incoming.setProduct("Phone");
        incoming.setQty(5);
        Cart existing = cartItem();
        existing.setQty(1);
        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product()));
        when(cartRepo.findByUserAndProduct("buyer1", "Phone")).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class,
                () -> cartService.createCartItem(incoming, "buyer1"));

        verify(cartRepo, never()).save(any(Cart.class));
    }

    @Test
    void updateCartItem_whenUserOwnsCartItem_shouldUpdateQuantityAndTotal() {
        Cart existing = cartItem();

        Cart incoming = new Cart();
        incoming.setQty(3);

        Product product = product();

        when(cartRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product));
        when(cartRepo.save(existing)).thenReturn(existing);

        Cart result = cartService.updateCartItem(1L, incoming, "buyer1", false);

        assertEquals(3, result.getQty());
        assertEquals(2700.0, result.getTotal(), 0.001);

        verify(cartRepo).save(existing);
    }

    @Test
    void updateCartItem_whenUserDoesNotOwnCartItem_shouldThrowAccessDeniedException() {
        Cart existing = cartItem();

        Cart incoming = new Cart();
        incoming.setQty(2);

        when(cartRepo.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(
                AccessDeniedException.class,
                () -> cartService.updateCartItem(1L, incoming, "buyer2", false)
        );

        verify(cartRepo, never()).save(any(Cart.class));
    }

    @Test
    void updateCartItem_whenCartItemDoesNotExist_shouldThrowResourceNotFoundException() {
        Cart incoming = new Cart();
        incoming.setQty(2);

        when(cartRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.updateCartItem(99L, incoming, "buyer1", false)
        );

        verify(cartRepo, never()).save(any(Cart.class));
    }

    @Test
    void updateCartItem_whenAdminEditsAnotherUsersCart_shouldAllowUpdate() {
        Cart existing = cartItem();
        Cart incoming = new Cart();
        incoming.setQty(2);
        when(cartRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product()));
        when(cartRepo.save(existing)).thenReturn(existing);

        Cart result = cartService.updateCartItem(1L, incoming, "admin", true);

        assertEquals(2, result.getQty());
        verify(cartRepo).save(existing);
    }

    @Test
    void updateCartItem_whenQuantityExceedsStock_shouldRejectUpdate() {
        Cart existing = cartItem();
        Cart incoming = new Cart();
        incoming.setQty(6);
        when(cartRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.findByName("Phone")).thenReturn(Optional.of(product()));

        assertThrows(BadRequestException.class,
                () -> cartService.updateCartItem(1L, incoming, "buyer1", false));

        verify(cartRepo, never()).save(any(Cart.class));
    }

    @Test
    void deleteCartItem_whenUserOwnsCartItem_shouldDeleteCartItem() {
        Cart existing = cartItem();

        when(cartRepo.findById(1L)).thenReturn(Optional.of(existing));

        cartService.deleteCartItem(1L, "buyer1", false);

        verify(cartRepo).deleteById(1L);
    }

    @Test
    void deleteCartItem_whenUserDoesNotOwnCartItem_shouldThrowAccessDeniedException() {
        Cart existing = cartItem();

        when(cartRepo.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(
                AccessDeniedException.class,
                () -> cartService.deleteCartItem(1L, "buyer2", false)
        );

        verify(cartRepo, never()).deleteById(1L);
    }

    @Test
    void deleteCartItem_whenAdminDeletesAnotherUsersCart_shouldAllowDelete() {
        when(cartRepo.findById(1L)).thenReturn(Optional.of(cartItem()));

        cartService.deleteCartItem(1L, "admin", true);

        verify(cartRepo).deleteById(1L);
    }

    @Test
    void deleteCartItem_whenCartDoesNotExist_shouldThrowNotFound() {
        when(cartRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.deleteCartItem(99L, "buyer1", false));

        verify(cartRepo, never()).deleteById(99L);
    }
}
