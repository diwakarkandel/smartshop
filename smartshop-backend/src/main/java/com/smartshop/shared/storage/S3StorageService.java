package com.smartshop.shared.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicUrlBase;

    public S3StorageService(S3Client s3Client, 
                            @Value("${app.s3.bucket-name}") String bucketName,
                            @Value("${app.supabase.storage.public-url-base}") String publicUrlBase) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.publicUrlBase = publicUrlBase;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String fileName = UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, 
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // Return the public URL using the configured base URL
        return publicUrlBase + bucketName + "/" + fileName;
    }
}
