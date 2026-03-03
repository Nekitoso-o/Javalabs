package com.example.mangacatalog.controller;

import com.example.mangacatalog.dto.ComicDto;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.service.ComicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comics")
@RequiredArgsConstructor
public class ComicController {

    private final ComicService comicService;

    // 1. GET endpoint c @PathVariable (Например: GET /api/comics/1)
    @GetMapping("/{id}")
    public ComicDto getComicById(@PathVariable Long id) {
        return comicService.getComicById(id);
    }

    // 2. GET endpoint c @RequestParam (Например: GET /api/comics/search?title=Naruto)
    @GetMapping("/search")
    public List<ComicDto> searchComics(@RequestParam String title) {
        return comicService.searchComicsByTitle(title);
    }

    // POST endpoint для добавления тестовых данных
    @PostMapping
    public ComicDto addComic(@RequestBody Comic comic) {
        return comicService.createComic(comic);
    }
}
