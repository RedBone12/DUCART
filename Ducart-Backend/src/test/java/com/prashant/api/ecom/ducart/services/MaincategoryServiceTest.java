package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Maincategory;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.MainResponseDTO;
import com.prashant.api.ecom.ducart.modal.MaincategoryDTO;
import com.prashant.api.ecom.ducart.repositories.MaincategoryRepo;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaincategoryServiceTest {

    @Mock
    private MaincategoryRepo maincategoryRepo;

    @Mock
    private ProductRepo productRepo;

    @InjectMocks
    private MaincategoryService maincategoryService;

    /*
     * 这份测试对应重构后的 MaincategoryService：MaincategoryDTO 不再包含 pic；
     * 图片来自 MultipartFile；删除 main category 前会检查 Product 表里是否还有商品使用这个 maincategory。
     */
    private Maincategory maincategory(Long id, String name, boolean active) {
        Maincategory maincategory = new Maincategory();
        maincategory.setId(id);
        maincategory.setName(name);
        maincategory.setPic("/uploads/maincategories/" + name.toLowerCase().replace(" ", "-") + ".jpg");
        maincategory.setActive(active);
        return maincategory;
    }

    private MaincategoryDTO maincategoryDTO(String name, Boolean active) {
        MaincategoryDTO dto = new MaincategoryDTO();
        dto.setName(name);
        dto.setActive(active);
        return dto;
    }

    private MultipartFile noFile() {
        return null;
    }

    @Test
    void create_whenMaincategoryIsValid_shouldSaveAndReturnResponseDTO() throws Exception {
        MaincategoryDTO dto = maincategoryDTO("Electronics", true);

        when(maincategoryRepo.save(any(Maincategory.class))).thenAnswer(invocation -> {
            Maincategory saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        MainResponseDTO result = maincategoryService.create(dto, noFile());

        assertEquals(1L, result.getId());
        assertEquals("Electronics", result.getName());
        assertTrue(result.getActive());
        assertNull(result.getPic());
        verify(maincategoryRepo).save(any(Maincategory.class));
    }

    @Test
    void create_whenActiveIsNull_shouldDefaultActiveToTrue() throws Exception {
        MaincategoryDTO dto = maincategoryDTO("Electronics", null);

        when(maincategoryRepo.save(any(Maincategory.class))).thenAnswer(invocation -> {
            Maincategory saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        MainResponseDTO result = maincategoryService.create(dto, noFile());

        assertTrue(result.getActive());
        verify(maincategoryRepo).save(any(Maincategory.class));
    }

    @Test
    void create_whenNameIsBlank_shouldThrowBadRequestException() {
        MaincategoryDTO dto = maincategoryDTO("", true);

        assertThrows(BadRequestException.class, () -> maincategoryService.create(dto, noFile()));

        verify(maincategoryRepo, never()).save(any(Maincategory.class));
    }

    @Test
    void create_whenDtoOrNameIsNull_shouldThrowBadRequestException() {
        MaincategoryDTO nullName = maincategoryDTO(null, true);

        assertThrows(BadRequestException.class, () -> maincategoryService.create(null, noFile()));
        assertThrows(BadRequestException.class, () -> maincategoryService.create(nullName, noFile()));

        verify(maincategoryRepo, never()).save(any(Maincategory.class));
    }

    @Test
    void create_whenPicIsProvided_shouldStoreAndAssignIt() throws Exception {
        MaincategoryDTO dto = maincategoryDTO("Electronics", true);
        MockMultipartFile pic = new MockMultipartFile(
                "pic", "electronics.png", "image/png",
                "maincategory-pic".getBytes(StandardCharsets.UTF_8));
        when(maincategoryRepo.save(any(Maincategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MainResponseDTO result = maincategoryService.create(dto, pic);
        Path storedFile = Path.of("uploads", "maincategories", Path.of(result.getPic()).getFileName().toString());

        try {
            assertTrue(Files.exists(storedFile));
            assertEquals("maincategory-pic", Files.readString(storedFile));
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }

    @Test
    void findAll_shouldReturnMaincategoryResponseDTOList() {
        when(maincategoryRepo.findAll()).thenReturn(List.of(
                maincategory(1L, "Electronics", true),
                maincategory(2L, "Fashion", false)));

        List<MainResponseDTO> result = maincategoryService.findAll();

        assertEquals(2, result.size());
        assertEquals("Electronics", result.get(0).getName());
        assertTrue(result.get(0).getActive());
        assertEquals("Fashion", result.get(1).getName());
        assertFalse(result.get(1).getActive());
        verify(maincategoryRepo).findAll();
    }

    @Test
    void findById_whenMaincategoryExists_shouldReturnResponseDTO() {
        when(maincategoryRepo.findById(1L))
                .thenReturn(Optional.of(maincategory(1L, "Electronics", true)));

        MainResponseDTO result = maincategoryService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Electronics", result.getName());
        assertEquals("/uploads/maincategories/electronics.jpg", result.getPic());
        assertTrue(result.getActive());
        verify(maincategoryRepo).findById(1L);
    }

    @Test
    void findById_whenMaincategoryDoesNotExist_shouldThrowResourceNotFoundException() {
        when(maincategoryRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> maincategoryService.findById(99L));

        verify(maincategoryRepo).findById(99L);
    }

    @Test
    void update_whenMaincategoryExists_shouldUpdateAndKeepOldPicWhenNoNewFile() throws Exception {
        Maincategory existing = maincategory(1L, "Electronics", true);
        MaincategoryDTO dto = maincategoryDTO("Updated Electronics", false);

        when(maincategoryRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(maincategoryRepo.save(existing)).thenReturn(existing);

        MainResponseDTO result = maincategoryService.update(1L, dto, noFile());

        assertEquals(1L, result.getId());
        assertEquals("Updated Electronics", result.getName());
        assertEquals("/uploads/maincategories/electronics.jpg", result.getPic());
        assertFalse(result.getActive());
        verify(maincategoryRepo).findById(1L);
        verify(maincategoryRepo).save(existing);
    }

    @Test
    void update_whenMaincategoryDoesNotExist_shouldThrowResourceNotFoundException() {
        MaincategoryDTO dto = maincategoryDTO("Updated Electronics", true);

        when(maincategoryRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> maincategoryService.update(99L, dto, noFile()));

        verify(maincategoryRepo).findById(99L);
        verify(maincategoryRepo, never()).save(any(Maincategory.class));
    }

    @Test
    void update_whenActiveIsOmittedAndPicIsProvided_shouldKeepActiveAndReplacePic() throws Exception {
        Maincategory existing = maincategory(1L, "Electronics", false);
        MaincategoryDTO dto = maincategoryDTO("Updated Electronics", null);
        MockMultipartFile pic = new MockMultipartFile(
                "pic", "updated.png", "image/png",
                "updated-maincategory".getBytes(StandardCharsets.UTF_8));
        when(maincategoryRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(maincategoryRepo.save(existing)).thenReturn(existing);

        MainResponseDTO result = maincategoryService.update(1L, dto, pic);
        Path storedFile = Path.of("uploads", "maincategories", Path.of(result.getPic()).getFileName().toString());

        try {
            assertFalse(result.getActive());
            assertTrue(Files.exists(storedFile));
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }

    @Test
    void updatePlain_whenMaincategoryExists_shouldUpdateWithoutImage() {
        Maincategory existing = maincategory(1L, "Electronics", true);
        MaincategoryDTO dto = maincategoryDTO("Updated Electronics", false);

        when(maincategoryRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(maincategoryRepo.save(existing)).thenReturn(existing);

        MainResponseDTO result = maincategoryService.updatePlain(1L, dto);

        assertEquals(1L, result.getId());
        assertEquals("Updated Electronics", result.getName());
        assertEquals("/uploads/maincategories/electronics.jpg", result.getPic());
        assertFalse(result.getActive());
        verify(maincategoryRepo).findById(1L);
        verify(maincategoryRepo).save(existing);
    }

    @Test
    void delete_whenMaincategoryExistsAndNoProductsUseIt_shouldDelete() {
        Maincategory existing = maincategory(1L, "Electronics", true);

        when(maincategoryRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.existsByMaincategory("Electronics")).thenReturn(false);

        maincategoryService.delete(1L);

        verify(maincategoryRepo).findById(1L);
        verify(productRepo).existsByMaincategory("Electronics");
        verify(maincategoryRepo).deleteById(1L);
    }

    @Test
    void delete_whenPicIsNull_shouldStillDeleteDatabaseRecord() {
        Maincategory existing = maincategory(1L, "Electronics", true);
        existing.setPic(null);
        when(maincategoryRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.existsByMaincategory("Electronics")).thenReturn(false);

        maincategoryService.delete(1L);

        verify(maincategoryRepo).deleteById(1L);
    }

    @Test
    void delete_whenMaincategoryDoesNotExist_shouldThrowResourceNotFoundException() {
        when(maincategoryRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> maincategoryService.delete(99L));

        verify(maincategoryRepo).findById(99L);
        verify(productRepo, never()).existsByMaincategory(any());
        verify(maincategoryRepo, never()).deleteById(99L);
    }

    @Test
    void delete_whenProductsStillUseMaincategory_shouldThrowConflictException() {
        Maincategory existing = maincategory(1L, "Electronics", true);

        when(maincategoryRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.existsByMaincategory("Electronics")).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> maincategoryService.delete(1L));

        assertEquals("Cannot delete main category because products still use it", exception.getMessage());
        verify(maincategoryRepo).findById(1L);
        verify(productRepo).existsByMaincategory("Electronics");
        verify(maincategoryRepo, never()).deleteById(1L);
    }
}
