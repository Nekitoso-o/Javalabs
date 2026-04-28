package com.example.mangacatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Запрос на массовое создание комиксов")
public record BulkComicRequest(
    @NotEmpty(message = "Список комиксов не может быть пустым")
    @Valid
    List<ComicRequest> comics
) {}