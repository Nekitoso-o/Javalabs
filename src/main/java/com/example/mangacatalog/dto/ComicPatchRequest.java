package com.example.mangacatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Schema(description = "Запрос на частичное обновление комикса")
public record ComicPatchRequest(
    @Size(min = 2, max = 100, message = "Название должно быть от 2 до 100 символов")
    String title,

    @Min(value = 1900, message = "Год должен быть не раньше 1900")
    @Max(value = 2100, message = "Некорректный год выпуска")
    Integer releaseYear,

    Long authorId,
    Long publisherId,
    Set<Long> genreIds
) {
}