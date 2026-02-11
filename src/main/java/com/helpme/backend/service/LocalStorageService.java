package com.helpme.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocalStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Lưu 1 file ảnh
     * 
     * @param file   MultipartFile từ request
     * @param folder Subfolder (vd: "orders", "addons")
     * @return Relative path (vd: "orders/abc-123.jpg")
     */
    public String saveImage(MultipartFile file, String folder) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            // Validate image type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("File must be an image");
            }

            // Validate file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("File size must be less than 10MB");
            }

            // Create folder if not exists
            Path folderPath = Paths.get(uploadDir, folder);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                extension = ".jpg";
            }

            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = folderPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return relative path
            String relativePath = folder + "/" + filename;
            log.info("✅ Saved image: {}", relativePath);

            return relativePath;

        } catch (IOException e) {
            log.error("❌ Failed to save image", e);
            throw new RuntimeException("Failed to save image: " + e.getMessage(), e);
        }
    }

    /**
     * Lưu nhiều files
     */
    public List<String> saveMultipleImages(List<MultipartFile> files, String folder) {
        return files.stream()
                .map(file -> saveImage(file, folder))
                .collect(Collectors.toList());
    }

    /**
     * Load file để serve qua HTTP
     */
    public Resource loadImage(String relativePath) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Image not found: " + relativePath);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Image not found: " + relativePath, e);
        }
    }

    /**
     * Xóa file
     */
    public void deleteImage(String relativePath) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
            Files.deleteIfExists(filePath);
            log.info("✅ Deleted image: {}", relativePath);
        } catch (IOException e) {
            log.error("❌ Failed to delete image: {}", relativePath, e);
        }
    }

    /**
     * Kiểm tra file có tồn tại không
     */
    public boolean exists(String relativePath) {
        Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
        return Files.exists(filePath);
    }
}