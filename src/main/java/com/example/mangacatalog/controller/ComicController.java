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

    @GetMapping("/{id}")
    public ComicDto getComicById(@PathVariable Long id) {
        return comicService.getComicById(id);
    }


    @GetMapping("/search")
    public List<ComicDto> searchComics(@RequestParam String title) {
        return comicService.searchComicsByTitle(title);
    }

    @PostMapping
    public ComicDto addComic(@RequestBody Comic comic) {
        return comicService.createComic(comic);
    }

    @GetMapping
    public List<ComicDto> getAll() {
        return comicService.getAllComics();
    }
}
