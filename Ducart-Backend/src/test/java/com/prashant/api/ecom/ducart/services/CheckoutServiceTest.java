package com.prashant.api.ecom.ducart.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prashant.api.ecom.ducart.modal.CheckoutLineDTO;
import com.prashant.api.ecom.ducart.entities.Checkout;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.CheckoutDTO;
import com.prashant.api.ecom.ducart.repositories.CheckoutRepo;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {
    @Mock
    private CheckoutRepo checkoutRepo;

    @InjectMocks
    private CheckoutService checkoutService;

    private CheckoutLineDTO cartItem() {
        CheckoutLineDTO cart = new CheckoutLineDTO();
        cart.setId(1L);
        cart.setUser("buyer1");
        cart.setProduct("Phone");
        cart.setName("Phone");
        cart.setBrand("Apple");
        cart.setColor("Black");
        cart.setSize("128G");
        cart.setPrice(new java.math.BigDecimal("900.00"));
        cart.setStockQuantity(5);
        cart.setPic("/uploads/products/phone.jpg");
        cart.setQty(2);
        cart.setTotal(new java.math.BigDecimal("1800.00"));
        return cart;
    }

    private CheckoutDTO checkoutDTO() {
        CheckoutDTO dto = new CheckoutDTO();
        dto.setId(1L);
        dto.setUser("buyer1");
        dto.setOrderStatus("Order is Placed");
        dto.setPaymentMode("Cash on Delivery");
        dto.setPaymentStatus("Pending");
        dto.setSubtotal(new java.math.BigDecimal("1800.0"));
        dto.setShipping(new java.math.BigDecimal("0.0"));
        dto.setTotal(new java.math.BigDecimal("1800.0"));
        dto.setDate("2026-06-30T10:00:00");
        dto.setRppid("demo-rppid");
        dto.setProducts(List.of(cartItem()));
        return dto;
    }

    private Checkout checkoutEntity() {
        Checkout checkout = new Checkout();
        checkout.setId(1L);
        checkout.setUser("buyer1");
        checkout.setOrderStatus("Order is Placed");
        checkout.setPaymentMode("Cash on Delivery");
        checkout.setPaymentStatus("Pending");
        checkout.setSubtotal(new java.math.BigDecimal("1800.0"));
        checkout.setShipping(new java.math.BigDecimal("0.0"));
        checkout.setTotal(new java.math.BigDecimal("1800.0"));
        checkout.setDate("2026-06-30T10:00:00");
        checkout.setRppid("demo-rppid");
        checkout.setProductsJson(
                """
                        [{"id":1,"user":"buyer1","product":"Phone","name":"Phone","brand":"Apple","color":"Black","size":"128GB","price":900.0,"stockQuantity":5,"pic":"/uploads/products/phone.jpg","qty":2,"total":1800.0}]
                        """);
        return checkout;
    }

    @Test
    void findAll_shouldReturnAllOrders() {
        Checkout checkout = checkoutEntity();

        when(checkoutRepo.findAll()).thenReturn(List.of(checkout));

        List<CheckoutDTO> result = checkoutService.findAll();

        assertEquals(1, result.size());
        assertEquals("buyer1", result.get(0).getUser());
        assertEquals(1, result.get(0).getProducts().size());
        assertEquals("Phone", result.get(0).getProducts().get(0).getName());
        verify(checkoutRepo).findAll();
    }

    @Test
    void findByUser_shouldReturnOrdersForUser() {
        Checkout checkout = checkoutEntity();

        when(checkoutRepo.findByUser("buyer1")).thenReturn(List.of(checkout));

        List<CheckoutDTO> result = checkoutService.findByUser("buyer1");

        assertEquals(1, result.size());
        assertEquals("buyer1", result.get(0).getUser());
        assertEquals(new java.math.BigDecimal("1800.0"), result.get(0).getTotal());
        verify(checkoutRepo).findByUser("buyer1");
    }

    @Test
    void findAll_whenProductsJsonIsNull_shouldReturnEmptyProducts() {
        Checkout checkout = checkoutEntity();
        checkout.setProductsJson(null);
        when(checkoutRepo.findAll()).thenReturn(List.of(checkout));

        List<CheckoutDTO> result = checkoutService.findAll();

        assertTrue(result.get(0).getProducts().isEmpty());
    }

    @Test
    void findByUser_whenProductsJsonIsBlank_shouldReturnEmptyProducts() {
        Checkout checkout = checkoutEntity();
        checkout.setProductsJson("   ");
        when(checkoutRepo.findByUser("buyer1")).thenReturn(List.of(checkout));

        List<CheckoutDTO> result = checkoutService.findByUser("buyer1");

        assertTrue(result.get(0).getProducts().isEmpty());
    }

    @Test
    void findById_whenOrderExists_shouldReturnOrder() {
        Checkout checkout = checkoutEntity();

        when(checkoutRepo.findById(1L)).thenReturn(Optional.of(checkout));

        CheckoutDTO result = checkoutService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("buyer1", result.getUser());
        assertEquals("Phone", result.getProducts().get(0).getName());
        verify(checkoutRepo).findById(1L);
    }

    @Test
    void findById_whenOrderDoesNotExist_shouldThrowResourceNotFoundException() {
        when(checkoutRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> checkoutService.findById(99L));

        verify(checkoutRepo).findById(99L);
    }

    @Test
    void findById_whenProductsJsonIsInvalid_shouldReturnOrderWithEmptyProducts() {
        Checkout checkout = checkoutEntity();
        checkout.setProductsJson("not-json");
        when(checkoutRepo.findById(1L)).thenReturn(Optional.of(checkout));

        CheckoutDTO result = checkoutService.findById(1L);

        assertEquals("buyer1", result.getUser());
        assertTrue(result.getProducts().isEmpty());
    }

    @Test
    void update_whenOrderExists_shouldUpdateAndReturnOrder() throws Exception {
        Checkout existing = checkoutEntity();

        CheckoutDTO incoming = checkoutDTO();
        incoming.setOrderStatus("Order is Shipped");
        incoming.setPaymentStatus("Paid");
        incoming.setTotal(new java.math.BigDecimal("1900.0"));

        when(checkoutRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(checkoutRepo.save(existing)).thenReturn(existing);

        CheckoutDTO result = checkoutService.update(1L, incoming);

        assertEquals("Order is Shipped", result.getOrderStatus());
        assertEquals("Paid", result.getPaymentStatus());
        assertEquals(new java.math.BigDecimal("1900.0"), result.getTotal());
        verify(checkoutRepo).findById(1L);
        verify(checkoutRepo).save(existing);
    }

    @Test
    void update_whenOrderDoesNotExist_shouldThrowResourceNotFoundException() {
        CheckoutDTO incoming = checkoutDTO();

        when(checkoutRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> checkoutService.update(99L, incoming));

        verify(checkoutRepo).findById(99L);
        verify(checkoutRepo, never()).save(any(Checkout.class));
    }

    @Test
    void update_whenDateIsNull_shouldGenerateDate() throws Exception {
        Checkout existing = checkoutEntity();
        CheckoutDTO incoming = checkoutDTO();
        incoming.setDate(null);
        when(checkoutRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(checkoutRepo.save(existing)).thenReturn(existing);

        CheckoutDTO result = checkoutService.update(1L, incoming);

        assertNotNull(result.getDate());
        assertFalse(result.getDate().isBlank());
    }

    @Test
    void delete_whenOrderExists_shouldDeleteOrder() {
        when(checkoutRepo.existsById(1L)).thenReturn(true);

        checkoutService.delete(1L);

        verify(checkoutRepo).existsById(1L);
        verify(checkoutRepo).deleteById(1L);
    }

    @Test
    void delete_whenOrderDoesNotExist_shouldThrowResourceNotFoundException() {
        when(checkoutRepo.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> checkoutService.delete(99L));

        verify(checkoutRepo).existsById(99L);
        verify(checkoutRepo, never()).deleteById(99L);
    }
}
