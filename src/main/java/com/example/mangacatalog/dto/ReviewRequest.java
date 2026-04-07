package com.example.mangacatalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
    @NotBlank(message = "Текст отзыва не может быть пустым")
    String text,

    @NotNull(message = "Оценка обязательна")
    @Min(value = 1, message = "Минимальная оценка 1")
    @Max(value = 10, message = "Максимальная оценка 10")
    Integer rating,

    @NotNull(message = "ID комикса обязательно")
    Long comicId
) {

}