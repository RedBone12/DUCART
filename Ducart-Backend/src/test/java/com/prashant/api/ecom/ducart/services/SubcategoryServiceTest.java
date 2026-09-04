package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Subcategory;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.SubcategoryDTO;
import com.prashant.api.ecom.ducart.modal.SubcategoryResponseDTO;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import com.prashant.api.ecom.ducart.repositories.SubcategoryRepo;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubcategoryServiceTest {

    @Mock
    private SubcategoryRepo subcategoryRepo;

    @Mock
    private ProductRepo productRepo;

    @InjectMocks
    private SubcategoryService subcategoryService;

    /*
     * 这份测试对应重构后的 SubcategoryService：SubcategoryDTO 不再包含 pic；
     * 图片来自 MultipartFile；删除 subcategory 前会检查 Product 表里是否还有商品使用这个 subcategory。
     */
    private Subcategory subcategory(Long id, String name, boolean active) {
        Subcategory subcategory = new Subcategory();
        subcategory.setId(id);
        subcategory.setName(name);
        subcategory.setPic("/uploads/subcategories/" + name.toLowerCase().replace(" ", "-") + ".jpg");
        subcategory.setActive(active);
        return subcategory;
    }

    private SubcategoryDTO subcategoryDTO(String name, Boolean active) {
        SubcategoryDTO dto = new SubcategoryDTO();
        dto.setName(name);
        dto.setActive(active);
        return dto;
    }

    private MultipartFile noFile() {
        return null;
    }

    @Test
    void create_whenSubcategoryIsValid_shouldSaveAndReturnResponseDTO() throws Exception {
        SubcategoryDTO dto = subcategoryDTO("Phones", true);

        when(subcategoryRepo.save(any(Subcategory.class))).thenAnswer(invocation -> {
            Subcategory saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        SubcategoryResponseDTO result = subcategoryService.create(dto, noFile());

        assertEquals(1L, result.getId());
        assertEquals("Phones", result.getName());
        assertTrue(result.getActive());
        assertNull(result.getPic());
        verify(subcategoryRepo).save(any(Subcategory.class));
    }

    @Test
    void create_whenActiveIsNull_shouldDefaultActiveToTrue() throws Exception {
        SubcategoryDTO dto = subcategoryDTO("Phones", null);

        when(subcategoryRepo.save(any(Subcategory.class))).thenAnswer(invocation -> {
            Subcategory saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        SubcategoryResponseDTO result = subcategoryService.create(dto, noFile());

        assertTrue(result.getActive());
        verify(subcategoryRepo).save(any(Subcategory.class));
    }

    @Test
    void create_whenNameIsBlank_shouldThrowBadRequestException() {
        SubcategoryDTO dto = subcategoryDTO("", true);

        assertThrows(BadRequestException.class, () -> subcategoryService.create(dto, noFile()));

        verify(subcategoryRepo, never()).save(any(Subcategory.class));
    }

    @Test
    void create_whenDtoOrNameIsNull_shouldThrowBadRequestException() {
        SubcategoryDTO nullName = subcategoryDTO(null, true);

        assertThrows(BadRequestException.class, () -> subcategoryService.create(null, noFile()));
        assertThrows(BadRequestException.class, () -> subcategoryService.create(nullName, noFile()));

        verify(subcategoryRepo, never()).save(any(Subcategory.class));
    }

    @Test
    void create_whenPicIsProvided_shouldStoreAndAssignIt() throws Exception {
        SubcategoryDTO dto = subcategoryDTO("Phones", true);
        MockMultipartFile pic = new MockMultipartFile(
                "pic", "phones.png", "image/png",
                "subcategory-pic".getBytes(StandardCharsets.UTF_8));
        when(subcategoryRepo.save(any(Subcategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubcategoryResponseDTO result = subcategoryService.create(dto, pic);
        Path storedFile = Path.of("uploads", "subcategories", Path.of(result.getPic()).getFileName().toString());

        try {
            assertTrue(Files.exists(storedFile));
            assertEquals("subcategory-pic", Files.readString(storedFile));
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }

    @Test
    void findAll_shouldReturnSubcategoryResponseDTOList() {
        when(subcategoryRepo.findAll()).thenReturn(List.of(
                subcategory(1L, "Phones", true),
                subcategory(2L, "Shoes", false)));

        List<SubcategoryResponseDTO> result = subcategoryService.findAll();

        assertEquals(2, result.size());
        assertEquals("Phones", result.get(0).getName());
        assertTrue(result.get(0).getActive());
        assertEquals("Shoes", result.get(1).getName());
        assertFalse(result.get(1).getActive());
        verify(subcategoryRepo).findAll();
    }

    @Test
    void findById_whenSubcategoryExists_shouldReturnResponseDTO() {
        when(subcategoryRepo.findById(1L))
                .thenReturn(Optional.of(subcategory(1L, "Phones", true)));

        SubcategoryResponseDTO result = subcategoryService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Phones", result.getName());
        assertEquals("/uploads/subcategories/phones.jpg", result.getPic());
        assertTrue(result.getActive());
        verify(subcategoryRepo).findById(1L);
    }

    @Test
    void findById_whenSubcategoryDoesNotExist_shouldThrowResourceNotFoundException() {
        when(subcategoryRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subcategoryService.findById(99L));

        verify(subcategoryRepo).findById(99L);
    }

    @Test
    void update_whenSubcategoryExists_shouldUpdateAndKeepOldPicWhenNoNewFile() throws Exception {
        Subcategory existing = subcategory(1L, "Phones", true);
        SubcategoryDTO dto = subcategoryDTO("Updated Phones", false);

        when(subcategoryRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(subcategoryRepo.save(existing)).thenReturn(existing);

        SubcategoryResponseDTO result = subcategoryService.update(1L, dto, noFile());

        assertEquals(1L, result.getId());
        assertEquals("Updated Phones", result.getName());
        assertEquals("/uploads/subcategories/phones.jpg", result.getPic());
        assertFalse(result.getActive());
        verify(subcategoryRepo).findById(1L);
        verify(subcategoryRepo).save(existing);
    }

    @Test
    void update_whenSubcategoryDoesNotExist_shouldThrowResourceNotFoundException() {
        SubcategoryDTO dto = subcategoryDTO("Updated Phones", true);

        when(subcategoryRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subcategoryService.update(99L, dto, noFile()));

        verify(subcategoryRepo).findById(99L);
        verify(subcategoryRepo, never()).save(any(Subcategory.class));
    }

    @Test
    void update_whenActiveIsOmittedAndPicIsProvided_shouldKeepActiveAndReplacePic() throws Exception {
        Subcategory existing = subcategory(1L, "Phones", false);
        SubcategoryDTO dto = subcategoryDTO("Updated Phones", null);
        MockMultipartFile pic = new MockMultipartFile(
                "pic", "updated-phones.png", "image/png",
                "updated-subcategory".getBytes(StandardCharsets.UTF_8));
        when(subcategoryRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(subcategoryRepo.save(existing)).thenReturn(existing);

        SubcategoryResponseDTO result = subcategoryService.update(1L, dto, pic);
        Path storedFile = Path.of("uploads", "subcategories", Path.of(result.getPic()).getFileName().toString());

        try {
            assertFalse(result.getActive());
            assertTrue(Files.exists(storedFile));
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }

    @Test
    void updatePlain_whenSubcategoryExists_shouldUpdateWithoutImage() {
        Subcategory existing = subcategory(1L, "Phones", true);
        SubcategoryDTO dto = subcategoryDTO("Updated Phones", false);

        when(subcategoryRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(subcategoryRepo.save(existing)).thenReturn(existing);

        SubcategoryResponseDTO result = subcategoryService.updatePlain(1L, dto);

        assertEquals(1L, result.getId());
        assertEquals("Updated Phones", result.getName());
        assertEquals("/uploads/subcategories/phones.jpg", result.getPic());
        assertFalse(result.getActive());
        verify(subcategoryRepo).findById(1L);
        verify(subcategoryRepo).save(existing);
    }

    @Test
    void delete_whenSubcategoryExistsAndNoProductsUseIt_shouldDelete() {
        Subcategory existing = subcategory(1L, "Phones", true);
        existing.setPic(null);

        when(subcategoryRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.existsBySubcategory("Phones")).thenReturn(false);

        subcategoryService.delete(1L);

        verify(subcategoryRepo).findById(1L);
        verify(productRepo).existsBySubcategory("Phones");
        verify(subcategoryRepo).deleteById(1L);
    }

    @Test
    void delete_whenPicIsPresent_shouldAttemptFileDeletionAndDeleteRecord() {
        Subcategory existing = subcategory(1L, "Phones", true);
        when(subcategoryRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.existsBySubcategory("Phones")).thenReturn(false);

        subcategoryService.delete(1L);

        verify(subcategoryRepo).deleteById(1L);
    }

    @Test
    void delete_whenSubcategoryDoesNotExist_shouldThrowResourceNotFoundException() {
        when(subcategoryRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subcategoryService.delete(99L));

        verify(subcategoryRepo).findById(99L);
        verify(productRepo, never()).existsBySubcategory(any());
        verify(subcategoryRepo, never()).deleteById(99L);
    }

    @Test
    void delete_whenProductsStillUseSubcategory_shouldThrowConflictException() {
        Subcategory existing = subcategory(1L, "Phones", true);

        when(subcategoryRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.existsBySubcategory("Phones")).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> subcategoryService.delete(1L));

        assertEquals("Cannot delete subcategory because products still use it", exception.getMessage());
        verify(subcategoryRepo).findById(1L);
        verify(productRepo).existsBySubcategory("Phones");
        verify(subcategoryRepo, never()).deleteById(1L);
    }
}
