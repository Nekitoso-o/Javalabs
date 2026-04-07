package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.AuthorDto;
import com.example.mangacatalog.dto.AuthorRequest;
import com.example.mangacatalog.service.AuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authors")
@Tag(name = "Авторы", description = "API для управления авторами")
public class AuthorController {

    private final AuthorService service;

    public AuthorController(AuthorService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Получить список всех авторов")
    public List<AuthorDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить автора по ID")
    public AuthorDto getById(@PathVariable("id") Long id) {
        return service.getById(id);
    }

    @PostMapping
    @Operation(summary = "Создать нового автора")
    public AuthorDto create(@Valid @RequestBody AuthorRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные автора")
    public AuthorDto update(@PathVariable("id") Long id, @Valid @RequestBody AuthorRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить автора")
    public String delete(@PathVariable("id") Long id) {
        service.delete(id);
        return "Автор успешно удален";
    }
}