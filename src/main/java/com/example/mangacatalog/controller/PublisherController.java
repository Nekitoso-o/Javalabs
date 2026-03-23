package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.PublisherDto;
import com.example.mangacatalog.service.PublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/publishers")
@RequiredArgsConstructor
public class PublisherController {
    private final PublisherService service;

    @GetMapping
    public List<PublisherDto> getAll() {
        return service.getAll();
    }

    @PostMapping
    public PublisherDto create(@RequestBody PublisherDto dto) {
        return service.create(dto);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Publisher deleted successfully";
    }
}