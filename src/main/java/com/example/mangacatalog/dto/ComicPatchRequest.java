package com.example.mangacatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

@Schema(description = "Запрос на частичное обновление комикса")
public record ComicPatchRequest(
    @NotBlank(message = "Название комикса не может быть пустым")
    String title,

    @Min(value = 1930, message = "Год должен быть не раньше 1930")
    @Max(value = 2026, message = "Некорректный год выпуска")
    Integer releaseYear,

    Long authorId,
    Long publisherId,
    Set<Long> genreIds
) {
}