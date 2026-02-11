package com.helpme.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class StorageConfig {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("✅ Created upload directory: {}", uploadPath.toAbsolutePath());
            } else {
                log.info("✅ Upload directory exists: {}", uploadPath.toAbsolutePath());
            }

            // Tạo các subfolder cần thiết
            createSubFolder("orders");
            createSubFolder("addons");
            createSubFolder("avatars");
            createSubFolder("providers");

        } catch (IOException e) {
            log.error("❌ Could not create upload directory!", e);
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    private void createSubFolder(String folderName) throws IOException {
        Path folderPath = Paths.get(uploadDir, folderName);
        if (!Files.exists(folderPath)) {
            Files.createDirectories(folderPath);
            log.info("✅ Created subfolder: {}", folderName);
        }
    }
}