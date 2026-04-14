package com.example.mangacatalog.controller;
import com.example.mangacatalog.dto.TransactionDemoDto;
import com.example.mangacatalog.dto.ComicDto;
import com.example.mangacatalog.dto.ComicRequest;
import com.example.mangacatalog.dto.ComicPatchRequest;
import com.example.mangacatalog.service.ComicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comics")
@Tag(name = "Комиксы", description = "Основной API для управления каталогом комиксов и манги")
public class ComicController {

    private final ComicService comicService;

    public ComicController(ComicService comicService) {
        this.comicService = comicService;
    }

    @GetMapping
    @Operation(summary = "Получить список всех комиксов")
    public List<ComicDto> getAll() {
        return comicService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить комикс по ID")
    public ComicDto getById(@PathVariable("id") Long id) {
        return comicService.getById(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Простой поиск комиксов по названию")
    public List<ComicDto> search(@RequestParam("title") String title) {
        return comicService.searchByTitle(title);
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "Получить все комиксы конкретного автора")
    public List<ComicDto> getComicsByAuthor(@PathVariable("authorId") Long authorId) {
        return comicService.getComicsByAuthor(authorId);
    }


    @GetMapping("/complex-search")
    @Operation(summary = "Сложный поиск (Жанр + Год) с пагинацией и кэшированием")
    public List<ComicDto> searchComplex(
        @RequestParam("genreName") String genreName,
        @RequestParam("minYear") Integer minYear,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "5") int size,
        @RequestParam(value = "useNative", defaultValue = "false") boolean useNative) {
        return comicService.searchComplex(genreName, minYear, page, size, useNative);
    }

    @PostMapping
    @Operation(summary = "Добавить новый комикс в каталог")
    public ComicDto create(@Valid @RequestBody ComicRequest request) {
        return comicService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные комикса")
    public ComicDto update(@PathVariable("id") Long id, @Valid @RequestBody ComicRequest request) {
        return comicService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить комикс из каталога")
    public String delete(@PathVariable("id") Long id) {
        comicService.delete(id);
        return "Комикс успешно удален";
    }



    @PostMapping("/demo/no-transaction")
    public String testNoTransaction(@RequestBody TransactionDemoDto dto) {
        try {
            comicService.saveDataWithoutTransaction(dto);
        } catch (Exception e) {
            return "Ошибка: " + e.getMessage() + " | Проверьте БД - Издатель БЫЛ сохранен!";
        }
        return "Успех! Издатель и комикс сохранены.";
    }

    @PostMapping("/demo/with-transaction")
    public String testWithTransaction(@RequestBody TransactionDemoDto dto) {
        try {
            comicService.saveDataWithTransaction(dto);
        } catch (Exception e) {
            return "Ошибка: " + e.getMessage() + " | Проверьте БД - Издатель НЕ сохранен (Rollback).";
        }
        return "Успех! Издатель и комикс сохранены.";
    }
    @PatchMapping("/{id}")
    @Operation(summary = "Частично обновить данные комикса")
    public ComicDto patch(@PathVariable("id") Long id, @Valid @RequestBody ComicPatchRequest request) {
        return comicService.patch(id, request);
    }
}