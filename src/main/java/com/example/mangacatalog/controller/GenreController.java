package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.GenreDto;
import com.example.mangacatalog.dto.GenreRequest;
import com.example.mangacatalog.service.GenreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
@Tag(name = "Жанры", description = "API для управления жанрами комиксов")
public class GenreController {

    private final GenreService service;

    public GenreController(GenreService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Получить список всех жанров")
    public List<GenreDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить жанр по ID")
    public GenreDto getById(@PathVariable("id") Long id) {
        return service.getById(id);
    }

    @PostMapping
    @Operation(summary = "Создать новый жанр")
    public GenreDto create(@Valid @RequestBody GenreRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить название жанра")
    public GenreDto update(@PathVariable("id") Long id, @Valid @RequestBody GenreRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить жанр")
    public String delete(@PathVariable("id") Long id) {
        service.delete(id);
        return "Жанр успешно удален";
    }
}