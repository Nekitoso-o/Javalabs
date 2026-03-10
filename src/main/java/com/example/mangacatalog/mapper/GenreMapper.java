package com.example.mangacatalog.mapper;
import com.example.mangacatalog.dto.GenreDto;
import com.example.mangacatalog.entity.Genre;
import org.springframework.stereotype.Component;

@Component
public class GenreMapper {

    public GenreDto toDto(Genre entity) {
        if (entity == null) return null;
        GenreDto dto = new GenreDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        return dto;
    }

    public Genre toEntity(GenreDto dto) {
        if (dto == null) return null;
        Genre entity = new Genre();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        return entity;
    }
}