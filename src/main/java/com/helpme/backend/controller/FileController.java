package com.helpme.backend.controller;

import com.helpme.backend.dto.UploadResponse;
import com.helpme.backend.service.LocalStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final LocalStorageService storageService;

    /**
     * GET /v1/files/{folder}/{filename}
     * Serve static image
     */
    @GetMapping("/{folder}/{filename:.+}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String folder,
            @PathVariable String filename) {
        Resource resource = storageService.loadImage(folder + "/" + filename);

        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(Paths.get(resource.getURI()));
            if (contentType == null) {
                contentType = "image/jpeg";
            }
        } catch (IOException e) {
            // Use default
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * POST /v1/files/upload
     * Upload single image
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {
        String path = storageService.saveImage(file, folder);
        String url = "/v1/files/" + path;

        UploadResponse response = new UploadResponse(
                url,
                path,
                file.getOriginalFilename(),
                file.getSize());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /v1/files/upload-multiple
     * Upload multiple images
     */
    @PostMapping("/upload-multiple")
    public ResponseEntity<List<UploadResponse>> uploadMultipleImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {
        List<UploadResponse> responses = files.stream()
                .map(file -> {
                    String path = storageService.saveImage(file, folder);
                    String url = "/v1/files/" + path;
                    return new UploadResponse(
                            url,
                            path,
                            file.getOriginalFilename(),
                            file.getSize());
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}