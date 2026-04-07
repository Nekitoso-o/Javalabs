package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.PublisherDto;
import com.example.mangacatalog.dto.PublisherRequest;
import com.example.mangacatalog.service.PublisherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/publishers")
@Tag(name = "Издатели", description = "API для управления издательствами")
public class PublisherController {

    private final PublisherService service;

    public PublisherController(PublisherService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Получить список всех издателей")
    public List<PublisherDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить издателя по ID")
    public PublisherDto getById(@PathVariable("id") Long id) {
        return service.getById(id);
    }

    @PostMapping
    @Operation(summary = "Добавить нового издателя")
    public PublisherDto create(@Valid @RequestBody PublisherRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные издателя")
    public PublisherDto update(@PathVariable("id") Long id, @Valid @RequestBody PublisherRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить издателя")
    public String delete(@PathVariable("id") Long id) {
        service.delete(id);
        return "Издатель успешно удален";
    }
}