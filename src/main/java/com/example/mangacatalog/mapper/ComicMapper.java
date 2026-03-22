package com.example.mangacatalog.mapper;

import com.example.mangacatalog.dto.ComicDto;
import com.example.mangacatalog.entity.Comic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ComicMapper {

    private final AuthorMapper authorMapper;
    private final PublisherMapper publisherMapper;
    private final GenreMapper genreMapper;

    public ComicDto toDto(Comic entity) {
        if (entity == null) {
            return null;
        }
        ComicDto dto = new ComicDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setReleaseYear(entity.getReleaseYear());

        if (entity.getAuthors() != null) {
            dto.setAuthors(entity.getAuthors().stream()
                .map(authorMapper::toDto)
                .collect(Collectors.toSet()));
        }
        if (entity.getPublishers() != null) {
            dto.setPublishers(entity.getPublishers().stream()
                .map(publisherMapper::toDto)
                .collect(Collectors.toSet()));
        }

        if (entity.getGenres() != null) {
            dto.setGenres(entity.getGenres().stream()
                .map(genreMapper::toDto)
                .collect(Collectors.toSet()));
        }
        return dto;
    }


    public Comic toEntity(ComicDto dto) {
        if (dto == null) {
            return null;
        }
        Comic entity = new Comic();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setReleaseYear(dto.getReleaseYear());
        return entity;
    }
}