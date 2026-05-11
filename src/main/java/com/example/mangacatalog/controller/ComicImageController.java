package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.ComicImageDto;
import com.example.mangacatalog.service.ComicImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/comics/{comicId}/images")
@Tag(name = "Обложки", description = "Управление изображениями комикса")
public class ComicImageController {

    private final ComicImageService imageService;

    public ComicImageController(ComicImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    @Operation(summary = "Получить все изображения комикса")
    public List<ComicImageDto> getImages(@PathVariable Long comicId) {
        return imageService.getImages(comicId);
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Загрузить изображения (одно или несколько)")
    public ResponseEntity<List<ComicImageDto>> upload(
        @PathVariable Long comicId,
        @RequestParam("files") MultipartFile[] files
    ) throws IOException {
        return ResponseEntity.ok(imageService.uploadImages(comicId, files));
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Удалить изображение")
    public ResponseEntity<Void> delete(
        @PathVariable Long comicId,
        @PathVariable Long imageId
    ) {
        imageService.deleteImage(comicId, imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @Operation(summary = "Изменить порядок изображений")
    public List<ComicImageDto> reorder(
        @PathVariable Long comicId,
        @RequestBody List<Long> orderedIds
    ) {
        return imageService.reorder(comicId, orderedIds);
    }
}