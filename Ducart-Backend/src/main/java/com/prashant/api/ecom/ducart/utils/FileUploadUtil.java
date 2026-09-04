package com.prashant.api.ecom.ducart.utils;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class FileUploadUtil {
    public static String uploadDirFor(String folder) throws IOException {
        if (folder == null || folder.isBlank()) {
            throw new IllegalArgumentException("Upload folder is required");
        }

        Path uploadsRoot = Paths.get("uploads").toAbsolutePath().normalize();
        Path dir = uploadsRoot.resolve(folder).normalize();
        if (!dir.startsWith(uploadsRoot)) {
            throw new IllegalArgumentException("Upload folder must stay inside the uploads directory");
        }
        Files.createDirectories(dir);
        return dir.toString();
    }
    public static String getUploadDirFor(String folder) {
    try {
        return uploadDirFor(folder) + java.io.File.separator;
    } catch (IOException e) {
        throw new RuntimeException("Could not create upload directory for: " + folder, e);
    }
}

    public static String save(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String original = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> !name.isBlank())
                .orElse("file");
        String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String filename = System.currentTimeMillis() + "_" + safeName;
        Path target = Paths.get(uploadDirFor(folder), filename);
        Files.write(target, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return "/uploads/" + folder + "/" + filename;
    }

    public static java.util.List<String> saveAll(MultipartFile[] files, String folder) throws IOException {
        java.util.List<String> paths = new java.util.ArrayList<>();
        if (files == null) return paths;
        for (MultipartFile file : files) {
            String path = save(file, folder);
            if (path != null) paths.add(path);
        }
        return paths;
    }
}
