package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Brand;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.BrandDTO;
import com.prashant.api.ecom.ducart.modal.BrandResponseDTO;
import com.prashant.api.ecom.ducart.repositories.BrandRepo;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepo brandRepo;

    @Mock
    private ProductRepo productRepo;

    @InjectMocks
    private BrandService brandService;

    private Brand brand(Long id, String name, boolean active) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setName(name);
        brand.setPic("/uploads/brands/" + name.toLowerCase() + ".jpg");
        brand.setActive(active);
        return brand;
    }

    private BrandDTO brandDTO(String name, Boolean active) {
        BrandDTO dto = new BrandDTO();
        dto.setName(name);
        dto.setActive(active);
        return dto;
    }

    private MultipartFile noFile() {
        return null;
    }

    @Test
    void create_whenBrandIsValid_shouldSaveBrandAndReturnResponseDTO() throws Exception {
        BrandDTO dto = brandDTO("Nike", true);

        when(brandRepo.save(any(Brand.class))).thenAnswer(invocation -> {
            Brand saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        BrandResponseDTO result = brandService.create(dto, noFile());

        assertEquals(1L, result.getId());
        assertEquals("Nike", result.getName());
        assertTrue(result.isActive());
        verify(brandRepo).save(any(Brand.class));
    }

    @Test
    void create_whenActiveIsNull_shouldDefaultActiveToTrue() throws Exception {
        BrandDTO dto = brandDTO("Nike", null);

        when(brandRepo.save(any(Brand.class))).thenAnswer(invocation -> {
            Brand saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        BrandResponseDTO result = brandService.create(dto, noFile());

        assertTrue(result.isActive());
        verify(brandRepo).save(any(Brand.class));
    }

    @Test
    void create_whenNameIsBlank_shouldThrowBadRequestException() {
        BrandDTO dto = brandDTO("", true);

        assertThrows(BadRequestException.class, () -> brandService.create(dto, noFile()));

        verify(brandRepo, never()).save(any(Brand.class));
    }

    @Test
    void create_whenDtoOrNameIsNull_shouldThrowBadRequestException() {
        BrandDTO nullName = brandDTO(null, true);

        assertThrows(BadRequestException.class, () -> brandService.create(null, noFile()));
        assertThrows(BadRequestException.class, () -> brandService.create(nullName, noFile()));

        verify(brandRepo, never()).save(any(Brand.class));
    }

    @Test
    void create_whenPicIsProvided_shouldStoreAndAssignIt() throws Exception {
        BrandDTO dto = brandDTO("Nike", true);
        MockMultipartFile pic = new MockMultipartFile(
                "pic", "nike logo.png", "image/png",
                "brand-pic".getBytes(StandardCharsets.UTF_8));
        when(brandRepo.save(any(Brand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BrandResponseDTO result = brandService.create(dto, pic);
        Path storedFile = Path.of("uploads", "brands", Path.of(result.getPic()).getFileName().toString());

        try {
            assertTrue(Files.exists(storedFile));
            assertEquals("brand-pic", Files.readString(storedFile));
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }

    @Test
    void findAll_shouldReturnBrandResponseDTOList() {
        when(brandRepo.findAll()).thenReturn(List.of(
                brand(1L, "Nike", true),
                brand(2L, "Adidas", false)));

        List<BrandResponseDTO> result = brandService.findAll();

        assertEquals(2, result.size());
        assertEquals("Nike", result.get(0).getName());
        assertTrue(result.get(0).isActive());
        assertEquals("Adidas", result.get(1).getName());
        assertFalse(result.get(1).isActive());
        verify(brandRepo).findAll();
    }

    @Test
    void findById_whenBrandExists_shouldReturnBrandResponseDTO() {
        when(brandRepo.findById(1L)).thenReturn(Optional.of(brand(1L, "Nike", true)));

        BrandResponseDTO result = brandService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Nike", result.getName());
        assertTrue(result.isActive());
        verify(brandRepo).findById(1L);
    }

    @Test
    void findById_whenBrandDoesNotExist_shouldThrowResourceNotFoundException() {
        when(brandRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> brandService.findById(99L));

        verify(brandRepo).findById(99L);
    }

    @Test
    void update_whenBrandExists_shouldUpdateBrandAndKeepOldPicWhenNoNewFile() throws Exception {
        Brand existing = brand(1L, "Nike", true);
        BrandDTO dto = brandDTO("Adidas", false);

        when(brandRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(brandRepo.save(existing)).thenReturn(existing);

        BrandResponseDTO result = brandService.update(1L, dto, noFile());

        assertEquals(1L, result.getId());
        assertEquals("Adidas", result.getName());
        assertEquals("/uploads/brands/nike.jpg", result.getPic());
        assertFalse(result.isActive());
        verify(brandRepo).findById(1L);
        verify(brandRepo).save(existing);
    }

    @Test
    void update_whenBrandDoesNotExist_shouldThrowResourceNotFoundException() {
        BrandDTO dto = brandDTO("Adidas", true);

        when(brandRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> brandService.update(99L, dto, noFile()));

        verify(brandRepo).findById(99L);
        verify(brandRepo, never()).save(any(Brand.class));
    }

    @Test
    void update_whenActiveIsOmittedAndPicIsProvided_shouldKeepActiveAndReplacePic() throws Exception {
        Brand existing = brand(1L, "Nike", false);
        BrandDTO dto = brandDTO("Nike Updated", null);
        MockMultipartFile pic = new MockMultipartFile(
                "pic", "new-nike.png", "image/png",
                "new-brand-pic".getBytes(StandardCharsets.UTF_8));
        when(brandRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(brandRepo.save(existing)).thenReturn(existing);

        BrandResponseDTO result = brandService.update(1L, dto, pic);
        Path storedFile = Path.of("uploads", "brands", Path.of(result.getPic()).getFileName().toString());

        try {
            assertFalse(result.isActive());
            assertTrue(Files.exists(storedFile));
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }

    @Test
    void updatePlain_whenBrandExists_shouldUpdateWithoutImage() {
        Brand existing = brand(1L, "Nike", true);
        BrandDTO dto = brandDTO("Adidas", false);

        when(brandRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(brandRepo.save(existing)).thenReturn(existing);

        BrandResponseDTO result = brandService.updatePlain(1L, dto);

        assertEquals("Adidas", result.getName());
        assertEquals("/uploads/brands/nike.jpg", result.getPic());
        assertFalse(result.isActive());
        verify(brandRepo).findById(1L);
        verify(brandRepo).save(existing);
    }

    @Test
    void delete_whenBrandExistsAndNoProductsUseIt_shouldDeleteBrand() {
        Brand existing = brand(1L, "Nike", true);

        when(brandRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.existsByBrand("Nike")).thenReturn(false);

        brandService.delete(1L);

        verify(brandRepo).findById(1L);
        verify(productRepo).existsByBrand("Nike");
        verify(brandRepo).deleteById(1L);
    }

    @Test
    void delete_whenBrandDoesNotExist_shouldThrowResourceNotFoundException() {
        when(brandRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> brandService.delete(99L));

        verify(brandRepo).findById(99L);
        verify(productRepo, never()).existsByBrand(any());
        verify(brandRepo, never()).deleteById(99L);
    }

    @Test
    void delete_whenProductsStillUseBrand_shouldThrowConflictException() {
        Brand existing = brand(1L, "Nike", true);

        when(brandRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.existsByBrand("Nike")).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> brandService.delete(1L));

        assertEquals("Cannot delete brand because products still use it", exception.getMessage());
        verify(brandRepo).findById(1L);
        verify(productRepo).existsByBrand("Nike");
        verify(brandRepo, never()).deleteById(1L);
    }
}
