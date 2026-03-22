package com.example.mangacatalog.dto;

import lombok.Data;

import java.util.Set;

@Data
public class ComicDto {
    private Long id;
    private String title;
    private Integer releaseYear;
    private Set<AuthorDto> authors;
    private Set<PublisherDto> publishers;
    private Set<GenreDto> genres;
}