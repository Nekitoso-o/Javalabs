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

    @PostMapping
    public AuthorDto create(@RequestBody AuthorDto dto) {
        return service.create(dto);
    }
}