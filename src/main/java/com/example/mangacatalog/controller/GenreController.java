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

    @PostMapping
    public GenreDto create(@RequestBody GenreDto dto) {
        return service.create(dto);
    }
}