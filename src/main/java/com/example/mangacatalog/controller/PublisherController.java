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

    @GetMapping("/{id}")
    public PublisherDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public PublisherDto create(@RequestBody PublisherDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public PublisherDto update(@PathVariable Long id, @RequestBody PublisherDto dto) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}")
    public PublisherDto patch(@PathVariable Long id, @RequestBody PublisherDto dto) {
        return service.patch(id, dto);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Publisher deleted successfully";
    }
}