package com.smartshop.shared.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Exposes the S3-backed file upload used by the frontend (e.g. the PAN
 * certificate upload during shop registration). Returns a raw
 * {@code {"url": ...}} body to match the shape the frontend expects
 * (it reads {@code response.data.url} directly, not the ApiResponse envelope).
 */
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class FileUploadController {

    private final S3StorageService s3StorageService;

    /**
     * POST /api/v1/test/upload
     * Uploads a single multipart file to object storage and returns its public URL.
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String url = s3StorageService.uploadFile(file);
        return ResponseEntity.ok(new UploadResponse(url));
    }

    public record UploadResponse(String url) {}
}
