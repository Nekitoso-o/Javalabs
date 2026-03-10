package com.example.mangacatalog.dto;
import lombok.Data;
import java.util.Set;

@Data
public class ComicDto {
    private Long id;
    private String title;
    private Integer releaseYear;
    private AuthorDto author;
    private PublisherDto publisher;
    private Set<GenreDto> genres;
}