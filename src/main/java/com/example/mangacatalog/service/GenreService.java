package com.example.mangacatalog.service;

import com.example.mangacatalog.dto.GenreDto;
import com.example.mangacatalog.entity.Genre;
import com.example.mangacatalog.mapper.GenreMapper;
import com.example.mangacatalog.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository repository;
    private final GenreMapper mapper;

    public List<GenreDto> getAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    public GenreDto create(GenreDto dto) {
        Genre saved = repository.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }
}