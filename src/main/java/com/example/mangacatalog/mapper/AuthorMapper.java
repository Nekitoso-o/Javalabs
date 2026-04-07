package com.example.mangacatalog.mapper;

import com.example.mangacatalog.dto.AuthorDto;
import com.example.mangacatalog.entity.Author;
import org.springframework.stereotype.Component;

@Component
public class AuthorMapper {
    public AuthorDto toDto(Author entity) {
        if (entity == null) return null;
        return new AuthorDto(entity.getId(), entity.getName());
    }
}