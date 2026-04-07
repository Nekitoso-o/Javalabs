package com.example.mangacatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(description = "Ответ с данными о комиксе")
public record ComicDto(
    Long id,
    String title,
    Integer releaseYear,
    AuthorDto author,
    PublisherDto publisher,
    Set<GenreDto> genres
) {

}