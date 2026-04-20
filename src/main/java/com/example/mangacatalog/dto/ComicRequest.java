package com.example.mangacatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.Set;

@Schema(description = "Запрос на создание/обновление комикса")
public record ComicRequest(
    @Schema(description = "Название комикса", example = "Берсерк")
    @NotBlank(message = "Название комикса не может быть пустым")
    String title,

    @Schema(description = "Год выпуска", example = "1989")
    @NotNull(message = "Год выпуска обязателен")
    @Min(value = 1930, message = "Год должен быть не раньше 1930")
    @Max(value = 2026, message = "Некорректный год выпуска")
    Integer releaseYear,

    @NotNull(message = "У комикса должен быть автор (укажите ID)")
    Long authorId,

    @NotNull(message = "У комикса должен быть издатель (укажите ID)")
    Long publisherId,

    @NotEmpty(message = "Комикс должен содержать хотя бы один жанр (укажите список ID)")
    Set<Long> genreIds
) {

}