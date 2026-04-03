package com.example.mangacatalog.service;

import com.example.mangacatalog.dto.GenreDto;
import com.example.mangacatalog.entity.Comic;
import com.example.mangacatalog.entity.Genre;
import com.example.mangacatalog.mapper.GenreMapper;
import com.example.mangacatalog.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository repository;
    private final GenreMapper mapper;

    public List<GenreDto> getAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    public GenreDto getById(Long id) {
        Genre genre = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Genre with ID " + id + " not found!"));
        return mapper.toDto(genre);
    }

    @Transactional
    public GenreDto create(GenreDto dto) {
        Genre saved = repository.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }

    @Transactional
    public GenreDto update(Long id, GenreDto dto) {
        Genre existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Genre with ID " + id + " not found!"));
        existing.setName(dto.getName());
        return mapper.toDto(repository.save(existing));
    }

    @Transactional
    public GenreDto patch(Long id, GenreDto dto) {
        Genre existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Genre with ID " + id + " not found!"));
        if (dto.getName() != null) {
            existing.setName(dto.getName());
        }
        return mapper.toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        Genre genre = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Genre with ID " + id + " not found!"));

        if (genre.getComics() != null) {
            for (Comic comic : genre.getComics()) {
                comic.getGenres().remove(genre);
            }
        }
        repository.delete(genre);
    }
}