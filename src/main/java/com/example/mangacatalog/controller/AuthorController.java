package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.AuthorDto;
import com.example.mangacatalog.service.AuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService service;

    @GetMapping
    public List<AuthorDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public AuthorDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public AuthorDto create(@RequestBody AuthorDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public AuthorDto update(@PathVariable Long id, @RequestBody AuthorDto dto) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}")
    public AuthorDto patch(@PathVariable Long id, @RequestBody AuthorDto dto) {
        return service.patch(id, dto);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Author deleted successfully";
    }
}