package com.example.mangacatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на создание/обновление автора")
public record AuthorRequest(
    @Schema(description = "Имя автора", example = "Кэнтаро Миура")
    @NotBlank(message = "Имя автора не может быть пустым")
    String name
) {

}