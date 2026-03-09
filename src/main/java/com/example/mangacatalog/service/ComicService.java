package com.example.mangacatalog.service;

import com.example.mangacatalog.dto.ComicDto;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.mapper.ComicMapper;
import com.example.mangacatalog.repository.ComicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComicService {

    private final ComicRepository comicRepository;
    private final ComicMapper comicMapper;

    public ComicDto getComicById(Long id) {
        Comic comic = comicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Комикс не найден с ID: " + id));
        return comicMapper.toDto(comic);
    }

    public List<ComicDto> searchComicsByTitle(String title) {
        List<Comic> comics = comicRepository.findByTitleContainingIgnoreCase(title);
        return comics.stream()
                .map(comicMapper::toDto)
                .toList();
    }


    public ComicDto createComic(Comic comic) {
        Comic savedComic = comicRepository.save(comic);
        return comicMapper.toDto(savedComic);
    }
    public List<ComicDto> getAllComics() {
        return comicRepository.findAll().stream()
                .map(comicMapper::toDto)
                .toList();
    }
}
