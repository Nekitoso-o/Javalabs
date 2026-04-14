package com.example.mangacatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Запрос на частичное обновление отзыва")
public record ReviewPatchRequest(
    String text,

    @Min(value = 1, message = "Минимальная оценка 1")
    @Max(value = 10, message = "Максимальная оценка 10")
    Integer rating,

    Long comicId
) {
}