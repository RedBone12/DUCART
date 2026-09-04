package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenericCatalogServiceTest {

    @Mock
    private JpaRepository<TestCatalogItem, Long> repo;

    private GenericCatalogService<TestCatalogItem> service;

    @BeforeEach
    void setUp() {
        // GenericCatalogService is not managed by Spring here.
        // In controllers, it is created manually with "new GenericCatalogService<>(...)".
        // So in this unit test, we also create it manually.
        service = new GenericCatalogService<>(repo, "test-folder", TestCatalogItem.class);
    }

    private TestCatalogItem item() {
        // This fake entity simulates Brand / Maincategory / Subcategory / Testimonial.
        // GenericCatalogService only needs JavaBean getters/setters.
        TestCatalogItem item = new TestCatalogItem();
        item.setId(1L);
        item.setName("Old Name");
        item.setPic("/uploads/test-folder/old.jpg");
        item.setActive(true);
        return item;
    }

    @Test
    void createWithoutFile_shouldSaveItemAndReturnSavedItem() {
        TestCatalogItem item = new TestCatalogItem();
        item.setName("New Name");
        item.setActive(true);

        TestCatalogItem saved = new TestCatalogItem();
        saved.setId(1L);
        saved.setName("New Name");
        saved.setActive(true);

        // Mock save().
        // Pretend the database saved the item and generated id = 1.
        when(repo.save(item)).thenReturn(saved);

        TestCatalogItem result = service.createWithoutFile(item);

        assertEquals(1L, result.getId());
        assertEquals("New Name", result.getName());
        assertTrue(result.isActive());

        verify(repo).save(item);
    }

    @Test
    void create_whenPicIsNull_shouldSaveItemWithoutChangingPic() throws IOException {
        TestCatalogItem item = new TestCatalogItem();
        item.setName("New Name");
        item.setActive(true);

        TestCatalogItem saved = new TestCatalogItem();
        saved.setId(1L);
        saved.setName("New Name");
        saved.setActive(true);
        saved.setPic(null);

        // Mock save().
        // pic is null here, so FileUploadUtil.save(null, ...) should return null,
        // and GenericCatalogService should save the item normally.
        when(repo.save(item)).thenReturn(saved);

        TestCatalogItem result = service.create(item, null);

        assertEquals(1L, result.getId());
        assertEquals("New Name", result.getName());
        assertNull(result.getPic());
        assertTrue(result.isActive());

        verify(repo).save(item);
    }

    @Test
    void findAll_shouldReturnAllItems() {
        TestCatalogItem item = item();

        // Mock findAll().
        // Pretend the database has one catalog item.
        when(repo.findAll()).thenReturn(List.of(item));

        List<TestCatalogItem> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Old Name", result.get(0).getName());
        assertEquals("/uploads/test-folder/old.jpg", result.get(0).getPic());
        assertTrue(result.get(0).isActive());

        verify(repo).findAll();
    }

    @Test
    void findById_whenItemExists_shouldReturnItem() {
        TestCatalogItem item = item();

        // Mock findById().
        // Pretend the item exists.
        when(repo.findById(1L)).thenReturn(Optional.of(item));

        TestCatalogItem result = service.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Old Name", result.getName());
        assertEquals("/uploads/test-folder/old.jpg", result.getPic());
        assertTrue(result.isActive());

        verify(repo).findById(1L);
    }

    @Test
    void findById_whenItemDoesNotExist_shouldThrowResourceNotFoundException() {
        // Mock findById().
        // Pretend the item does not exist.
        when(repo.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.findById(99L)
        );

        assertEquals("TestCatalogItem not found", exception.getMessage());

        verify(repo).findById(99L);
    }

    @Test
    void updatePlain_whenItemExistsAndNewPicIsNull_shouldUpdateItemAndKeepOldPic() {
        TestCatalogItem existing = item();

        TestCatalogItem updateData = new TestCatalogItem();
        updateData.setId(999L); // This should not overwrite existing id.
        updateData.setName("Updated Name");
        updateData.setPic(null); // Null pic should not remove old pic.
        updateData.setActive(false);

        // Mock findById().
        // Pretend the item exists.
        when(repo.findById(1L)).thenReturn(Optional.of(existing));

        // Mock save().
        // Return the updated existing object.
        when(repo.save(existing)).thenReturn(existing);

        TestCatalogItem result = service.updatePlain(1L, updateData);

        // id should still be 1, because BeanUtils ignores "id".
        assertEquals(1L, result.getId());

        assertEquals("Updated Name", result.getName());

        // Old pic should be preserved when incoming pic is null.
        assertEquals("/uploads/test-folder/old.jpg", result.getPic());

        assertFalse(result.isActive());

        verify(repo).findById(1L);
        verify(repo).save(existing);
    }

    @Test
    void updatePlain_whenItemExistsAndNewPicIsProvided_shouldUseNewPic() {
        TestCatalogItem existing = item();

        TestCatalogItem updateData = new TestCatalogItem();
        updateData.setName("Updated Name");
        updateData.setPic("/uploads/test-folder/new.jpg");
        updateData.setActive(false);

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenReturn(existing);

        TestCatalogItem result = service.updatePlain(1L, updateData);

        assertEquals(1L, result.getId());
        assertEquals("Updated Name", result.getName());
        assertEquals("/uploads/test-folder/new.jpg", result.getPic());
        assertFalse(result.isActive());

        verify(repo).findById(1L);
        verify(repo).save(existing);
    }

    @Test
    void update_whenItemExistsAndPicFileIsNull_shouldUpdateItemAndKeepOldPic() throws IOException {
        TestCatalogItem existing = item();

        TestCatalogItem updateData = new TestCatalogItem();
        updateData.setName("Updated Name");
        updateData.setPic(null);
        updateData.setActive(false);

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenReturn(existing);

        TestCatalogItem result = service.update(1L, updateData, null);

        assertEquals(1L, result.getId());
        assertEquals("Updated Name", result.getName());

        // Since MultipartFile pic is null,
        // FileUploadUtil.save(null, ...) returns null,
        // so GenericCatalogService should keep the old pic.
        assertEquals("/uploads/test-folder/old.jpg", result.getPic());

        assertFalse(result.isActive());

        verify(repo).findById(1L);
        verify(repo).save(existing);
    }

    @Test
    void update_whenPicFileIsProvided_shouldReplaceOldPic() throws IOException {
        TestCatalogItem existing = item();
        TestCatalogItem updateData = new TestCatalogItem();
        updateData.setName("Updated Name");
        MockMultipartFile pic = new MockMultipartFile(
                "pic", "new image.png", "image/png",
                "new-pic".getBytes(StandardCharsets.UTF_8));
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenReturn(existing);

        TestCatalogItem result = service.update(1L, updateData, pic);
        Path storedFile = Path.of("uploads", "test-folder", Path.of(result.getPic()).getFileName().toString());

        try {
            assertTrue(result.getPic().startsWith("/uploads/test-folder/"));
            assertTrue(Files.exists(storedFile));
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }

    @Test
    void updatePlain_whenItemDoesNotExist_shouldThrowResourceNotFoundException() {
        TestCatalogItem updateData = new TestCatalogItem();
        updateData.setName("Updated Name");

        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.updatePlain(99L, updateData)
        );

        verify(repo).findById(99L);
        verify(repo, never()).save(any(TestCatalogItem.class));
    }

    @Test
    void delete_whenItemExists_shouldDeleteItem() {
        // Mock existsById().
        // Pretend the item exists.
        when(repo.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repo).existsById(1L);
        verify(repo).deleteById(1L);
    }

    @Test
    void delete_whenItemDoesNotExist_shouldThrowResourceNotFoundException() {
        // Mock existsById().
        // Pretend the item does not exist.
        when(repo.existsById(99L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.delete(99L)
        );

        assertEquals("TestCatalogItem not found", exception.getMessage());

        verify(repo).existsById(99L);
        verify(repo, never()).deleteById(99L);
    }

    /**
     * A small fake catalog entity used only for testing GenericCatalogService.
     *
     * GenericCatalogService uses:
     * - getPic()
     * - setPic(String)
     * - BeanUtils.copyProperties(...)
     *
     * So this fake class provides normal JavaBean getters and setters.
     */
    static class TestCatalogItem {
        private Long id;
        private String name;
        private String pic;
        private boolean active;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPic() {
            return pic;
        }

        public void setPic(String pic) {
            this.pic = pic;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
