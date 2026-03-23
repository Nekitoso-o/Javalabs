package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.ComicDto;
import com.example.mangacatalog.service.ComicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/comics")
@RequiredArgsConstructor
public class ComicController {
    private final ComicService comicService;

    @PostMapping
    public ComicDto create(@RequestBody ComicDto dto) {
        return comicService.create(dto);
    }

    @GetMapping
    public List<ComicDto> getAll() {
        return comicService.getAll();
    }

    @GetMapping("/{id}")
    public ComicDto getById(@PathVariable Long id) {
        return comicService.getById(id);
    }

    @GetMapping("/search")
    public List<ComicDto> search(@RequestParam String title) {
        return comicService.searchByTitle(title);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        comicService.delete(id);
        return "Comic deleted successfully";
    }

    @GetMapping("/demo/n-plus-one")
    public String testNPlusOne() {
        comicService.demonstrateNPlusOne();
        return "Check console logs for multiple SELECTs";
    }

    @GetMapping("/demo/entity-graph")
    public String testEntityGraph() {
        comicService.demonstrateEntityGraph();
        return "Check console logs for a single JOIN SELECT";
    }

    @PostMapping("/demo/no-transaction")
    public String testNoTransaction() {
        try {
            comicService.saveDataWithoutTransaction();
        } catch (Exception e) {
            return "Error occurred! Check DB - Publisher WAS saved.";
        }
        return "Success";
    }

    @PostMapping("/demo/with-transaction")
    public String testWithTransaction() {
        try {
            comicService.saveDataWithTransaction();
        } catch (Exception e) {
            return "Error occurred! Check DB - Publisher WAS NOT saved (Rollback successful).";
        }
        return "Success";
    }

    @PutMapping("/{id}")
    public ComicDto update(@PathVariable Long id, @RequestBody ComicDto dto) {
        return comicService.update(id, dto);
    }

    @PatchMapping("/{id}")
    public ComicDto patch(@PathVariable Long id, @RequestBody ComicDto dto) {
        return comicService.patch(id, dto);
    }
}