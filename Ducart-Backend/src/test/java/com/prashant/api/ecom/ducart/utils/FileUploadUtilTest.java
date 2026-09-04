package com.prashant.api.ecom.ducart.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadUtilTest {

    private final String folder = "test-" + UUID.randomUUID();

    @AfterEach
    void removeTestUploadDirectory() throws Exception {
        Path directory = Paths.get("uploads", folder).toAbsolutePath().normalize();
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    @Test
    void save_shouldSanitizeFilenameWriteBytesAndReturnPublicPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "pic", "my unsafe image?.png", "image/png", new byte[] {1, 2, 3});

        String publicPath = FileUploadUtil.save(file, folder);

        assertTrue(publicPath.matches("/uploads/" + folder + "/\\d+_my_unsafe_image_.png"));
        Path savedFile = Paths.get("uploads", folder, publicPath.substring(publicPath.lastIndexOf('/') + 1));
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(savedFile));
    }

    @Test
    void saveAndSaveAll_shouldIgnoreNullAndEmptyFiles() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("pic", "empty.png", "image/png", new byte[0]);
        MockMultipartFile content = new MockMultipartFile("pic", null, "text/plain", "ok".getBytes());

        assertNull(FileUploadUtil.save(null, folder));
        assertNull(FileUploadUtil.save(empty, folder));
        assertTrue(FileUploadUtil.saveAll(null, folder).isEmpty());

        List<String> paths = FileUploadUtil.saveAll(new MockMultipartFile[] {empty, content}, folder);
        assertEquals(1, paths.size());
        assertTrue(paths.get(0).endsWith("_file"));
    }

    @Test
    void uploadDirectory_shouldRejectBlankAndTraversalFolders() {
        assertThrows(IllegalArgumentException.class, () -> FileUploadUtil.uploadDirFor(null));
        assertThrows(IllegalArgumentException.class, () -> FileUploadUtil.uploadDirFor(" "));
        assertThrows(IllegalArgumentException.class, () -> FileUploadUtil.uploadDirFor("..\\outside"));
    }
}
