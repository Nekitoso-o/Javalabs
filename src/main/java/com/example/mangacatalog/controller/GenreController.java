package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.GenreDto;
import com.example.mangacatalog.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService service;

    @GetMapping
    public List<GenreDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public GenreDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public GenreDto create(@RequestBody GenreDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public GenreDto update(@PathVariable Long id, @RequestBody GenreDto dto) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}")
    public GenreDto patch(@PathVariable Long id, @RequestBody GenreDto dto) {
        return service.patch(id, dto);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Genre deleted successfully";
    }
}