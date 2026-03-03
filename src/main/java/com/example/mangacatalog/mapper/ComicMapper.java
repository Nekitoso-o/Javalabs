package com.example.mangacatalog.mapper;

import com.example.mangacatalog.dto.ComicDto;
import com.example.mangacatalog.entity.Comic;
import org.springframework.stereotype.Component;

@Component
public class ComicMapper {

    public ComicDto toDto(Comic entity) {
        if (entity == null) {
            return null;
        }
        ComicDto dto = new ComicDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setAuthor(entity.getAuthor());
        dto.setGenre(entity.getGenre());
        return dto;
    }

    public Comic toEntity(ComicDto dto) {
        if (dto == null) {
            return null;
        }
        Comic entity = new Comic();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setAuthor(dto.getAuthor());
        entity.setGenre(dto.getGenre());
        return entity;
    }
}