package com.example.mangacatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные автора")
public record AuthorDto(
    Long id,
    String name
) {

}