package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.ComicChapterDto;
import com.example.mangacatalog.service.ComicChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/comics/{comicId}/chapters")
@Tag(name = "Главы", description = "Управление главами комикса")
public class ComicChapterController {

    private final ComicChapterService chapterService;

    public ComicChapterController(ComicChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping
    @Operation(summary = "Список глав комикса")
    public List<ComicChapterDto> getChapters(@PathVariable Long comicId) {
        return chapterService.getChapters(comicId);
    }

    @GetMapping("/{chapterId}")
    @Operation(summary = "Получить главу со всеми страницами")
    public ComicChapterDto getChapter(
        @PathVariable Long comicId,
        @PathVariable Long chapterId
    ) {
        return chapterService.getChapter(comicId, chapterId);
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Создать главу и загрузить страницы")
    public ResponseEntity<ComicChapterDto> createChapter(
        @PathVariable Long comicId,
        @RequestParam("chapterNumber") Double chapterNumber,
        @RequestParam(value = "title", required = false) String title,
        @RequestParam("files") MultipartFile[] files
    ) throws IOException {
        ComicChapterDto result = chapterService.createChapter(
            comicId, chapterNumber, title, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{chapterId}")
    @Operation(summary = "Удалить главу со всеми страницами")
    public ResponseEntity<Void> deleteChapter(
        @PathVariable Long comicId,
        @PathVariable Long chapterId
    ) {
        chapterService.deleteChapter(comicId, chapterId);
        return ResponseEntity.noContent().build();
    }
}