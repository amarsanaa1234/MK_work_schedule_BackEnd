package com.example.mk_backEnd.service;

import com.example.mk_backEnd.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;
    private final String baseUrl;

    public FileStorageService(
            @Value("${app.upload-dir}") String uploadDir,
            @Value("${app.base-url}") String baseUrl) {
        this.uploadDir = Path.of(uploadDir);
        this.baseUrl = baseUrl;
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Upload directory-г үүсгэж чадсангүй: " + uploadDir, e);
        }
    }

    /**
     * Stores the given file on local disk under a generated, collision-free name and returns
     * the publicly reachable URL it can be fetched back from (served via WebConfig).
     */
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Хоосон файл байна.");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String filename = UUID.randomUUID() + extension;

        try {
            Files.copy(file.getInputStream(), uploadDir.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("Файл хадгалахад алдаа гарлаа: " + filename, e);
        }

        return baseUrl + "/uploads/" + filename;
    }
}
