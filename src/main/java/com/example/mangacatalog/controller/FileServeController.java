package com.example.mangacatalog.controller;

import com.example.mangacatalog.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Файлы", description = "Раздача загруженных файлов")
public class FileServeController {

    private static final Map<String, MediaType> MEDIA_TYPES = Map.of(
        "jpg",  MediaType.IMAGE_JPEG,
        "jpeg", MediaType.IMAGE_JPEG,
        "png",  MediaType.IMAGE_PNG,
        "gif",  MediaType.IMAGE_GIF,
        "webp", MediaType.valueOf("image/webp"),
        "avif", MediaType.valueOf("image/avif")
    );

    private final FileStorageService fileStorage;

    public FileServeController(FileStorageService fileStorage) {
        this.fileStorage = fileStorage;
    }

    @GetMapping("/covers/{fileName:.+}")
    @Operation(summary = "Получить обложку/изображение комикса")
    public ResponseEntity<Resource> serveCover(
        @PathVariable String fileName
    ) throws MalformedURLException {
        return serve("covers", fileName);
    }

    @GetMapping("/chapters/{fileName:.+}")
    @Operation(summary = "Получить страницу главы")
    public ResponseEntity<Resource> serveChapterPage(
        @PathVariable String fileName
    ) throws MalformedURLException {
        return serve("chapters", fileName);
    }

    private ResponseEntity<Resource> serve(
        String subDir, String fileName
    ) throws MalformedURLException {
        Path filePath = fileStorage.resolve(subDir, fileName);
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String ext = fileName.contains(".")
            ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
            : "jpg";
        MediaType mediaType = MEDIA_TYPES.getOrDefault(ext, MediaType.IMAGE_JPEG);

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
            .body(resource);
    }
}