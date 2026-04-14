package com.example.mangacatalog.mapper;

import com.example.mangacatalog.dto.ComicDto;
import com.example.mangacatalog.entity.Comic;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ComicMapper {
    private final AuthorMapper authorMapper;
    private final PublisherMapper publisherMapper;
    private final GenreMapper genreMapper;

    public ComicMapper(AuthorMapper authorMapper, PublisherMapper publisherMapper, GenreMapper genreMapper) {
        this.authorMapper = authorMapper;
        this.publisherMapper = publisherMapper;
        this.genreMapper = genreMapper;
    }

    public ComicDto toDto(Comic entity) {
        if (entity == null) {
            return null;
        }

        return new ComicDto(
            entity.getId(),
            entity.getTitle(),
            entity.getReleaseYear(),
            authorMapper.toDto(entity.getAuthor()),
            publisherMapper.toDto(entity.getPublisher()),
            entity.getGenres() != null ?
                entity.getGenres().stream().map(genreMapper::toDto).collect(Collectors.toSet()) : null
        );
    }
}