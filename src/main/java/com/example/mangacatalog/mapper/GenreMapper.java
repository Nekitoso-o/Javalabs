package com.example.mangacatalog.mapper;

import com.example.mangacatalog.dto.GenreDto;
import com.example.mangacatalog.entity.Genre;
import org.springframework.stereotype.Component;

@Component
public class GenreMapper {
    public GenreDto toDto(Genre entity) {
        if (entity == null) return null;
        return new GenreDto(entity.getId(), entity.getName());
    }
}