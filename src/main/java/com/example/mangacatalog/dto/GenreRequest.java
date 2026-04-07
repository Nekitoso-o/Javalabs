package com.example.mangacatalog.dto;

import jakarta.validation.constraints.NotBlank;

public record GenreRequest(
    @NotBlank(message = "Название жанра обязательно")
    String name
) {

}