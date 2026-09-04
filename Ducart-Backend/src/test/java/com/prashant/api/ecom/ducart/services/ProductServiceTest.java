package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Product;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.ProductDTO;
import com.prashant.api.ecom.ducart.modal.ProductResponseDTO;
import com.prashant.api.ecom.ducart.modal.ProductStockUpdateDTO;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepo productRepo;

    @InjectMocks
    private ProductService productService;

    private ProductDTO validProductDTO() {
        ProductDTO productDTO = new ProductDTO();

        productDTO.setName("Phone");
        productDTO.setMaincategory("Electronics");
        productDTO.setSubcategory("Mobile");
        productDTO.setBrand("Apple");
        productDTO.setColor("Black");
        productDTO.setSize("128GB");
        productDTO.setBasePrice(100.0);
        productDTO.setDiscount(10.0);
        productDTO.setStock(true);
        productDTO.setStockQuantity(5);
        productDTO.setActive(true);
        productDTO.setDescription("Test product");

        return productDTO;
    }

    private Product validProductEntity() {
        Product product = new Product();

        product.setId(1L);
        product.setName("Phone");
        product.setMaincategory("Electronics");
        product.setSubcategory("Mobile");
        product.setBrand("Apple");
        product.setColor("Black");
        product.setSize("128GB");
        product.setBasePrice(100.0);
        product.setDiscount(10.0);
        product.setFinalPrice(90.0);
        product.setStockQuantity(5);
        product.setStock(true);
        product.setActive(true);
        product.setDescription("Test product");
        product.setPics(List.of("old-phone.jpg"));

        return product;
    }

    private MultipartFile[] noFiles() {
        return new MultipartFile[0];
    }

    private void assertInvalidProduct(Consumer<ProductDTO> change, String expectedMessage) {
        ProductDTO dto = validProductDTO();
        change.accept(dto);

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> productService.create(dto, noFiles()));

        assertEquals(expectedMessage, error.getMessage());
    }

    @Test
    void create_whenProductIsValid_shouldSaveProductWithCalculatedFinalPrice() throws Exception {
        ProductDTO productDTO = validProductDTO();

        when(productRepo.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product savedProduct = invocation.getArgument(0);
                    savedProduct.setId(1L);
                    return savedProduct;
                });

        ProductResponseDTO result = productService.create(productDTO, noFiles());

        assertEquals(1L, result.getId());
        assertEquals("Phone", result.getName());
        assertEquals("Electronics", result.getMaincategory());
        assertEquals("Mobile", result.getSubcategory());
        assertEquals("Apple", result.getBrand());
        assertEquals("Black", result.getColor());
        assertEquals("128GB", result.getSize());
        assertEquals(100.0, result.getBasePrice(), 0.001);
        assertEquals(10.0, result.getDiscount(), 0.001);
        assertEquals(90.0, result.getFinalPrice(), 0.001);
        assertTrue(result.getStock());
        assertEquals(5, result.getStockQuantity());
        assertTrue(result.isActive());

        verify(productRepo).save(any(Product.class));
    }

    @Test
    void create_whenProductNameIsBlank_shouldThrowBadRequestException() {
        ProductDTO productDTO = validProductDTO();
        productDTO.setName("");

        assertThrows(
                BadRequestException.class,
                () -> productService.create(productDTO, noFiles()));

        verify(productRepo, never()).save(any(Product.class));
    }

    @Test
    void create_whenBasePriceIsNegative_shouldThrowBadRequestException() {
        ProductDTO productDTO = validProductDTO();
        productDTO.setBasePrice(-1.0);

        assertThrows(
                BadRequestException.class,
                () -> productService.create(productDTO, noFiles()));

        verify(productRepo, never()).save(any(Product.class));
    }

    @Test
    void create_whenDiscountIsGreaterThan100_shouldThrowBadRequestException() {
        ProductDTO productDTO = validProductDTO();
        productDTO.setDiscount(150.0);

        assertThrows(
                BadRequestException.class,
                () -> productService.create(productDTO, noFiles()));

        verify(productRepo, never()).save(any(Product.class));
    }

    @Test
    void create_whenStockQuantityIsNegative_shouldThrowBadRequestException() {
        ProductDTO productDTO = validProductDTO();
        productDTO.setStockQuantity(-1);

        assertThrows(
                BadRequestException.class,
                () -> productService.create(productDTO, noFiles()));

        verify(productRepo, never()).save(any(Product.class));
    }

    @Test
    void create_shouldRejectEveryMissingOrBlankRequiredField() {
        BadRequestException nullProduct = assertThrows(
                BadRequestException.class,
                () -> productService.create(null, noFiles()));
        assertEquals("Product data is required", nullProduct.getMessage());

        assertInvalidProduct(dto -> dto.setName(null), "Product name is required");
        assertInvalidProduct(dto -> dto.setName("   "), "Product name is required");
        assertInvalidProduct(dto -> dto.setMaincategory(null), "Main category is required");
        assertInvalidProduct(dto -> dto.setMaincategory(" "), "Main category is required");
        assertInvalidProduct(dto -> dto.setSubcategory(null), "Subcategory is required");
        assertInvalidProduct(dto -> dto.setSubcategory(" "), "Subcategory is required");
        assertInvalidProduct(dto -> dto.setBrand(null), "Brand is required");
        assertInvalidProduct(dto -> dto.setBrand(" "), "Brand is required");
        assertInvalidProduct(dto -> dto.setColor(null), "Color is required");
        assertInvalidProduct(dto -> dto.setColor(" "), "Color is required");
        assertInvalidProduct(dto -> dto.setSize(null), "Size is required");
        assertInvalidProduct(dto -> dto.setSize(" "), "Size is required");
        assertInvalidProduct(dto -> dto.setBasePrice(null), "Base price must be 0 or greater");
        assertInvalidProduct(dto -> dto.setDiscount(null), "Discount must be between 0 and 100");
        assertInvalidProduct(dto -> dto.setDiscount(-0.01), "Discount must be between 0 and 100");
        assertInvalidProduct(dto -> dto.setDescription(null), "Description is required");
        assertInvalidProduct(dto -> dto.setDescription(" "), "Description is required");
        assertInvalidProduct(dto -> dto.setStockQuantity(null), "Stock quantity cannot be negative");

        verify(productRepo, never()).save(any(Product.class));
    }

    @Test
    void create_shouldDeriveStockAndActiveDefaultsWhenFlagsAreOmitted() throws Exception {
        ProductDTO dto = validProductDTO();
        dto.setStock(null);
        dto.setActive(null);
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO positiveStock = productService.create(dto, null);

        assertTrue(positiveStock.getStock());
        assertTrue(positiveStock.isActive());

        dto.setStockQuantity(0);
        ProductResponseDTO zeroStock = productService.create(dto, noFiles());
        assertFalse(zeroStock.getStock());
    }

    @Test
    void create_whenStockExistsButManualFlagIsFalse_shouldKeepProductUnavailable() throws Exception {
        ProductDTO dto = validProductDTO();
        dto.setStockQuantity(5);
        dto.setStock(false);
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO result = productService.create(dto, noFiles());

        assertFalse(result.getStock());
        assertEquals(5, result.getStockQuantity());
    }

    @Test
    void create_shouldSaveARealMultipartPictureAndReturnItsPublicPath() throws Exception {
        MockMultipartFile picture = new MockMultipartFile(
                "pic", "phone image.png", "image/png", new byte[] {1, 2, 3});
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO result = productService.create(
                validProductDTO(),
                new MultipartFile[] {picture});

        assertEquals(1, result.getPics().size());
        assertTrue(result.getPics().get(0).matches("/uploads/products/\\d+_phone_image.png"));

        Path savedPicture = Paths.get(
                "uploads", "products",
                result.getPics().get(0).substring(result.getPics().get(0).lastIndexOf('/') + 1));
        try {
            assertTrue(Files.exists(savedPicture));
        } finally {
            Files.deleteIfExists(savedPicture);
        }
    }

  @Test
    void update_whenProductExists_shouldUpdateAndSaveProduct() throws Exception {
        Product existing = validProductEntity();
        existing.setName("Old Phone");
        existing.setPics(List.of("old-phone.jpg"));

        ProductDTO incoming = validProductDTO();
        incoming.setName("New Phone");
        incoming.setBasePrice(200.0);
        incoming.setDiscount(25.0);
        incoming.setStockQuantity(0);
        incoming.setStock(true);

        when(productRepo.findById(1L)).thenReturn(Optional.of(existing));

        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO result = productService.update(1L, incoming, noFiles());

        assertEquals("New Phone", result.getName());
        assertEquals(200.0, result.getBasePrice(), 0.001);
        assertEquals(25.0, result.getDiscount(), 0.001);
        assertEquals(150.0, result.getFinalPrice(), 0.001);
        assertFalse(result.getStock());
        assertEquals(0, result.getStockQuantity());
        assertEquals(List.of("old-phone.jpg"), result.getPics());

        verify(productRepo).findById(1L);
        verify(productRepo).save(existing);
    }

    @Test
    void update_whenProductDoesNotExist_shouldThrowResourceNotFoundException() {
        ProductDTO incoming = validProductDTO();

        when(productRepo.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.update(99L, incoming, noFiles()));

        verify(productRepo).findById(99L);
        verify(productRepo, never()).save(any(Product.class));
    }

    @Test
    void update_whenFlagsAreOmitted_shouldPreserveActiveAndDeriveStock() throws Exception {
        Product existing = validProductEntity();
        existing.setActive(false);
        ProductDTO incoming = validProductDTO();
        incoming.setActive(null);
        incoming.setStock(null);
        incoming.setStockQuantity(3);
        when(productRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO result = productService.update(
                1L,
                incoming,
                new MultipartFile[] {new MockMultipartFile("pic", "empty.png", "image/png", new byte[0])});

        assertFalse(result.isActive());
        assertTrue(result.getStock());
        assertEquals(List.of("old-phone.jpg"), result.getPics());
    }

    @Test
    void update_whenNewPictureIsProvided_shouldReplaceOldPictures() throws Exception {
        Product existing = validProductEntity();
        existing.setPics(List.of("old-phone.jpg"));
        ProductDTO incoming = validProductDTO();
        MockMultipartFile picture = new MockMultipartFile(
                "pic", "updated phone.png", "image/png", new byte[] {4, 5, 6});
        when(productRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.save(existing)).thenReturn(existing);

        ProductResponseDTO result = productService.update(
                1L, incoming, new MultipartFile[] {picture});
        Path savedPicture = Paths.get(
                "uploads", "products",
                result.getPics().get(0).substring(result.getPics().get(0).lastIndexOf('/') + 1));

        try {
            assertEquals(1, result.getPics().size());
            assertTrue(result.getPics().get(0).contains("updated_phone.png"));
            assertTrue(Files.exists(savedPicture));
        } finally {
            Files.deleteIfExists(savedPicture);
        }
    }

    @Test
    void updateStock_whenQuantityIsPositive_shouldUpdateStockOnly() {
        Product existing = validProductEntity();
        existing.setName("Phone");
        existing.setStockQuantity(5);
        existing.setStock(true);

        ProductStockUpdateDTO stockDTO = new ProductStockUpdateDTO(10, true);

        when(productRepo.findById(1L))
                .thenReturn(Optional.of(existing));

        when(productRepo.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO result = productService.updateStock(1L, stockDTO);

        assertEquals("Phone", result.getName());
        assertEquals(10, result.getStockQuantity());
        assertTrue(result.getStock());

        verify(productRepo).findById(1L);
        verify(productRepo).save(existing);
    }

    @Test
    void updateStock_whenQuantityIsZero_shouldSetStockFalse() {
        Product existing = validProductEntity();

        ProductStockUpdateDTO stockDTO = new ProductStockUpdateDTO(0, true);

        when(productRepo.findById(1L))
                .thenReturn(Optional.of(existing));

        when(productRepo.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO result = productService.updateStock(1L, stockDTO);

        assertEquals(0, result.getStockQuantity());
        assertFalse(result.getStock());

        verify(productRepo).findById(1L);
        verify(productRepo).save(existing);
    }

    @Test
    void updateStock_whenManualStockIsFalse_shouldSetStockFalse() {
        Product existing = validProductEntity();

        ProductStockUpdateDTO stockDTO = new ProductStockUpdateDTO(10, false);

        when(productRepo.findById(1L))
                .thenReturn(Optional.of(existing));

        when(productRepo.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO result = productService.updateStock(1L, stockDTO);

        assertEquals(10, result.getStockQuantity());
        assertFalse(result.getStock());

        verify(productRepo).findById(1L);
        verify(productRepo).save(existing);
    }

    @Test
    void updateStock_whenProductDoesNotExist_shouldThrowResourceNotFoundException() {
        ProductStockUpdateDTO stockDTO = new ProductStockUpdateDTO(10, true);

        when(productRepo.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateStock(99L, stockDTO));

        verify(productRepo).findById(99L);
        verify(productRepo, never()).save(any(Product.class));
    }

    @Test
    void updateStock_whenStockQuantityIsNegative_shouldThrowBadRequestException() {
        ProductStockUpdateDTO stockDTO = new ProductStockUpdateDTO(-1, true);

        Product existing = validProductEntity();

        when(productRepo.findById(1L))
                .thenReturn(Optional.of(existing));

        assertThrows(
                BadRequestException.class,
                () -> productService.updateStock(1L, stockDTO));

        verify(productRepo).findById(1L);
        verify(productRepo, never()).save(any(Product.class));
    }

    @Test
    void updateStock_whenQuantityIsMissing_shouldThrowBadRequestException() {
        Product existing = validProductEntity();
        when(productRepo.findById(1L)).thenReturn(Optional.of(existing));

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> productService.updateStock(1L, new ProductStockUpdateDTO(null, null)));

        assertEquals("Stock quantity is required", error.getMessage());
        verify(productRepo, never()).save(any(Product.class));
    }

    @Test
    void updateStock_whenStockFlagIsMissing_shouldDeriveAvailabilityFromQuantity() {
        Product existing = validProductEntity();
        when(productRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO available = productService.updateStock(
                1L, new ProductStockUpdateDTO(2, null));
        ProductResponseDTO unavailable = productService.updateStock(
                1L, new ProductStockUpdateDTO(0, null));

        assertTrue(available.getStock());
        assertFalse(unavailable.getStock());
    }

    @Test
    void findAll_shouldReturnProductResponseDTOList() {
        Product product1 = validProductEntity();
        product1.setName("Phone");

        Product product2 = validProductEntity();
        product2.setId(2L);
        product2.setName("Laptop");

        when(productRepo.findAll())
                .thenReturn(List.of(product1, product2));

        List<ProductResponseDTO> result = productService.findAll();

        assertEquals(2, result.size());
        assertEquals("Phone", result.get(0).getName());
        assertEquals("Laptop", result.get(1).getName());

        verify(productRepo).findAll();
    }

    @Test
    void findById_whenProductExists_shouldReturnProductResponseDTO() {
        Product product = validProductEntity();
        product.setName("Phone");

        when(productRepo.findById(1L))
                .thenReturn(Optional.of(product));

        ProductResponseDTO result = productService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Phone", result.getName());
        assertEquals("Apple", result.getBrand());

        verify(productRepo).findById(1L);
    }

    @Test
    void findById_whenProductDoesNotExist_shouldThrowResourceNotFoundException() {
        when(productRepo.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.findById(99L));

        verify(productRepo).findById(99L);
    }

    @Test
    void delete_whenProductExists_shouldDeleteProduct() {
        when(productRepo.existsById(1L))
                .thenReturn(true);

        productService.delete(1L);

        verify(productRepo).existsById(1L);
        verify(productRepo).deleteById(1L);
    }

    @Test
    void delete_whenProductDoesNotExist_shouldThrowResourceNotFoundException() {
        when(productRepo.existsById(99L))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.delete(99L));

        verify(productRepo).existsById(99L);
        verify(productRepo, never()).deleteById(99L);
    }
}
