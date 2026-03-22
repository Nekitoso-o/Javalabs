package com.example.mangacatalog.mapper;
import com.example.mangacatalog.dto.*;
import com.example.mangacatalog.entity.*;
import org.springframework.stereotype.Component;

@Component
public class AuthorMapper {
    public AuthorDto toDto(Author entity) {
        if (entity == null) {
            return null;
        }

        AuthorDto dto = new AuthorDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        return dto;
    }
    public Author toEntity(AuthorDto dto) {
        if (dto == null) {
            return null;
        }
        Author entity = new Author();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        return entity;
    }
}