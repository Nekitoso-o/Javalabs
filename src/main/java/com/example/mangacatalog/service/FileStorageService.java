package com.example.mangacatalog.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif",
        "image/webp", "image/avif"
    );
    private static final long MAX_SIZE = 20 * 1024 * 1024; // 20 MB

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path rootPath;

    @PostConstruct
    public void init() throws IOException {
        rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(rootPath);
        Files.createDirectories(rootPath.resolve("covers"));
        Files.createDirectories(rootPath.resolve("chapters"));
        LOG.info("Хранилище файлов: {}", rootPath);
    }

    public String store(MultipartFile file, String subDir) throws IOException {
        validateFile(file);

        String ext = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "." + ext;
        Path target = rootPath.resolve(subDir).resolve(fileName).normalize();

        // Защита от path traversal
        if (!target.startsWith(rootPath)) {
            throw new IOException("Недопустимый путь файла");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        LOG.info("Файл сохранён: {}/{}", subDir, fileName);
        return fileName;
    }

    public void delete(String subDir, String fileName) {
        try {
            Path file = rootPath.resolve(subDir).resolve(fileName).normalize();
            if (!file.startsWith(rootPath)) return;
            Files.deleteIfExists(file);
            LOG.info("Файл удалён: {}/{}", subDir, fileName);
        } catch (IOException e) {
            LOG.warn("Не удалось удалить файл {}/{}: {}", subDir, fileName, e.getMessage());
        }
    }

    public Path resolve(String subDir, String fileName) {
        return rootPath.resolve(subDir).resolve(fileName).normalize();
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Файл пустой");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IOException("Файл слишком большой (максимум 20 MB)");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_TYPES.contains(ct)) {
            throw new IOException("Недопустимый тип файла: " + ct);
        }
    }

    private String getExtension(String name) {
        if (name == null || !name.contains(".")) return "jpg";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }
}